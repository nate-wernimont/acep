package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
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

/**
 * 
 * Evaluates the given data using Weka
 * @author Nate Wernimont
 *
 */
public class EvaluateData {

	/**
	 * The configs of the benchmarks to evaluate
	 */
	private List<String> _training_configs, _eval_configs;
	
	/**
	 * The classifier to analyze the data with
	 */
	private Classifier _classifier;
	
	/**
	 * The instances containing either the training or eval data
	 */
	private Instances training_data = null, eval_data = null;
	
	/**
	 * A mapping from all of the paths to their respective FeatureStatistic
	 */
	private HashMap<Path<Unit>, FeatureStatistic> training_statistics, eval_statistics;
	
	/**
	 * The percentage of the paths to mark as hot
	 */
	private static final double HOT_PATH_PERCENTAGE = .10;
	
	/**
	 * The name of the test
	 */
	private String _name;
	
	/**
	 * List of all of the hot paths
	 */
	private List<Path<Unit>> _hot_paths;
	
	/**
	 * The evaluator object
	 */
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
	
	/**
	 * Executes the object to produce the Weka data
	 */
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
	
	/**
	 * Collect all of the paths within the File titled filename using a list of the paths that are possible
	 * @param filename The name of the file
	 * @param possiblePaths A list of the statically enumerated paths
	 * @return All of the paths used within the actual execution of the file along with their execution count
	 */
	public List<Pair<Path<Unit>, Integer>> collectResults(String filename, List<Path<Unit>> possiblePaths){
		List<Pair<Path<Unit>, Integer>> result = new ArrayList<>();
		File results = new File(filename);
		System.out.println("Looking among "+possiblePaths.size()+" paths.");
		for(Path<Unit> path : possiblePaths){
			System.out.println(path);
		}
		try(ObjectInputStream reader = new ObjectInputStream(new FileInputStream(results))){
			Pair<Path<Unit>, Integer> path;
			while((path = (Pair<Path<Unit>, Integer>) reader.readObject()) != null){
				boolean found = false;
				for(Path<Unit> possiblePath : possiblePaths){
					if(possiblePath.equals(path.first())){
						found = true;
						result.add(path);
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
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new Error("Error loading object!");
		}
		return result;
	}
	
	/**
	 * Identify all of the hot paths from a given list of paths
	 * @param pathCounts A list of the paths along with their execution counts
	 */
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
	
	/**
	 * Build the classifier and evaluate it on the eval data
	 * @throws Exception
	 */
	private void evaluateWeka() throws Exception {
		_classifier.buildClassifier(training_data);
		evaluator = new Evaluation(training_data);
		evaluator.evaluateModel(_classifier, eval_data);
		System.out.println(evaluator.correct()+", "+evaluator.incorrect());
	}
	
	/**
	 * Collect all of the FeatureStatistics and execution counts from the given configs
	 * @param configs A list of the configs to analyze
	 * @param map A map in which to put all of the newly found FeatureStatistics
	 */
	private void collectStatistics(List<String> configs, HashMap<Path<Unit>, FeatureStatistic> map){
		for(String config : configs){
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
			
			Scene.v().getClasses().clear();
		}
	}
	
	/**
	 * Puts all of the feature statistics and hot path information into Weka usable data objects
	 * @param data Where to put the newly created data
	 * @param statistics The object containing all of the original information
	 */
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
				dataValues[count.ordinal()] = (int) feature.getValue(count);
			}
			for(Coverage coverage : FeatureStatistic.Coverage.values()){
				dataValues[coverage.ordinal()+18] = (float) feature.getValue(coverage);
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
