package edu.iastate.cs.design.asymptotic.branching.test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.branching.BlockPath;
import edu.iastate.cs.design.asymptotic.branching.PathEnumerator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class ExtractPaths {

	public static void main(String[] args){
		String _classpath = "/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/rt.jar:/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/jce.jar:/Users/natemw/Documents/workspace/Test/bin";
	
		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		System.out.println(Options.v().soot_classpath());
		Scene.v().loadNecessaryClasses();
		Options.v().set_whole_program(true);
		SootClass sClass = Scene.v().forceResolve("Test.alg", SootClass.BODIES);
		sClass.setApplicationClass();
		
		PathEnumerator pathFinder = new PathEnumerator(sClass);
		pathFinder.findMethodPaths();
		HashMap<SootMethod, List<BlockPath>> map = pathFinder.getMap();
		Set<SootMethod> keys = map.keySet();
		for(SootMethod meth : keys ){
			List<BlockPath> pathForMeth = map.get(meth);
			System.out.println(meth.toString());
			System.out.println(pathForMeth.toString());
		}
	}
	
}
