package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BlockEdgeFrequencyPassImpl;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BranchPredictionPassImpl;

public class BEFPTest {
	
	public static void main(String[] args) {
		int count = 0;
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("examples.Binary");
		sClass.setApplicationClass();

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			// if (m.getName().equals("run")) {
//			if (m.getName().equals("atoi"))
//				continue;
			if (!m.getName().equals("main"))
				continue;
			
			System.out
			.println("****************"+m.getName()+"*****************");
			// LoopInfo LI = new LoopInfo(m);
			BlockEdgeFrequencyPassImpl BPP = new BlockEdgeFrequencyPassImpl(m);
			BPP.display1();
			BPP.display2();
		}
	}

}
