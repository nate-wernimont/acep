package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class TestUpdateLoopBlocks {
	
	public static void main(String[] args) {
		int count = 0;
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("permute");
		sClass.setApplicationClass();

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("atoi"))
				continue;
			System.out
			.println("****************"+m.getName()+"*****************");
			LoopInfo LI = new LoopInfo(m);
			Iterator<Loop> loopsIter = LI.iterator();
			while (loopsIter.hasNext()) {
				Loop loop = loopsIter.next();
				System.out.println (loop.toString());
			}
		}
	}

}
