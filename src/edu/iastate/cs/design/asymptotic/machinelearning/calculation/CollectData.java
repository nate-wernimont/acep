package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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

public class CollectData {

	String[] _training_configs, _eval_configs;
	
	Classifier _classifier;
	
	Instances training_data = null, eval_data = null;
	
	HashMap<Path<Unit>, FeatureStatistic> training_statistics, eval_statistics;
	
	String _name;
	
	List<Path<Unit>> _hot_paths;
	
	Evaluation evaluator = null;
	
	/**
	 * Construct the class for evaluating the data
	 * @param training_configs Paths to each config file for the training set
	 * @param eval_configs Paths to each config file for the test set
	 * @param classifier The classifier to use
	 * @param name The name of the test
	 * @param hotPaths A List of all of the hot paths
	 */
	public CollectData(String[] training_configs, String[] eval_configs, Classifier classifier, String name, List<Path<Unit>> hotPaths){
		_training_configs = training_configs;
		_eval_configs = eval_configs;
		_classifier = classifier;
		_name = name;
		_hot_paths = hotPaths;
		training_statistics = new HashMap<Path<Unit>, FeatureStatistic>();
		eval_statistics = new HashMap<Path<Unit>, FeatureStatistic>();
	}
	
	public void run(){
		collectStatistics(_training_configs, training_statistics);
		collectStatistics(_eval_configs, eval_statistics);
		generateData(training_data, training_statistics);
		generateData(eval_data, eval_statistics);
		try {
			evaluateWeka();
		} catch (Exception e) {
			throw new Error("Weka encountered an error");
		}
	}
	
	private void evaluateWeka() throws Exception {
		_classifier.buildClassifier(training_data);
		evaluator = new Evaluation(training_data);
		evaluator.evaluateModel(_classifier, eval_data);
		System.out.println(evaluator.correct()+", "+evaluator.incorrect());
	}
	
	private void collectStatistics(String[] configs, HashMap<Path<Unit>, FeatureStatistic> map){
		for(String config : configs){
			new Test(config);
			for(SootClass _class : Scene.v().getApplicationClasses()){
				if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
					continue;
				}
				PathEnumerator calculateInfo = new PathEnumerator(_class);
				calculateInfo.run();
				map.putAll(calculateInfo.getFeatureStatistics());
			}
			for(SootClass _class : Scene.v().getClasses()){//Reset the scene
				Scene.v().removeClass(_class);
			}
		}
	}
	
	private void generateData(Instances data, HashMap<Path<Unit>, FeatureStatistic> statistics){
		//Create data framework
		ArrayList<Attribute> attributes = new ArrayList<>();
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
