package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionInfo;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BranchPredictionInfoImpl;

import soot.Body;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.DirectedGraph;

public class PathGenerator {

	//private final SootMethod _method;
	/*private final Body b;
	private final BriefBlockGraph cGraph;
	private final DirectedGraph<Block> diGraph;*/
	private final Collection<ExecutionPath> _paths;
	/*private BranchPredictionInfo BPI;
	static final int LIMIT = 500;*/
	private final LoopInfo LI;
	private String method;
	// static boolean isLimit = false;
	private double methodTime = 0.0;

	public PathGenerator(SootMethod method, LoopInfo LI) {
		//_method = method;
		this.method = method.getName();
		_paths = new ArrayList<ExecutionPath>();
		Body b = method.retrieveActiveBody();
		BriefBlockGraph cGraph = new BriefBlockGraph(b);
		this.LI = LI;//new LoopInfo(method);
		_paths.addAll(CfgWalker.process(cGraph));
		//diGraph = LI.getCleanGraph();
		//System.out.println("Number of loops: " + LI.loops());
		//BPI = new BranchPredictionInfoImpl(LI);
		// Error case
		/*if (LI.loops() > 0) {
			// has some loops ...so backedges shouldn't be zero
			if (BPI.backEdgeCount() < 1) {
				System.out
						.println("Error! unable to form loop properly. Sorry...");
				return;
			}
		}

		transform();
*/		//display();
	}
	
	public PathGenerator(SootMethod method, LoopInfo LI, 
			HashMap<Block, Double> blockTimes, HashMap<Pair<Block, Block>, Double> efreqMap) {
		//_method = method;
		this.method = method.getName();
		_paths = new ArrayList<ExecutionPath>();
		Body b = method.retrieveActiveBody();
		BriefBlockGraph cGraph = new BriefBlockGraph(b);
		this.LI = LI;//new LoopInfo(method);
		methodTime = CfgWalker.process(cGraph, blockTimes, efreqMap);
		//diGraph = LI.getCleanGraph();
		//System.out.println("Number of loops: " + LI.loops());
		//BPI = new BranchPredictionInfoImpl(LI);
		// Error case
		/*if (LI.loops() > 0) {
			// has some loops ...so backedges shouldn't be zero
			if (BPI.backEdgeCount() < 1) {
				System.out
						.println("Error! unable to form loop properly. Sorry...");
				return;
			}
		}

		transform();
*/		//display();
	}
	
	public double getTime () {
		return methodTime;
	}

	public void display() {
		System.out.println("Listing all program paths for method: "
				+ method);
		System.out.println("Number of non-loop paths: " + _paths.size());
		for (ExecutionPath path : _paths) {
			System.out
					.println("");
			Iterator<Block> nodesIter = path.blocks().iterator();
			while (nodesIter.hasNext()) {
				Block block = nodesIter.next();
				System.out.print(block.toShortString());
				if (nodesIter.hasNext())
					System.out.print("==============>");
			}
		}
	}

	public Collection<ExecutionPath> getPaths() {
		return _paths;
	}
	
	public int getloopCount () {
		return LI.loops();
	}

}
