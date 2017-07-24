package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.EvaluateData;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.Unit;

public class ComparePaths {

	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println("No benchmark supplied");
			return;
		}
		String b = args[0];
		String config = b + File.separator + "config.xml";
		new Test(config);
	
		EvaluateData ed = new EvaluateData();
		List<List<Unit>> possiblePaths = new ArrayList<>();
		for(SootClass _class : Scene.v().getApplicationClasses()){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			PathEnumerator paths = new PathEnumerator(_class);
			paths.run();
			possiblePaths.addAll(paths.getListPaths());
		}
		
		ed.collectResults("results/results_"+Scene.v().getMainClass().getShortName()+".txt", possiblePaths);
	}

}
