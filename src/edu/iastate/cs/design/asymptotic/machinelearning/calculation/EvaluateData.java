package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.nustaq.serialization.FSTConfiguration;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class EvaluateData {
	
	private ArrayList<String> training_configs;
	
	private String eval_config;
	
	private ArrayList<String> hotVals;
	
	private FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);
	
	private List<ListWrapper> training_paths, eval_paths;
	
	private Instances training_data, eval_data;
	
	private List<SootClass> classesDone;
	
	private CallGraphDFS evalCG;
	
	public EvaluateData(ArrayList<String> training_configs, String eval_config){
		this.training_configs = training_configs;
		this.eval_config = eval_config;
		init();
	}
	
	private void init(){
		training_paths = new ArrayList<>();
		eval_paths = new ArrayList<>();
		hotVals = new ArrayList<>();
		hotVals.add("Hot");
		hotVals.add("Cold");
		classesDone = new ArrayList<>();
		evalCG = null;
	}
	
	public void run(){
		prepareEval();//Run PathEnumerator and collect all paths
		getHotPaths();//Read from files and identify which are hot
		evaluateTrainingPaths();//get feature statistics for every path
		prepareData();//intitialize data
		createData(training_data, training_paths);//insert feature statistics and hot/coldness in
		createData(eval_data, eval_paths);
		//evaluateData() = use Weka to find future hot path. Do this iteratively and create a list of the hot paths
		predictHotMethod(evaluateData());//use the predicted hot paths and their original methods and called methods to find hot methods
		prepareArffFiles();//look at data in weka
	}

	private void prepareArffFiles() {
		File trainingFile = new File(PrintInfo.FILE_LOCATION+"training.arff");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(trainingFile))){
			writer.write(training_data.toString());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error writing to training file");
		}
		
		File evalFile = new File(PrintInfo.FILE_LOCATION+"eval.arff");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(evalFile))){
			writer.write(eval_data.toString());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error writing to eval file");
		}
	}

	private void predictHotMethod(List<ListWrapper> hotPaths) {
		HashMap<SootMethod, Integer> methodCount = new HashMap<>();
		for(ListWrapper hotPath : hotPaths){
			increase(hotPath.getMeth(), methodCount);
//			for(Unit unit : hotPath.getList()){
//				SootMethod methodInvoked = PathEnumerator.methodInvocation(unit);
//				if(methodInvoked != null && evalCG.callGraph.edgesInto(methodInvoked).hasNext() && methodInvoked.getDeclaringClass().isApplicationClass() && !methodInvoked.getDeclaringClass().isJavaLibraryClass() && methodInvoked.getDeclaringClass().isConcrete()){
//					increase(methodInvoked, methodCount);
//				}
//			}
		}
		System.out.println(methodCount);
		List<SootMethod> recommendedMethods = new ArrayList<>();
		List<Entry<SootMethod, Integer>> methodCounts = new ArrayList<>(methodCount.entrySet());
		Collections.sort(methodCounts, new Comparator<Entry<SootMethod, Integer>>(){

			@Override//This is opposite, but the Collections.sort normally does lowest to highest.
			public int compare(Entry<SootMethod, Integer> o1, Entry<SootMethod, Integer> o2) {
				return o2.getValue().intValue()-o1.getValue().intValue();
			}
		
		});
		for(int i = 0; i <= methodCounts.size()*.05; i++){
			recommendedMethods.add(methodCounts.get(i).getKey());
		}
		System.out.println("Original methods: "+recommendedMethods);
		System.out.println(evalCG.callGraph.size());
		getPreviousMethods(new ArrayList<>(recommendedMethods), evalCG.callGraph, recommendedMethods);
		evalCG.setACEPMethods(recommendedMethods);
		evalCG.drawCG(evalCG.getFunction().getDeclaringClass().getShortName());
		System.out.println("Recommended Methods: "+evalCG.methodsAlongACEP.toString());
		System.out.println("Recommended: "+recommendedMethods);
		
	}

	private void getPreviousMethods(List<SootMethod> methsToRecurse, CallGraph cg, List<SootMethod> masterList) {
		List<SootMethod> nextMethsToCheck = new ArrayList<>();
		for(SootMethod sm : methsToRecurse){
			System.out.println("Called: "+sm);
			Iterator<Edge> edgeIter = cg.edgesInto(sm);
			while(edgeIter.hasNext()){
				Edge edge = edgeIter.next();
				SootMethod prevMethod = edge.getSrc().method();
				System.out.println("By: "+prevMethod+": "+edge.getTgt());
				if(!methsToRecurse.contains(prevMethod) && !masterList.contains(prevMethod)){
					nextMethsToCheck.add(prevMethod);
					masterList.add(prevMethod);
				}
			}
		}
		if(nextMethsToCheck.size() > 0)
			getPreviousMethods(nextMethsToCheck, cg, masterList);
	}

	private void increase(SootMethod methodInvoked, HashMap<SootMethod, Integer> map) {
		if(map.get(methodInvoked) != null){
			map.put(methodInvoked, map.get(methodInvoked).intValue()+1);
		} else {
			map.put(methodInvoked, 1);
		}
	}

	private List<ListWrapper> evaluateData() {
		List<ListWrapper> hotPaths = null;
		try {
			hotPaths = new ArrayList<>();
			
			Classifier classifier = new Logistic();
			classifier.buildClassifier(training_data);
			Evaluation evaluator = new Evaluation(training_data);
			for(ListWrapper path : eval_paths){
				double hotOrCold = evaluator.evaluateModelOnce(classifier, path.getInstance());
				if(hotOrCold == hotVals.indexOf("Hot")){
					hotPaths.add(path);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hotPaths;
	}

	private void createData(Instances data, List<ListWrapper> paths) {
		for(ListWrapper path : paths){
			double[] info = new double[data.numAttributes()];
			FeatureStatistic feature = path.getFS();
			for(Count count : FeatureStatistic.Count.values()){
				info[count.identifier] = feature.getValue(count);
			}
			for(Coverage coverage : FeatureStatistic.Coverage.values()){
				info[coverage.identifier+18] = feature.getValue(coverage);
			}
			if(path.getHot()){
				info[data.numAttributes()-1] = hotVals.indexOf("Hot");
			} else {
				info[data.numAttributes()-1] = hotVals.indexOf("Cold");
			}
			Instance instance = new DenseInstance(1.0, info);
			instance.setDataset(data);
			data.add(instance);
			path.setInstance(instance);
		}
	}

	private void evaluateTrainingPaths() {
		PathEnumerator trainingEvaluator = new PathEnumerator(null);
		trainingEvaluator.calculateCounts(training_paths);
	}

	private void prepareData() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(Count count : FeatureStatistic.Count.values()){
			attributes.add(new Attribute(count.name()+"_COUNT"));
		}
		for(Coverage coverage : FeatureStatistic.Coverage.values()){
			attributes.add(new Attribute(coverage.name()+"_COVERAGE"));
		}
		Attribute hotAttr = new Attribute("TEMPERATURE", hotVals);
		attributes.add(hotAttr);
		
		training_data = new Instances("training", attributes, 0);
		training_data.setClass(hotAttr);
		eval_data = new Instances("training", attributes, 0);
		eval_data.setClass(hotAttr);
	}

	private void prepareEval() {
		Benchmark eval = new Test(eval_config, true);
		evalCG = eval.getCallGraph();
		
		for(SootClass evalClass : Scene.v().getApplicationClasses()){
			if(evalClass.isLibraryClass() || evalClass.isJavaLibraryClass() || !evalClass.isConcrete() || classesDone.contains(evalClass))
				continue;
			PathEnumerator evalStatisticGetter = new PathEnumerator(evalClass, true);
			evalStatisticGetter.run();
			List<ListWrapper> paths = evalStatisticGetter.getWrappedPaths();
			evalStatisticGetter.calculateCounts(paths);
			eval_paths.addAll(paths);
		}
	}

	private void getHotPaths() {
		for(String config : training_configs){
			Benchmark b = new Benchmark(config);
			List<ListWrapper> paths = getPaths(b._class);
			
			for(ListWrapper path : paths){
				if(path.getCount() > 10){
					path.setHot(true);
				}
			}
			
			//Eliminate the correct classes for the eval path enumeration
			for(SootClass trainingClass : Scene.v().getApplicationClasses()){
				if(trainingClass.isLibraryClass() || trainingClass.isJavaLibraryClass() || !trainingClass.isConcrete())
					continue;
				if(!trainingClass.getPackageName().equals("jgfutil"))
					classesDone.add(trainingClass);
			}
			
			training_paths.addAll(paths);
		}
	}
	
	private List<ListWrapper> getPaths(String className){
		List<ListWrapper> paths = null;
		File resultFile = new File(PrintInfo.FILE_LOCATION+"results/results_"+className+".txt");
		try(FileInputStream fileReader = new FileInputStream(resultFile)){
			byte[] input = new byte[(int) resultFile.length()];
			fileReader.read(input);
			paths = (List<ListWrapper>) conf.asObject(input);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Error("The results file doesn't exist: "+resultFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error reading from file: "+resultFile.getAbsolutePath());
		}
		return paths;
	}
	
	
	
}