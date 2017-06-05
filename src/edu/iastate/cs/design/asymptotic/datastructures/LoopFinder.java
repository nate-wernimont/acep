package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import soot.Body;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.HashReversibleGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.MutableDirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.Block;

public class LoopFinder {

	private UnitGraph g;
	//private BlockGraph b;
	private HashReversibleGraph mdg;

	private HashMap<Stmt, List<Stmt>> loops;
	private HashMap<Block, List<Block>> blockloops;

	public Collection<Loop> loops() {
		Collection<Loop> result = new HashSet<Loop>();
		for (Map.Entry<Stmt, List<Stmt>> entry : loops.entrySet()) {
			result.add(new Loop(entry.getKey(), entry.getValue(), g));
		}
		return result;
	}
	
	/*public Collection<Loop> blockloops() {
		Collection<Loop> result = new HashSet<Loop>();
		for (Map.Entry<Block, List<Block>> entry : blockloops.entrySet()) {
			result.add(new Loop(entry.getKey(), entry.getValue(), g));
		}
		return result;
	}*/
	
	public HashReversibleGraph cleanGraph (BlockGraph g) {
		Object node;
		boolean foundMoreThanOne = false;
		// HashMutableDirectedGraph...
		HashReversibleGraph cleanGraph = new HashReversibleGraph(g);
		Iterator nodeIter;
		boolean hasNext = true;
		do {
			// find nodes with no predecessors
			foundMoreThanOne = false;
			List heads = cleanGraph.getHeads();
			ArrayList<Block> entries = new ArrayList<Block>();
			nodeIter = heads.iterator();
			nodeIter.next();
			while (nodeIter.hasNext()) {
				node = (Block)nodeIter.next();
				if ((cleanGraph.getPredsOf(node).size() == 0) && (cleanGraph.getSuccsOf(node).size() > 0))
					entries.add((Block)node);
			}
				
			if (entries.size() > 0) {
				foundMoreThanOne = true;
			}
			// remove all but first
			if (foundMoreThanOne) {
				nodeIter = entries.iterator();
				if (!nodeIter.hasNext()) {
					// error
				} else {
					while (nodeIter.hasNext()) {
						// first iteration will go past first
						node = nodeIter.next();
						List nodeSuccs = cleanGraph.getSuccsOf(node);
						for (Object succ : nodeSuccs) {
							cleanGraph.removeEdge(node, succ);
						}
						//mdg.removeEdge(node);
					}
				}
			}
		} while (foundMoreThanOne);
		
		return cleanGraph;
	}
	
