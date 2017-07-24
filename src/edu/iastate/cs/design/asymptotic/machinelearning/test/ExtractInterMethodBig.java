package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.util.Chain;

public class ExtractInterMethodBig {

	public static void main(String[] args){
		Benchmark bench = new Test("crypt/config.xml");
		SootClass sClass = Scene.v().getMainClass();
		Chain<SootClass> classes = Scene.v().getClasses();
		for(SootClass _class : classes){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			PathEnumerator pathMaker = new PathEnumerator(_class);
			pathMaker.run();
			HashMap<SootMethod, List<List<Block>>> map = pathMaker.getBlockMap();
			Set<SootMethod> keys = map.keySet();
			for(SootMethod meth : keys ){
				List<List<Block>> pathForMeth = map.get(meth);
				System.out.println(meth.toString());
				System.out.println(pathForMeth.toString());
			}
		}
	}
	
}
