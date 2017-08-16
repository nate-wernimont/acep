package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.iastate.cs.design.asymptotic.annotations.ParameterScale;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.ListWrapper;
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
		long firstTime = System.currentTimeMillis();
		for(SootClass _class : Scene.v().getApplicationClasses()){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			pathExtracters.add(new PathEnumerator(_class));
			System.out.println(_class.getName());
		}
		
		List<Path> paths = new ArrayList<>();
		
		List<ListWrapper> lists = new ArrayList<>(); 
		
		for(PathEnumerator pe : pathExtracters){
			System.out.println("==="+pe.getDeclaredClass()+"===");
			pe.run();
			paths.addAll(pe.getPaths());
			lists.addAll(pe.getWrappedPaths());
			//HashMap<Path, FeatureStatistic> features = pe.getFeatureStatistics();
//			System.out.println("Paths: "+features.size());
//			for(Path path : features.keySet()){
//				FeatureStatistic fs = features.get(path);
//				System.out.println(path);
//				System.out.println(fs);
//			}
		}
		List<List<Unit>> convertedPaths = new ArrayList<>();
		for(Path path: paths){
			convertedPaths.addAll(path.getAllPaths(null));
			System.out.println(convertedPaths.size());
		}
		
		System.out.println(lists.size()+"=============");
		
		System.out.println((System.currentTimeMillis()-firstTime));
	}
	
}