	public void internalTransform(Body b, BlockGraph g) {
		// g = new ExceptionalUnitGraph(b);
		//this.g = g;
		// / NEW
		Object node;
		boolean foundMoreThanOne = false;
		// HashMutableDirectedGraph...
		mdg = new HashReversibleGraph(g);
		Iterator nodeIter;
		boolean hasNext = true;
		do {
			// find nodes with no predecessors
			/*
			Iterator nodeIter = mdg.getNodes().iterator();
			ArrayList<Unit> entries = new ArrayList<Unit>();
			foundMoreThanOne = false;
			while (nodeIter.hasNext()) {
				node = (Unit)nodeIter.next();
				if ((mdg.getPredsOf(node).size() == 0) && (mdg.getSuccsOf(node).size() > 0))
					entries.add((Unit)node);
			}*/
			foundMoreThanOne = false;
			List heads = mdg.getHeads();
			ArrayList<Block> entries = new ArrayList<Block>();
			nodeIter = heads.iterator();
			nodeIter.next();
			while (nodeIter.hasNext()) {
				node = (Block)nodeIter.next();
				if ((mdg.getPredsOf(node).size() == 0) && (mdg.getSuccsOf(node).size() > 0))
					entries.add((Block)node);
			}
				
			if (entries.size() > 0) {
				foundMoreThanOne = true;
			}
			// remove all but first
			if (foundMoreThanOne) {
				nodeIter = entries.iterator();
				if (!nodeIter.hasNext()) {
					// error
				} else {
					while (nodeIter.hasNext()) {
						// first iteration will go past first
						node = nodeIter.next();
						List nodeSuccs = mdg.getSuccsOf(node);
						for (Object succ : nodeSuccs) {
							mdg.removeEdge(node, succ);
						}
						//mdg.removeEdge(node);
					}
				}
			}
		} while (foundMoreThanOne);
		//g = new BriefUnitGraph();
		// / END NEW

		MHGDominatorsFinder a = new MHGDominatorsFinder(mdg);

		blockloops = new HashMap<Block, List<Block>>();

		
		Iterator blockIter = mdg.getNodes().iterator();
		while (blockIter.hasNext()) {
			Block s = (Block) blockIter.next();

			List<Block> succs = mdg.getSuccsOf(s);
			//List<Unit> preds = mdg.getPredsOf(s);
			Collection<Block> dominaters = (Collection<Block>) a.getDominators(s);

			ArrayList<Block> headers = new ArrayList<Block>();

			Iterator<Block> succsIt = succs.iterator();
			while (succsIt.hasNext()) {
				Block succ = (Block) succsIt.next();
				if (dominaters.contains(succ)) {
					// header succeeds and dominates s, we have a loop
					headers.add(succ);
					// Just to make sure 
					/*if (preds.size() < succs.size())
						break;*/
				}
			}

			Iterator<Block> headersIt = headers.iterator();
			while (headersIt.hasNext()) {
				Block header = headersIt.next();
				List<Block> loopBody = getLoopBodyFor1(header, s);

				// for now just print out loops as sets of stmts
				// System.out.println("FOUND LOOP: Header: "+header+" Body: "+loopBody);
				if (blockloops.containsKey(header)) {
					// merge bodies
					List<Block> lb1 = blockloops.get(header);
					blockloops.put(header, union1(lb1, loopBody));
				} else {
					blockloops.put(header, loopBody);
				}
			}
		}
	}

	@SuppressWarnings( { "unchecked" })
	public void internalTransform(Body b, UnitGraph g) {

		// g = new ExceptionalUnitGraph(b);
		this.g = g;
		// / NEW
		Object node;
		boolean foundMoreThanOne = false;
		// HashMutableDirectedGraph...
		mdg = new HashReversibleGraph(g);
		Iterator nodeIter;
		boolean hasNext = true;
		do {
			// find nodes with no predecessors
			/*
			Iterator nodeIter = mdg.getNodes().iterator();
			ArrayList<Unit> entries = new ArrayList<Unit>();
			foundMoreThanOne = false;
			while (nodeIter.hasNext()) {
				node = (Unit)nodeIter.next();
				if ((mdg.getPredsOf(node).size() == 0) && (mdg.getSuccsOf(node).size() > 0))
					entries.add((Unit)node);
			}*/
			foundMoreThanOne = false;
			List heads = mdg.getHeads();
			ArrayList<Unit> entries = new ArrayList<Unit>();
			nodeIter = heads.iterator();
			nodeIter.next();
			while (nodeIter.hasNext()) {
				node = (Unit)nodeIter.next();
				if ((mdg.getPredsOf(node).size() == 0) && (mdg.getSuccsOf(node).size() > 0))
					entries.add((Unit)node);
			}
				
			if (entries.size() > 0) {
				foundMoreThanOne = true;
			}
			// remove all but first
			if (foundMoreThanOne) {
				nodeIter = entries.iterator();
				if (!nodeIter.hasNext()) {
					// error
				} else {
					while (nodeIter.hasNext()) {
						// first iteration will go past first
						node = nodeIter.next();
						List nodeSuccs = mdg.getSuccsOf(node);
						for (Object succ : nodeSuccs) {
							mdg.removeEdge(node, succ);
						}
						//mdg.removeEdge(node);
					}
				}
			}
		} while (foundMoreThanOne);
		//g = new BriefUnitGraph();
		// / END NEW

		MHGDominatorsFinder a = new MHGDominatorsFinder(mdg);

		loops = new HashMap<Stmt, List<Stmt>>();

		
		Iterator stmtsIt = mdg.getNodes().iterator();
		while (stmtsIt.hasNext()) {
			Stmt s = (Stmt) stmtsIt.next();

			List<Unit> succs = mdg.getSuccsOf(s);
			//List<Unit> preds = mdg.getPredsOf(s);
			Collection<Unit> dominaters = (Collection<Unit>) a.getDominators(s);

			ArrayList<Stmt> headers = new ArrayList<Stmt>();

			Iterator<Unit> succsIt = succs.iterator();
			while (succsIt.hasNext()) {
				Stmt succ = (Stmt) succsIt.next();
				if (dominaters.contains(succ)) {
					// header succeeds and dominates s, we have a loop
					headers.add(succ);
					// Just to make sure 
					/*if (preds.size() < succs.size())
						break;*/
				}
			}

			Iterator<Stmt> headersIt = headers.iterator();
			while (headersIt.hasNext()) {
				Stmt header = headersIt.next();
				List<Stmt> loopBody = getLoopBodyFor(header, s);

				// for now just print out loops as sets of stmts
				// System.out.println("FOUND LOOP: Header: "+header+" Body: "+loopBody);
				if (loops.containsKey(header)) {
					// merge bodies
					List<Stmt> lb1 = loops.get(header);
					loops.put(header, union(lb1, loopBody));
				} else {
					loops.put(header, loopBody);
				}
			}
		}
	}

