package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
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

public class CopyOfPathGenerator {

	private final SootMethod _method;
	private final Body b;
	private final BriefBlockGraph cGraph;
	private final DirectedGraph<Block> diGraph;
	private final List<Path> _paths;
	private BranchPredictionInfo BPI;
	static final int LIMIT = 500;
	private final LoopInfo LI;

	// static boolean isLimit = false;

	public CopyOfPathGenerator(SootMethod method) {
		_method = method;
		_paths = new ArrayList<Path>();
		b = _method.retrieveActiveBody();
		cGraph = new BriefBlockGraph(b);
		LI = new LoopInfo(method);
		diGraph = LI.getCleanGraph();
		System.out.println("Number of loops: " + LI.loops());
		BPI = new BranchPredictionInfoImpl(LI);
		// Error case
		if (LI.loops() > 0) {
			// has some loops ...so backedges shouldn't be zero
			if (BPI.backEdgeCount() < 1) {
				System.out
						.println("Error! unable to form loop properly. Sorry...");
				return;
			}
		}

		transform();
		//display();
	}
	
	public CopyOfPathGenerator(SootMethod method, LoopInfo loopInfo, BranchPredictionInfo BPI) {
		_method = method;
		_paths = new ArrayList<Path>();
		b = _method.retrieveActiveBody();
		cGraph = new BriefBlockGraph(b);
		if (loopInfo == null)
			LI = new LoopInfo(method);
		else 
			LI = loopInfo;//new LoopInfo(method);
		
		diGraph = LI.getCleanGraph();
		System.out.println("Number of loops: " + LI.loops());
		if (BPI == null)
			BPI = new BranchPredictionInfoImpl(LI);
		else
			this.BPI = BPI;
		// Error case
		if (LI.loops() > 0) {
			// has some loops ...so backedges shouldn't be zero
			if (BPI.backEdgeCount() < 1) {
				System.out
						.println("Error! unable to form loop properly. Sorry...");
				return;
			}
		}

		if(b!=null) transform();
		// display();
	}

	private final void transform() {
			/*
			 * Iterator<soot.toolkits.graph.Block> blockIter =
			 * diGraph.iterator(); if (blockIter.hasNext()) { Block block =
			 * blockIter.next(); Path path = new Path(); path.addToPath(block);
			 * getPath(block, path); }
			 */
			// Check for multiple heads => error case, since we have removed the
			// exceptional nodes
			/*if (diGraph.getHeads().size() > 1) {
				System.out.println("Error!, problem with graph");
			}*/
			List<Block> heads = diGraph.getHeads();
			Block head = heads.get(0); // Assuming that first node itself is
										// right node and not exceptional node
			// for (Block head : heads) {
			// if (head.getSuccs().size() == 0) {
			// continue;
			// }
			Path path = new Path();
			path.addToPath(head);
			getPath(head, path);
			// }
	}

	/*
	 * private void getPath(Block block, Path path) { List<Block> succs =
	 * cGraph.getSuccsOf(block);
	 * 
	 * boolean hasOnlyLoopSuccessors = true; // When there is no successor, add
	 * the current path to paths list if (succs.size() == 0) { _paths.add(path);
	 * /*if (_paths.size() >= LIMIT) isLimit = true; return; }
	 * 
	 * for (Block succ : succs) { // If the successor is in the list of
	 * predecessors // then its a loop, so add the path to paths list // and
	 * exit for now, need to find out better ways // to handle loops and
	 * recursion /* if (isPred(block, succ)) { _paths.add(path); continue; }
	 * else
	 */
	// If we have reached the limit then return immediately.
	/*
	 * if (isLimit) return;
	 * 
	 * if (!BPP.isBackEdge(new Pair<Block, Block>(block, succ)) &&
	 * path.contains(succ)) { continue; }
	 * 
	 * hasOnlyLoopSuccessors = false; // Copy the path Path succPath = new
	 * Path(); succPath.copy(path); succPath.addToPath(succ); getPath(succ,
	 * succPath); }
	 * 
	 * // If we have reached the limit then return immediately. /*if (isLimit)
	 * return;
	 * 
	 * if (hasOnlyLoopSuccessors) _paths.add(path); }
	 */

	private void getPath(Block block, Path path) {
		if (_paths.size() >= LIMIT)
			return;

		List<Block> succs = diGraph.getSuccsOf(block);

		boolean hasOnlyLoopSuccessors = true;
		// When there is no successor, add the current path to paths list
		if (succs.size() == 0) {
			_paths.add(path);
			return;
		}

		for (Block succ : succs) {
			// If the successor is in the list of predecessors
			// then its a loop, so add the path to paths list
			// and exit for now, need to find out better ways
			// to handle loops and recursion
			/*
			 * if (isPred(block, succ)) { _paths.add(path); continue; } else
			 */
			// If succ and block pointing to the same, continue
			if (succ.toString().equals(block.toString()))	continue;

			if (BPI.isBackEdge(new Pair<Block, Block>(block, succ))
					/* && path.contains(succ) && path.contains(block, succ)*/) {
				continue;
			}
			
			if (LI.isLoopHeader(block) && path.contains(block, succ))
				continue;

			//if (!block.toString().equals(succ.toString()))
				hasOnlyLoopSuccessors = false;
			// Copy the path
			Path succPath = new Path();
			succPath.copy(path);
			succPath.addToPath(succ);
			succPath.addEdge(block, succ);
			getPath(succ, succPath);
			if (_paths.size() >= LIMIT)	break;
		}

		// If we have reached the limit then return immediately.
		/*
		 * if (isLimit) return;
		 */

		/*if (hasOnlyLoopSuccessors)
			_paths.add(path);*/
	}

	private boolean isPred(Block block, Block succ) {
		List<Block> preds = diGraph.getPredsOf(block);
		for (Block pred : preds) {
			if (pred.toString().equals(succ.toString()))
				return true;
		}
		return false;
	}

	public void display() {
		System.out.println("Listing all program paths for method: "
				+ _method.getName());
		System.out.println("Number of non-loop paths: " + _paths.size());
		for (Path path : _paths) {
			System.out
					.println("......................................................");
			Iterator<Block> nodesIter = path.iterator();
			while (nodesIter.hasNext()) {
				Block block = nodesIter.next();
				System.out.print(block.toString());
				if (nodesIter.hasNext())
					System.out.print("==============>");
			}
		}
	}

	public List<Path> getPaths() {
		return _paths;
	}
	
	public int getloopCount () {
		return LI.loops();
	}

}
