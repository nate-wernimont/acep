package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;

public class SootUnitTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SootClass c = Scene.v().loadClassAndSupport("MyClass");
		c.setApplicationClass();
		SootMethod m = c.getMethodByName("main");
		Body b = m.retrieveActiveBody();
		UnitGraph g = new ExceptionalUnitGraph(b);
		Iterator<Unit> unitIter = g.iterator();
		while(unitIter.hasNext()) {
			Unit u = unitIter.next();
			System.out.println(u.toString());
		}
		CFGToDotGraph graph = new CFGToDotGraph();
		soot.util.dot.DotGraph dotGraph = graph.drawCFG(g, b);
		
		dotGraph.plot(m.getName()+".dot");
	}

}
