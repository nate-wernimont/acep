package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.EvaluateData;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

public class CollectDataTest {
	
	public static void main(String[] args) throws Exception{
		String[] classes = {"lufact", "series", "crypt", "sor", "sparsematmult"};
		String[] training_configs = new String[4];
		String[] test_configs = new String[1];
		for(int i = 0; i < 4; i++){
			training_configs[i] = classes[i] + File.separator + "config.xml";
		}
		test_configs[0] = classes[4] + File.separator + "config.xml";
		Classifier classifier = AbstractClassifier.forName("J48", null);
		EvaluateData data = new EvaluateData(training_configs, test_configs, classifier, "Sparsematmult", null);
	}
}
