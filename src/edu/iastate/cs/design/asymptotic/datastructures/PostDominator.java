package edu.iastate.cs.design.asymptotic.datastructures;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.MHGPostDominatorsFinder;


public class PostDominator {
	
	MHGPostDominatorsFinder PDT;
	MHGDominatorsFinder dominator;
	
	ExceptionalUnitGraph eug;
	CompleteBlockGraph cGraph;
	DirectedGraph g;
	
	public PostDominator (ExceptionalUnitGraph g) {
		eug = g;
		PDT = new MHGPostDominatorsFinder (eug);
		dominator = new MHGDominatorsFinder(eug);
	}
	
	public PostDominator (DirectedGraph g) {
		this.g = g;
		PDT = new MHGPostDominatorsFinder (g);
		dominator = new MHGDominatorsFinder(g);
	}
	
	public PostDominator (CompleteBlockGraph g) {
		cGraph = g;
		PDT = new MHGPostDominatorsFinder (cGraph);
		dominator = new MHGDominatorsFinder(cGraph);
	}
	
	public boolean dominates (soot.toolkits.graph.Block u1, soot.toolkits.graph.Block u2) {
		try {
			return PDT.isDominatedBy(u2.getHead(), u1.getHead());
		} catch (Exception e) {
			return dominator.isDominatedBy(u2.getHead(), u1.getHead());
		}
	}

}
