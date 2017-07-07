package edu.iastate.cs.design.asymptotic.tests.benchmarks;

import java.util.Iterator;
import java.util.List;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.interfaces.impl.StaticProfilePassImpl;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

public class Xalan extends Benchmark {
	
	private static String _config = "/home/ganeshau/benchmarks/xalan/config.xml";
	private StaticProfilePassImpl spp;
	private CallGraphDFS dfs;
	//private SootMethod main;
	
	public Xalan () {
		super(_config);
		prepareSoot();
		//display();
	}
	
	public void display () {    
        // display the list of application classes
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for (SootClass sootClass : classes) {
			System.out.println (sootClass.getName());
		}
        
        // CallGraph CG = Scene.v().getCallGraph();
        dfs = getCallGraph();
        // spp = new StaticProfilePassImpl(dfs, main);
        //spp.runOnModule(null);
        List<SootMethod> methods = dfs.buildDFSFunctionTree();
        
        System.out.println ("====================================================================================");
        // display methods
        
        Iterator methodIt = methods.iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			System.out.println(m.getName()+"-------------------"+m.getDeclaringClass());
			
		}	
        
	}

}
