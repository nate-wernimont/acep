package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.util.ArrayList;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.EvaluateData;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;

public class CollectDataTest {
	
	
	public static void main(String[] args) throws Exception{
		if(args.length < 1){
			System.out.println("No args supplied");
			return;
		}
		ArrayList<Integer> toTest = new ArrayList<>();
		for(String arg : args){
			toTest.add(Integer.parseInt(arg));
		}
		
		String[] classes = {/*"lufact",*/ "series", "crypt", "sor", /*"sparsematmult"*/, "montecarlo"};
		ArrayList<String> training_configs = new ArrayList<>();
		ArrayList<String> test_configs = new ArrayList<>();
		for(int i = 0; i < classes.length; i++){
			if(toTest.contains(new Integer(i)))
				test_configs.add(classes[i] + File.separator + "config.xml");
			else
				training_configs.add(classes[i] + File.separator + "config.xml");
		}
		
		Classifier classifier = new NaiveBayes();
		EvaluateData data = new EvaluateData(training_configs, test_configs, classifier, classes[toTest.get(0)]);
		data.run();
	}
}
