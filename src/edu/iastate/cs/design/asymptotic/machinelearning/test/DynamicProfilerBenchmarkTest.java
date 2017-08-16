package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.DynamicProfiler;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.options.Options;

public class DynamicProfilerBenchmarkTest {

	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("No benchmark supplied");
			return;
		}
		String b = args[8];
		String config = b + File.separator + "config.xml";
		new Test(config);
		
		DynamicProfiler dp = new DynamicProfiler(Scene.v().getMainClass());
		//dp.addTransformer(Options.v().output_format_class);
		dp.analyzeFiles();
	}
}