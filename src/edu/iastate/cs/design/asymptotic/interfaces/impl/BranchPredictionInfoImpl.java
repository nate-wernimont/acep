package edu.iastate.cs.design.asymptotic.interfaces.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionInfo;

import soot.Body;
import soot.NormalUnitPrinter;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.JastAddJ.AssignExpr;
import soot.baf.StoreInst;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * Implementation of the auxiliary class for branch predictor routines. It
 * calculates back edges and exit edges for functions and also find which basic
 * blocks contains calls and store instructions.
 * 
 * @author gupadhyaya
 * 
 */

public class BranchPredictionInfoImpl implements BranchPredictionInfo {

	Set<Pair<Block, Block>> listBackEdges, listExitEdges;
	HashMap<Unit, Integer> backEdgesCount = new HashMap<Unit, Integer>();
	Set<Block> listCalls, listStores;

	LoopInfo LI;

	public BranchPredictionInfoImpl(LoopInfo LI) {
		this.LI = LI;
		listBackEdges = new HashSet<Pair<Block, Block>>();
		listExitEdges = new HashSet<Pair<Block, Block>>();
		listCalls = new HashSet<Block>();
		listStores = new HashSet<Block>();
		BuildInfo(LI.method());
	}

	/**
	 * FindBackAndExitEdges - Search for back and exit edges for all blocks
	 * within the function loops, calculated using loop information.
	 */
	public void FindBackAndExitEdges(SootMethod method) {
		Collection<Loop> loopsVisited = new HashSet<Loop>();
		Collection<Unit> blocksVisited = new HashSet<Unit>();
		Iterator<Loop> loopsIter = LI.iterator();
		//System.out.println("Number of loops: "+LI.size());
		while (loopsIter.hasNext()) {
			Loop root = loopsIter.next();
			Unit loopHeader = root.getHead();

			// Check if we already visited this loop.
			if (loopsVisited.contains(loopHeader))
				continue;

			// Create a stack to hold loops (inner most on the top).
			Stack<Loop> stack = new Stack<Loop>();
			Collection<Unit> inStack = new HashSet<Unit>();

			// Put the current loop into the Stack.
			stack.push(root);
			inStack.add(loopHeader);

			do {
				Loop loop = stack.peek();

				// Search for new inner loops.
				Boolean foundNew = false;
				Iterator<Loop> innerIter = LI.inner(loop);
				while (innerIter.hasNext()) {
					Loop inner = innerIter.next();
					Unit innerHeader = inner.getHead();
					//System.out.println("found inner-loop");
					// Skip visited inner loops.
					if (!loopsVisited.contains(inner)) {
						stack.push(inner);
						inStack.add(innerHeader);
						foundNew = true;
						break;
					}
				}

				// If a new loop is found, continue.
				// Otherwise, it is time to expand it, because it is the most
				// inner loop
				// yet unprocessed.
				if (foundNew)
					continue;
				//System.out.println("processing loop");
				// The variable "loop" is now the unvisited inner most loop.
				Unit header = loop.getHead();

				// List<Stmt> blocks = loop.getLoopStatements();

				// List<Block> loopBlocks = loop.getLoopBlocks();

				Iterator<Block> blocksIter = loop.loopIterator();

				// Search for all basic blocks on the loop.
				while (blocksIter.hasNext()) {
					Block block = blocksIter.next();

					// Ignore the first loop statement, as it is loop head
					// if (block.equals(header))
					// continue;

					// if(blocksVisited.contains(block))
					// continue;

					List<Block> successors = LI.getSuccessors(block);

					// For each loop block successor, check if the block
					// pointing is
					// outside the loop.
					for (Iterator<Block> succIter = successors.iterator(); succIter
							.hasNext();) {
						Block successor = succIter.next();
						Pair<Block, Block> edge = new Pair<Block, Block>(block,
								successor);

						// // If the successor matches any loop header on the
						// stack, then Back edge
						if (inStack.contains(successor.getHead()))
							listBackEdges.add(edge);

						// If the successor is not present in the loop block
						// list,
						// then it is an exit edge
						// if(loop.jumpsOutOfLoop(block, LI.getGraph()))
						// listExitEdges.add(edge);
						if (!loop.contains(successor))
							listExitEdges.add(edge);
					}
				}

				// Cleaning the visited loop.
				loopsVisited.add(loop);
				stack.pop();
				inStack.remove(header);
				//System.out.println("Processing next loop: do-while");
			} while (!stack.isEmpty());
		}

	}

	/**
	 * FindCallsAndStores - Search for call and store instruction on basic
	 * blocks.
	 */
	public void FindCallsAndStores(SootMethod method) {

		Body b = LI.methodBody();
		// Run through all the basic blocks
		if (b != null) {
			BriefBlockGraph cGraph = new BriefBlockGraph(b);
			Iterator<Block> blocks = cGraph.iterator();
			while (blocks.hasNext()) {
				// boolean calls = false, stores = false;
				Block blk = blocks.next();
				boolean hasCalls = false, hasStore = false;

				// if (blk instanceof StoreInst)
				// listStores.add(blk);

				// If the terminator instruction is an InvokeInstruction, add it
				// directly.
				Unit tailUnit = blk.getTail();
				for (ValueBox vb : tailUnit.getUseBoxes()) {
					Value v = vb.getValue();
					// Needs to check if something more has to be done here
					if (v instanceof InvokeExpr) {
						listCalls.add(blk);
						hasCalls = true;
					}
				}

				// Get all the units in the block
				Iterator<Unit> blkUnitIter = blk.iterator();
				while (blkUnitIter.hasNext()) {
					Unit blkUnit = blkUnitIter.next();

					if (hasCalls && hasStore)
						break;

					// if (blkUnit instanceof AssignStmt) {
					// System.out.println ("Yes");
					// }
					
					for (ValueBox vb : (List<ValueBox>)blkUnit.getUseAndDefBoxes()) {
						Value v = vb.getValue();
						// Needs to check if something more has to be done here
						if ((v instanceof InvokeExpr) && !hasCalls) {
							listCalls.add(blk);
							hasCalls = true;
						} else if ((blkUnit instanceof AssignStmt)
								&& (v instanceof JimpleLocal) && !hasStore) {
							listStores.add(blk);
							hasStore = true;
						}
					}
				}
			}
		}

	}

