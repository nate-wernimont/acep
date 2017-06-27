package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.options.Options;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class EvaluateData {

	private List<String> _training_configs, _eval_configs;
	
	private Classifier _classifier;
	
	private Instances training_data = null, eval_data = null;
	
	private HashMap<Path<Unit>, FeatureStatistic> training_statistics, eval_statistics;
	
	private static final double HOT_PATH_PERCENTAGE = .10;
	
	private String _name;
	
	private List<Path<Unit>> _hot_paths;
	
	private Evaluation evaluator = null;
	
	/**
	 * Evaluate the data
	 * @param training_configs String array of all of the training config names
	 * @param eval_configs String array of the configs to evaluate
	 * @param classifier Classifier to use
	 * @param name Name of the test
	 */
	public EvaluateData(List<String> training_configs, List<String> eval_configs, Classifier classifier, String name){
		_training_configs = training_configs;
		_eval_configs = eval_configs;
		_classifier = classifier;
		_name = name;
		training_statistics = new HashMap<Path<Unit>, FeatureStatistic>();
		eval_statistics = new HashMap<Path<Unit>, FeatureStatistic>();
		_hot_paths = new ArrayList<>();
	}
	
	public void run(){
		System.out.println("Collecting Statistics");
		collectStatistics(_training_configs, training_statistics);
		collectStatistics(_eval_configs, eval_statistics);
		System.out.println("Evaluating Data");
		generateData(training_data, training_statistics);
		generateData(eval_data, eval_statistics);
		try {
			System.out.println("Evalauting data");
			evaluateWeka();
		} catch (Exception e) {
			throw new Error("Weka encountered an error");
		}
	}
	
	public List<Pair<Path<Unit>, Integer>> collectResults(String filename, List<Path<Unit>> possiblePaths){
		List<Pair<Path<Unit>, Integer>> result = new ArrayList<>();
		File results = new File(filename);
		try(BufferedReader reader = new BufferedReader(new FileReader(results))){
			String line;
			while((line = reader.readLine()) != null){
				String path = line.split(PrintInfo.DIVIDER)[0];
				int pathCount = Integer.parseInt(line.split(PrintInfo.DIVIDER)[1]);
				boolean found = false;
				for(Path<Unit> possiblePath : possiblePaths){
					if(possiblePath.toString().equals(path)){
						found = true;
						result.add(new Pair<>(possiblePath, new Integer(pathCount)));
						break;
					}
				}
				if(!found)
					System.out.println("Path Not Found: "+path);
				else
					System.out.println("Path Found! "+path);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Error("The results file couldn't be found!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error encountered while reading from results file!");
		}
		return result;
	}
	
	public void getHotPaths(List<Pair<Path<Unit>, Integer>> pathCounts){
		System.out.println(pathCounts.size());
		System.out.println(pathCounts);
		for(int evaluating = 0; evaluating < pathCounts.size(); evaluating++){
			boolean added = false;
			for(int comparing = evaluating-1; comparing >= 0; comparing--){
				if(pathCounts.get(evaluating).second() > pathCounts.get(comparing).second())
					continue;
				else {
					pathCounts.add(comparing+1, pathCounts.get(evaluating));
					pathCounts.remove(evaluating+1);
					added = true;
					break;
				}
			}
			if(!added){
				pathCounts.add(0, pathCounts.get(evaluating));
				pathCounts.remove(evaluating+1);
			}
		}
		System.out.println(pathCounts);
		int limit = (int) (pathCounts.size() * HOT_PATH_PERCENTAGE);
		System.out.println("Getting first "+limit+" paths from "+pathCounts.size());
		for(int i = 0; i < limit; i++){
			System.out.println(pathCounts.get(i));
			_hot_paths.add(pathCounts.get(i).first());
		}
	}
	
	private void evaluateWeka() throws Exception {
		_classifier.buildClassifier(training_data);
		evaluator = new Evaluation(training_data);
		evaluator.evaluateModel(_classifier, eval_data);
		System.out.println(evaluator.correct()+", "+evaluator.incorrect());
	}
	
	private void collectStatistics(List<String> _training_configs2, HashMap<Path<Unit>, FeatureStatistic> map){
		for(String config : _training_configs2){
			new Test(config);
			
			List<Path<Unit>> possiblePaths = new ArrayList<>();
			
			for(SootClass _class : Scene.v().getApplicationClasses()){
				if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
					continue;
				}
				PathEnumerator calculateInfo = new PathEnumerator(_class);
				calculateInfo.run();
				possiblePaths.addAll(calculateInfo.getPaths());
				map.putAll(calculateInfo.getFeatureStatistics());
			}
			
			getHotPaths(collectResults("results/results_"+Scene.v().getMainClass().getShortName()+".txt", possiblePaths));
			
			for(SootClass _class : Scene.v().getClasses()){//Reset the scene
				Scene.v().removeClass(_class);
			}
		}
	}
	
	private void generateData(Instances data, HashMap<Path<Unit>, FeatureStatistic> statistics){
		//Create data framework
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(Count count : FeatureStatistic.Count.values()){
			attributes.add(new Attribute(count.name()+"_COUNT"));
		}
		for(Coverage coverage : FeatureStatistic.Coverage.values()){
			attributes.add(new Attribute(coverage.name()+"_COVERAGE"));
		}
		ArrayList<String> hotVals = new ArrayList<String>();
		hotVals.add("Hot");
		hotVals.add("Cold");
		attributes.add(new Attribute("Hot Path", hotVals));
		data = new Instances(_name+data.hashCode(), attributes, 0);
		
		//Add the actual data
		for(Path<Unit> path : statistics.keySet()){
			double[] dataValues = new double[training_data.numAttributes()];
			FeatureStatistic feature = statistics.get(path);
			for(Count count : FeatureStatistic.Count.values()){
				dataValues[count.ordinal()] = feature.getValue(count);
			}
			for(Coverage coverage : FeatureStatistic.Coverage.values()){
				dataValues[coverage.ordinal()+18] = feature.getValue(coverage);
			}
			if(_hot_paths.contains(path)){
				dataValues[data.numAttributes()-1] = hotVals.indexOf("Hot");
			} else {
				dataValues[data.numAttributes()-1] = hotVals.indexOf("Cold");
			}
			data.add(new DenseInstance(1.0, dataValues));
		}
	}
	
}
