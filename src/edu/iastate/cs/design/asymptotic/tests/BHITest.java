package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BranchHeuristicsInfoImpl;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BranchPredictionInfoImpl;

public class BHITest {

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
			if (!m.getName().equals("permute_next_pos"))
				continue;
//			if (!m.getName().equals("atoi"))
//				continue;
			
			System.out
			.println("****************"+m.getName()+"*****************");
			LoopInfo LI = new LoopInfo(m);
			BranchPredictionInfoImpl BPI = new BranchPredictionInfoImpl(LI);
			//BPI.displayResult();

			// Let's test the heuristics
			BranchHeuristicsInfoImpl BHI = new BranchHeuristicsInfoImpl(LI, BPI);

			JimpleBody b = (JimpleBody) LI.methodBody();

			// JimpleBody body = Jimple.v().newBody(m);
			// Run through all the basic blocks
			if (b != null) {
				CompleteBlockGraph cGraph = new CompleteBlockGraph(b);
				Iterator<Block> blocks = cGraph.iterator();
				while (blocks.hasNext()) {
					Block block = blocks.next();
					
//					// Just for debugging purpose
//					if (!(unit instanceof JInvokeStmt))
//						continue;
					
					// Test all the heuristics to find the match
					for (int bh = 0; bh < 9; bh++) {
						Pair<Block, Block> prediction = BHI.MatchHeuristic(bh,
								block);

						// Heuristic did not match
						if (prediction == null)
							continue;

						count++;
						System.out
								.println("***************************************************");
						System.out.println("Block: " + block.toString());
						System.out.println("Matched heuristics: " + bh);
						System.out.println("Edge taken: "
								+ prediction.first().toString());
						System.out.println("Edge Not taken: "
								+ prediction.second().toString());
						System.out
								.println("***************************************************");
					}
				}
			}
			// }
		}

		System.out.println(count);
	}

}
