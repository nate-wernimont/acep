package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.DynamicProfiler;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PrintInfo;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.options.Options;

public class DynamicProfilerBenchmarkTest {

	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("No benchmark supplied");
			return;
		}
		String b = args[0];
		String config = b + File.separator + "config.xml";
		Benchmark benchmark = new Test(config);
		Scene.v().loadNecessaryClasses();
		
		DynamicProfiler dp = new DynamicProfiler(Scene.v().getMainClass());
		//dp.addTransformer(Options.v().output_format_class);
		dp.analyzeFile(new File(PrintInfo.FILE_LOCATION+"JGFLUFactBenchSizeA.txt"));
	}
}