	private List<Stmt> getLoopBodyFor(Stmt header, Stmt node) {

		ArrayList<Stmt> loopBody = new ArrayList<Stmt>();
		Stack<Unit> stack = new Stack<Unit>();

		loopBody.add(header);
		stack.push(node);

		while (!stack.isEmpty()) {
			Stmt next = (Stmt) stack.pop();
			if (!loopBody.contains(next)) {
				// add next to loop body
				loopBody.add(0, next);
				// put all preds of next on stack
				/*
				 * if(g==null){ System.out.println("graph null"); }
				 */
				Iterator<Unit> it = mdg.getPredsOf(next).iterator();
				while (it.hasNext()) {
					stack.push(it.next());
				}
			}
		}

		/*
		 * assert (node==header && loopBody.size()==1) ||
		 * loopBody.get(loopBody.size()-2)==node; assert
		 * loopBody.get(loopBody.size()-1)==header;
		 */

		return loopBody;
	}

	
	private List<Block> getLoopBodyFor1(Block header, Block node) {

		ArrayList<Block> loopBody = new ArrayList<Block>();
		Stack<Block> stack = new Stack<Block>();

		loopBody.add(header);
		stack.push(node);

		while (!stack.isEmpty()) {
			Block next = (Block) stack.pop();
			if (!loopBody.contains(next)) {
				// add next to loop body
				loopBody.add(0, next);
				// put all preds of next on stack
				/*
				 * if(g==null){ System.out.println("graph null"); }
				 */
				Iterator<Block> it = mdg.getPredsOf(next).iterator();
				while (it.hasNext()) {
					stack.push(it.next());
				}
			}
		}

		/*
		 * assert (node==header && loopBody.size()==1) ||
		 * loopBody.get(loopBody.size()-2)==node; assert
		 * loopBody.get(loopBody.size()-1)==header;
		 */

		return loopBody;
	}

	private List<Stmt> union(List<Stmt> l1, List<Stmt> l2) {
		Iterator<Stmt> it = l2.iterator();
		while (it.hasNext()) {
			Stmt next = it.next();
			if (!l1.contains(next)) {
				l1.add(next);
			}
		}
		return l1;
	}
	
	private List<Block> union1(List<Block> l1, List<Block> l2) {
		Iterator<Block> it = l2.iterator();
		while (it.hasNext()) {
			Block next = it.next();
			if (!l1.contains(next)) {
				l1.add(next);
			}
		}
		return l1;
	}
	
	public HashReversibleGraph getGraph() {
		return mdg;
	}
}
