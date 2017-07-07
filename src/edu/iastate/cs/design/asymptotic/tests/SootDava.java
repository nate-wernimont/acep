package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

public class SootDava {

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		prepare (args);
		PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {

					@Override
					protected void internalTransform(Body body, String phase,
							Map options) {
						
					}
				}));
		Options.v().set_output_dir("myOut");
		Options.v().set_output_format(Options.output_format_d); // Options.output_format_d
		PackManager.v().runPacks();
		PackManager.v().writeOutput();

	}
	
	/**
	 * 
	 * @return
	 */
	public static String construct_class_path() {
		String cp = "";
		String[] _jre_jars = { "rt.jar", "jce.jar" };
		String JRE = "/usr/lib/jvm/java-6-openjdk/jre/lib";
		String HOME = "/home/ganeshau";
		//String JRE = HOME;
		for (String jar : _jre_jars) {
			cp = cp + JRE + "/" + jar + ":";
		}
		cp = cp + HOME + "/" + "Travis.jar" + ":";
		cp = cp + HOME;
		return cp;
	}

	/**
	 * 
	 * @param args
	 */
	public static void prepare(String[] args) {
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);

		String classpath = construct_class_path();
		System.out.println(classpath);
		Options.v().set_soot_classpath(classpath);

		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");

		// Ready to include stuffs
		List<String> includes = new ArrayList<String>();
		includes.add("util");
		includes.add("wordSearch");
		Options.v().set_include(includes);

		// Mention the starting point and the main method
		String mainClass = "wordSearch.Controller", mainMethod = "main";
		SootClass c = Scene.v().loadClassAndSupport(mainClass);
		SootMethod sootMethod = c.getMethodByName(mainMethod);
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		// Important step, without which you will not be able to run spark
		// analysis
		Scene.v().loadNecessaryClasses();
	}

}
