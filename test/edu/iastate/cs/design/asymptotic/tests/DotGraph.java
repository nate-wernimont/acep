package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;

public class DotGraph {

	public static void main(String[] args) {
		int count = 0;
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("test");
		sClass.setApplicationClass();
		
		Scene.v().loadNecessaryClasses();
		//Scene.v().getPointsToAnalysis().

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			/*if (!m.getName().equals("main"))
				continue;*/
			System.out
			.println("****************"+m.getName()+"*****************");
			//LoopInfo LI = new LoopInfo(m);
			JimpleBody b = (JimpleBody) m.retrieveActiveBody();
			ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
			
			// BlockGraph bGraph = new BlockGraph(eug);
			CompleteBlockGraph cGraph = new CompleteBlockGraph(b);
			
			CFGToDotGraph graph = new CFGToDotGraph();
			soot.util.dot.DotGraph dotGraph = graph.drawCFG(cGraph, b);
			
			dotGraph.plot(m.getName()+".dot");

			/*// JimpleBody body = Jimple.v().newBody(m);
			// Run through all the basic blocks
			if (b != null) {
				//ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
				//Iterator<Unit> units = eug.iterator();
				Iterator<Block> blockIter = cGraph.iterator();
				while (blockIter.hasNext()) {
					Block blk = blockIter.next();
					Iterator<Unit> blkUnitsIter = blk.iterator();
					while (blkUnitsIter.hasNext()) {
						Unit unit = blkUnitsIter.next();
						System.out.println (unit.toString());
						System.out.println ("Preds:= "+eug.getPredsOf(unit));
						System.out.println("Succ:= "+eug.getSuccsOf(unit));
						System.out.println ("===================================================");
					}
					//System.out.println (blk.toString());
				}
//				while (units.hasNext()) {
//					Unit unit = units.next();
//					System.out.println (unit.toString());
//				}
			}*/
		}
		
	}

}
