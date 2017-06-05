package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.JastAddJ.ArrayAccess;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphBuilder;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.interfaces.impl.Analysis;

public class JGSerialCrypto {

	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		prepare();
	}
	
	public static String construct_class_path(String proc) {
		String cp = "";
		String[] _jre_jars = { "rt.jar", "jce.jar" };
		String JRE = "/home/ganeshau";//"/usr/lib/jvm/java-6-openjdk/jre/lib";
		for (String jar : _jre_jars) {
			cp = cp + JRE + "/" + jar + ":";
		}
		/*for (String jar : _lib_jars) {
			cp = cp + _LIB + "/" + jar + ":";
		}*/
		//for (String processdir : _processdir) {
		cp = cp + JRE + "/" + "JGSerial.jar" + ":";
			cp = cp + JRE;
		//}
		//cp = cp + _LIB;
		//_classpath = cp;
		return cp;
	}
	
	public static void prepare () {
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);
		// Processdir should specify the class file locations which
		// have to be considered for the analysis
		List<String> procs = new ArrayList<String>();
		String proc = "/home/gupadhyaya/parallel";
		procs.add(proc);
		Options.v().set_process_dir(procs);
		// Soot classpath should have path to JRE (rt.jar, jce.jar),
		// specific bechmark related jar file locations and,
		// processdir paths
		//construct_class_path();
		//System.out.println(_classpath);
		String classpath = construct_class_path(proc);
		System.out.println(classpath);
		Options.v().set_soot_classpath(classpath);
		// Setting the phase options accelerates the initial cg construction
		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");
		
		//String dir = "org.paninij.concurrentPatterns.examples.observer.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.visitor.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.composite.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.command.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.abstractFactory.sequential.financial";
		//String dir = "org.paninij.concurrentPatterns.examples.adapter.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.chainOfResponsibility.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.decorator.sequential";
		//String dir = "org.paninij.concurrentPatterns.examples.facade.sequential";
		
		// Only the classes which are part of includes are analyzed
		List<String> incs = new ArrayList<String>();
		incs.add("jgfutil");
		incs.add("crypt");
		Options.v().set_include(incs); 
		// Mention the starting point and the main method
		SootClass c = Scene.v().loadClassAndSupport("JGFCryptBenchSizeA");
		SootMethod sootMethod = c.getMethodByName("main");
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		// Important step, without which you will not be able to run spark
		// analysis
		Scene.v().loadNecessaryClasses();
		// Spark analysis, true for brief analysis and false for full
		CallGraphBuilder.setSparkPointsToAnalysis(true);
		// Create callgraph
		CallGraphDFS cg = new CallGraphDFS(sootMethod);
		
		Analysis profile = new Analysis(sootMethod, cg);
	}

}
