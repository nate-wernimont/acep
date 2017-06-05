package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphBuilder;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;

public class ControlFlowGraphs {

	public ControlFlowGraphs() {
		init();
	}
	
	private void init() {
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		
		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");
		
		SootClass c = Scene.v().loadClassAndSupport("examples.Beckett");
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		Scene.v().loadNecessaryClasses();
		// Spark analysis, true for brief analysis and false for full
		CallGraphBuilder.setSparkPointsToAnalysis(true);
		// Create callgraph
		//CallGraphDFS _cg = new CallGraphDFS(_sootMethod);
		
		Iterator methodIt = c.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if(!m.hasActiveBody())	continue;
			System.out.println("================"+ m.getSignature() +"===============");
			JimpleBody b = (JimpleBody) m.retrieveActiveBody();
			BriefBlockGraph cGraph = new BriefBlockGraph(b);
			Iterator<Block> srcBlockIter = cGraph.iterator();
			while(srcBlockIter.hasNext()) {
				Block blk = srcBlockIter.next();
				System.out.println(blk.toString());
			}
		}
	}
	public static void main(String[] args) {
		ControlFlowGraphs cfg = new ControlFlowGraphs();
	}
}
