package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.io.IOException;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.DynamicProfiler;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.options.Options;

public class DynamicProfilerTest {

	public static void main(String[] args) throws IOException, InterruptedException{
		String _classpath = "/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/rt.jar:/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/jce.jar:/Users/natemw/Documents/workspace/Test/bin:/Users/natemw/Documents/acep/bin";
		
		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		System.out.println(Options.v().soot_classpath());
		Scene.v().loadNecessaryClasses();
		SootClass sClass = Scene.v().forceResolve("Test.alg", SootClass.BODIES);
		sClass.setApplicationClass();
		DynamicProfiler alg = new DynamicProfiler(sClass);
		Scene.v().setMainClass(sClass);
		alg.addTransformer(Options.output_format_jimple);
		//alg.runNewClass();
		//alg.analyzeFile(f);
	}
	
}