package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.Unit;

public class BenchmarkExtractPaths {

	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("No benchmark supplied");
			return;
		}
		String b = args[0];
		String config = b + File.separator + "config.xml";
		Benchmark benchmark = new Test(config);
		ArrayList<PathEnumerator> pathExtracters = new ArrayList<>();
		for(SootClass _class : Scene.v().getApplicationClasses()){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			pathExtracters.add(new PathEnumerator(_class));
			System.out.println(_class.getName());
		}
		
		for(PathEnumerator pe : pathExtracters){
			pe.run();
			HashMap<Path<Unit>, FeatureStatistic> features = pe.getFeatureStatistics();
			System.out.println("Paths: "+features.size());
		}
	}
	
}