	/**
	 * BuildInfo will be invoked by the constructor
	 */
	public void BuildInfo(SootMethod method) {
		Clear();
		//System.out.println("BuildInfo");
		FindBackAndExitEdges(method);
		//System.out.println("BuildInfo: after FindBackAndExitEdges");
		FindCallsAndStores(method);
		//System.out.println("BuildInfo: after FindCallsAndStores");
	}

	/**
	 * Clear all the stored maps
	 */
	public void Clear() {
		listBackEdges.clear();
		listExitEdges.clear();
		listCalls.clear();
		listStores.clear();
		backEdgesCount.clear();
	}

	public int CountBackEdges(Block block) {
		int count = 0;
		for (Iterator<Pair<Block, Block>> edgeIter = listBackEdges.iterator(); edgeIter
				.hasNext();) {
			Pair<Block, Block> edge = edgeIter.next();
			if (edge.first().toString().equals(block.toString())) //|| edge.second().toString().equals(block.toString())
				count++;
		}
		return count;// backEdgesCount.get(block);
	}

	public Boolean CallsExit(Block basicBlock) {
		return false;
	}

//	public final Boolean isBackEdge(Block start, Block finish) {
//		for (Iterator<Pair<Block, Block>> edgeIter = listBackEdges.iterator(); edgeIter
//				.hasNext();) {
//			Pair<Block, Block> edgeLocal = edgeIter.next();
//			
//			if (edge.toString().equals(edgeLocal.toString()))
//				return true;
//		}
//		return false;
//	}

	public final Boolean isBackEdge(Pair<Block, Block> edge) {
		for (Iterator<Pair<Block, Block>> edgeIter = listBackEdges.iterator(); edgeIter
				.hasNext();) {
			Pair<Block, Block> edgeLocal = edgeIter.next();
			if (edge.toString().equals(edgeLocal.toString()))
				return true;
		}
		return false;
	}

	public Boolean isExitEdge(Pair<Block, Block> edge) {
		for (Iterator<Pair<Block, Block>> edgeIter = listExitEdges.iterator(); edgeIter
				.hasNext();) {
			Pair<Block, Block> edgeLocal = edgeIter.next();
			if (edge.toString().equals(edgeLocal.toString()))
				return true;
		}
		return false;
	}

	public Boolean hasCall(Block basicBlock) {
		// if (listCalls.contains(basicBlock))
		// return true;
		for (Iterator<Block> blockIter = listCalls.iterator(); blockIter
				.hasNext();) {
			Block listBlock = blockIter.next();
			if (basicBlock.toString().equals(listBlock.toString())) {
				return true;
			}
		}

		return false;
	}

	public Boolean hasStore(Block basicBlock) {
		for (Iterator<Block> blockIter = listStores.iterator(); blockIter
				.hasNext();) {
			Block listBlock = blockIter.next();
			if (basicBlock.toString().equals(listBlock.toString())) {
				return true;
			}
		}
		return false;
	}

	public LoopInfo getLoopInformation() {
		return LI;
	}

	public int backEdgeCount() {
		return listBackEdges.size();
	}

	public int exitEdgeCount() {
		return listExitEdges.size();
	}

	public int callsCount() {
		return listCalls.size();
	}

	public int storeCount() {
		return listStores.size();
	}

	// Extra methods, will be removed later

	public void displayResult() {
		displayBackEdgeInfo();
		displayExitEdgeInfo();
		displayCallsInfo();
		displayStoreInfo();
	}

	public void displayBackEdgeInfo() {
		System.out
				.println("============================================================================");
		System.out.println("List of all back edges");
		System.out
				.println("============================================================================");
		for (Iterator<Pair<Block, Block>> edgeIter = listBackEdges.iterator(); edgeIter
				.hasNext();) {
			// Unit u = callsIter.next();
			Pair<Block, Block> edge = edgeIter.next();
			System.out.println(edge.toString());
		}
	}

	public void displayExitEdgeInfo() {
		System.out
				.println("============================================================================");
		System.out.println("List of all exit edges");
		System.out
				.println("============================================================================");
		for (Iterator<Pair<Block, Block>> edgeIter = listExitEdges.iterator(); edgeIter
				.hasNext();) {
			// Unit u = callsIter.next();
			Pair<Block, Block> edge = edgeIter.next();
			System.out.println(edge.toString());
		}
	}

	public void displayCallsInfo() {
		System.out
				.println("============================================================================");
		System.out.println("List of all calls");
		System.out
				.println("============================================================================");
		for (Iterator<Block> callsIter = listCalls.iterator(); callsIter
				.hasNext();) {
			Block blk = callsIter.next();
			System.out.println(blk.toString());
		}
	}

	public void displayStoreInfo() {
		System.out
				.println("============================================================================");
		System.out.println("List of all stores");
		System.out
				.println("============================================================================");
		for (Iterator<Block> storeIter = listStores.iterator(); storeIter
				.hasNext();) {
			Block blk = storeIter.next();
			System.out.println(blk.toString());
		}
	}

}
