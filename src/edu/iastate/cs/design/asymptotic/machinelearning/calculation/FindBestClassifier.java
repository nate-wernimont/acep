package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.nustaq.serialization.FSTConfiguration;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.Unit;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SGD;
import weka.classifiers.meta.MultiScheme;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class FindBestClassifier {

	static FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);
	public static void main(String[] args) {
		Instances training_data, eval_data;
		File training = new File("Training.txt");
		File eval = new File("Evaluating.txt");
		try(FileInputStream reader = new FileInputStream(training)){
			byte[] in = new byte[(int) training.length()];
			reader.read(in);
			training_data = (Instances) conf.asObject(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Error("The training file couldn't be found!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error encountered while reading from training file!");
		}
		
		File trainingArff = new File("training.arff");
		try(FileWriter fos = new FileWriter(trainingArff)){
			trainingArff.createNewFile();
			fos.write(training_data.toString());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error occurred while writing to the results file!");
		}
		
		try(FileInputStream reader = new FileInputStream(eval)){
			byte[] in = new byte[(int) eval.length()];
			reader.read(in);
			eval_data = (Instances) conf.asObject(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Error("The eval file couldn't be found!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error encountered while reading from eval file!");
		}
		
		File evalArff = new File("eval.arff");
		try(FileWriter fos = new FileWriter(evalArff)){
			evalArff.createNewFile();
			fos.write(eval_data.toString());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error occurred while writing to the results file!");
		}
		
		
		ArrayList<Classifier> classifiers = new ArrayList<>();
		classifiers.add(new ZeroR());
		classifiers.add(new J48());
		classifiers.add(new NaiveBayes());
		classifiers.add(new Logistic());
		classifiers.add(new SGD());
		classifiers.add(new MultiScheme());
		classifiers.add(new JRip());
		classifiers.add(new DecisionStump());
		classifiers.add(new BayesNet());
		
		for(Classifier classifier : classifiers){
			try {
				classifier.buildClassifier(training_data);
				Evaluation evaluator = new Evaluation(training_data);
				evaluator.evaluateModel(classifier, eval_data);
				System.out.println("==="+classifier.getClass().getSimpleName()+"===");
				System.out.println(evaluator.correct()+", "+evaluator.incorrect());
				double[][] confusionMatrix = evaluator.confusionMatrix();
				System.out.println(confusionMatrix[0][0]+":"+confusionMatrix[0][1]+"\n"+confusionMatrix[1][0]+":"+confusionMatrix[1][1]);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Failed evaluating model");
			}
		}
	}

}
