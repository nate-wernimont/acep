package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.BlockPath;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
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
			pathMaker.findMethodPaths();
			HashMap<SootMethod, List<BlockPath>> map = pathMaker.getBlockMap();
			Set<SootMethod> keys = map.keySet();
			for(SootMethod meth : keys ){
				List<BlockPath> pathForMeth = map.get(meth);
				System.out.println(meth.toString());
				System.out.println(pathForMeth.toString());
			}
		}
	}
	
}
