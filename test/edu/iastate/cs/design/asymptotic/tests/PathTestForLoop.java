package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.PathGenerator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class PathTestForLoop {
	
	public static void main(String[] args) {
		SootClass sClass = Scene.v().loadClassAndSupport("examples.Average");
		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("main"))
				continue;
			LoopInfo LI = new LoopInfo(m);
			PathGenerator pathGen = new PathGenerator (m,LI);
			pathGen.display();
		}
	}

}
