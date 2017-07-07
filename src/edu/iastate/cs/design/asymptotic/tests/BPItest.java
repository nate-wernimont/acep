package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BranchPredictionInfoImpl;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class BPItest {
	
	public static void main(String[] args) {
		SootClass sClass = Scene.v().loadClassAndSupport("permute");		
		sClass.setApplicationClass();
		
		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod)methodIt.next();
			if (m.getName().equals("permute_next_pos")) {
				LoopInfo LI = new LoopInfo (m);
				BranchPredictionInfoImpl BPI = new BranchPredictionInfoImpl (LI);
				BPI.displayResult();
			}
		}
	}

}
