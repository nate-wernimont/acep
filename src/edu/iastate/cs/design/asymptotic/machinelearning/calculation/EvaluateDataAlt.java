package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.nustaq.serialization.FSTConfiguration;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
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
public class EvaluateDataAlt {

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
	private List<Path<Unit>> _hot_paths, _cold_paths;
	
	/**
	 * The evaluator object
	 */
	private Evaluation evaluator = null;
	
	FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);
	
	private ArrayList<String> hotVals;
	
	public EvaluateDataAlt(){
		
	}
	
	/**
	 * Evaluate the data
	 * @param training_configs String array of all of the training config names
	 * @param eval_configs String array of the configs to evaluate
	 * @param classifier Classifier to use
	 * @param name Name of the test
	 */
	public EvaluateDataAlt(List<String> training_configs, List<String> eval_configs, Classifier classifier, String name){
		_training_configs = training_configs;
		_eval_configs = eval_configs;
		_classifier = classifier;
		_name = name;
		training_statistics = new HashMap<Path<Unit>, FeatureStatistic>();
		eval_statistics = new HashMap<Path<Unit>, FeatureStatistic>();
		_hot_paths = new ArrayList<>();
		_cold_paths = new ArrayList<>();
		hotVals = new ArrayList<String>();
		hotVals.add("Hot");
		hotVals.add("Cold");
	}
	
	/**
	 * Executes the object to produce the Weka data
	 */
	public void run(){
		System.out.println("Collecting Statistics");
		collectStatistics(_training_configs, training_statistics);
		collectStatistics(_eval_configs, eval_statistics);
		System.out.println("Generating Data");
		setupData();
		addData(training_data, training_statistics, false);
		addData(eval_data, eval_statistics, true);
		System.out.println("Evalauting data");
		evaluateWeka();
	}
	
	/**
	 * Collect all of the paths within the File titled filename using a list of the paths that are possible
	 * @param filename The name of the file
	 * @param possiblePaths A list of the statically enumerated paths
	 * @return All of the paths used within the actual execution of the file along with their execution count
	 */
	public List<Pair<Path<Unit>, Integer>> collectResults(String filename){
		List<Pair<Path<Unit>, Integer>> result = new ArrayList<>();
		File results = new File(filename);
//		for(Path<Unit> path : possiblePaths){
//			System.out.println(path);
//		}
		try(FileInputStream reader = new FileInputStream(results)){
			byte[] in = new byte[(int) results.length()];
			reader.read(in);
			ArrayList<Pair<Path<Unit>, Integer>> paths = (ArrayList<Pair<Path<Unit>, Integer>>) conf.asObject(in);
			result.addAll(paths);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Error("The results file couldn't be found!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error encountered while reading from results file!");
		}
		return result;
	}
	
	/**
	 * Identify all of the hot paths from a given list of paths
	 * @param pathCounts A list of the paths along with their execution counts
	 */
	public void getHotPaths(List<Pair<Path<Unit>, Integer>> pathCounts){
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
//		System.out.println(pathCounts);
		int limit = (int) (pathCounts.size() * HOT_PATH_PERCENTAGE);
		System.out.println("Getting first "+limit+" paths from "+pathCounts.size());
		for(int i = 0; i < limit; i++){
			_hot_paths.add(pathCounts.get(i).first());
		}
		for(int i = limit; i < pathCounts.size(); i++){
			_cold_paths.add(pathCounts.get(i).first());
		}
	}
	
	/**
	 * Build the classifier and evaluate it on the eval data
	 * @throws Exception
	 */
	private void evaluateWeka() {
		System.out.println(training_data);
		try {
			_classifier.buildClassifier(training_data);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed building Classifier");
		}
		try {
			evaluator = new Evaluation(training_data);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed building Evaluator");
		}
		try {
			evaluator.evaluateModel(_classifier, eval_data);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed evaluating model");
		}
		System.out.println(evaluator.correct()+", "+evaluator.incorrect());
		double[][] confusionMatrix = evaluator.confusionMatrix();
		System.out.println(confusionMatrix[0][0]+":"+confusionMatrix[0][1]+"\n"+confusionMatrix[1][0]+":"+confusionMatrix[1][1]);
		File f = new File("Training.txt");
		File f2 = new File("Evaluating.txt");
		try(FileOutputStream fos = new FileOutputStream(f)){
			f.createNewFile();
			fos.write(conf.asByteArray(training_data));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error occurred while writing to the results file!");
		}
		try(FileOutputStream fos = new FileOutputStream(f2)){
			f2.createNewFile();
			fos.write(conf.asByteArray(eval_data));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error occurred while writing to the results file!");
		}
	}
	
	/**
	 * Collect all of the FeatureStatistics and execution counts from the given configs
	 * @param configs A list of the configs to analyze
	 * @param map A map in which to put all of the newly found FeatureStatistics
	 */
	private void collectStatistics(List<String> configs, HashMap<Path<Unit>, FeatureStatistic> map){
		List<SootClass> classesDone = new ArrayList<>();
		for(String config : configs){
			new Test(config);
			
			for(SootClass _class : Scene.v().getApplicationClasses()){
				if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete() || classesDone.contains(_class)){
					continue;
				}
				PathEnumerator calculateInfo = new PathEnumerator(_class, false);
				List<Pair<Path<Unit>, Integer>> result = collectResults("results/results_"+Scene.v().getMainClass().getShortName()+".txt");
				List<Path<Unit>> paths = new ArrayList<>();
				for(Pair<Path<Unit>, Integer> path : result){
					for(SootMethod sm : _class.getMethods()){
						if(Path.unitEquals(sm.retrieveActiveBody().getUnits().getFirst(), path.first().get(0)))
							paths.add(path.first());
					}
				}
				calculateInfo.calculateCounts(paths, map);
				if(!_class.getPackageName().contains("jgfutil"))
					classesDone.add(_class);
			}
			
			getHotPaths(collectResults("results/results_"+Scene.v().getMainClass().getShortName()+".txt"));
			
			Scene.v().getClasses().clear();
		}
	}
	
	private void setupData(){
		//Create data framework
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(Count count : FeatureStatistic.Count.values()){
			attributes.add(new Attribute(count.name()+"_COUNT"));
		}
		for(Coverage coverage : FeatureStatistic.Coverage.values()){
			attributes.add(new Attribute(coverage.name()+"_COVERAGE"));
		}
		Attribute hotAttr = new Attribute("Hot Path", hotVals);
		attributes.add(hotAttr);
		training_data = new Instances(_name+"-training", attributes, 0);
		eval_data = new Instances(_name+"-eval", attributes, 0);
		training_data.setClass(hotAttr);
		eval_data.setClass(hotAttr);
	}
	
	/**
	 * Puts all of the feature statistics and hot path information into Weka usable data objects
	 * @param data Where to put the newly created data
	 * @param statistics The object containing all of the original information
	 */
	private void addData(Instances data, HashMap<Path<Unit>, FeatureStatistic> statistics, boolean eval){
		ArrayList<Path<Unit>> coldAndHot = new ArrayList<>(_hot_paths);
		ArrayList<Path<Unit>> pathsToEval = new ArrayList<>(statistics.keySet());
		coldAndHot.addAll(_cold_paths);
//		if(!eval){
			pathsToEval.retainAll(coldAndHot);
//		}
		//Add the actual data
		for(Path<Unit> path : pathsToEval){
			double[] dataValues = new double[data.numAttributes()];
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
