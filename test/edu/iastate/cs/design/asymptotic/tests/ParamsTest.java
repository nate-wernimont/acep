package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;

public class ParamsTest {
	
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
			Iterator<Unit> unitsIter = LI.getGraph().iterator();
			while (unitsIter.hasNext()) {
				Unit unit = unitsIter.next();
				if (LI.isParam(unit)) {
					System.out.println (unit.toString());
				}
			}
		}
		
	}

}
