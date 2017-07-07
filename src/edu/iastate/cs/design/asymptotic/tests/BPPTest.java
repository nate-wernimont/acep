package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BranchPredictionPassImpl;

public class BPPTest {
	
	public static void main(String[] args) {
		int count = 0;
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("permute");
		sClass.setApplicationClass();

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			// if (m.getName().equals("run")) {
//			if (m.getName().equals("atoi"))
//				continue;
			if (!m.getName().equals("permute_next_pos"))
				continue;
			
			System.out
			.println("****************"+m.getName()+"*****************");
			LoopInfo LI = new LoopInfo(m);
			BranchPredictionPassImpl BPP = new BranchPredictionPassImpl(LI);
			BPP.result();
		}
	}

}
