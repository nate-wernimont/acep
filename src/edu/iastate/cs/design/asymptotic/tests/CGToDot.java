package edu.iastate.cs.design.asymptotic.tests;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Module;

public class CGToDot {

	public static void main(String[] args) {
		int count = 0;
		Module m = new Module("examples.Average");
		m.init();
		CallGraphDFS cg = new CallGraphDFS(m.main());
		// Draw callgraph as dotgraph
		cg.drawCG("Average");
		
	}
}
