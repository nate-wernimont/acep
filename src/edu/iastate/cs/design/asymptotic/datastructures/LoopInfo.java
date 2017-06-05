package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class LoopInfo {

	// List<SootLoopStmt> loops = new ArrayList<SootLoopStmt> ();

	private SootMethod method;
	private Body b;
	//private ExceptionalUnitGraph eug;
	private BriefUnitGraph eug;
	private BriefBlockGraph cGraph;
	private DirectedGraph graph;
	private DirectedGraph cleanGraph;

	private Set<Unit> params = new HashSet<Unit>();

	// <header, loopBody>
	private Collection<Loop> loops;

	public LoopInfo(SootMethod method) {
		this.method = method;
		init();

	}

	public Iterator<Block> getblockIter() {
		return cGraph.iterator();
	}

	private void init() {
		b = method.retrieveActiveBody();
		//eug = new ExceptionalUnitGraph(b);
		eug = new BriefUnitGraph(b);
		cGraph = new BriefBlockGraph(b);
		LoopFinder lf = new LoopFinder();
		lf.internalTransform(b, eug);
		//lf.internalTransform(b, cGraph);
		loops = lf.loops();
		graph = lf.getGraph();
		cleanGraph = lf.cleanGraph(cGraph); //
		// Update loops with block information
		updateLoops();
		// Set the params
		setParams();

		// buildloops(this.method);
	}

	public boolean isParam(Unit unit) {
		return params.contains(unit);
	}
	
	public boolean inputVariant() {
		if (params.size() > 0)	return true;
		return false;
	}

	private void setParams() {
		Iterator<Unit> unitsIter = eug.iterator();
		while (unitsIter.hasNext()) {
			Unit unit = unitsIter.next();
			if (unit instanceof IdentityStmt) {
				Value value = (Value) ((IdentityStmt) unit).getRightOp();
				if (value instanceof ParameterRef) {
					params.add(unit);
				}
			}
		}
	}

	public boolean isParam(Value value) {
		Iterator<Unit> paramsIter = params.iterator();
		while (paramsIter.hasNext()) {
			Unit unit = paramsIter.next();
			if (unit instanceof IdentityStmt) {
				Value val = (Value) ((IdentityStmt) unit).getLeftOp();
				if (val.equals(value))
					return true;
			}
		}
		return false;
	}

	public boolean isUsedInBasicBlock(Block block) {
		Iterator<Unit> blkUnitIter = block.iterator();
		while (blkUnitIter.hasNext()) {
			Unit blkUnit = blkUnitIter.next();
			for (ValueBox vb : blkUnit.getUseBoxes()) {
				Value v = vb.getValue();
				if (isParam(v))
					return true;
			}
		}
		return false;
	}

	private void updateLoops1() {
		// for each loop update blocks information
		Iterator<Loop> loopsIter = loops.iterator();
		Iterator<Block> blocksIter = cGraph.iterator();
		Iterator<Block> blksIterBackUp = blocksIter;
		while (loopsIter.hasNext()) {
			Loop loop = loopsIter.next();
			// Re-initialize blocksIter
			blocksIter = blksIterBackUp;
			// Get all the loop units and check to which block the units belong
			// to
			Iterator<Stmt> loopStmtIter = loop.getLoopStatements().iterator();
			Iterator<Stmt> loopStmtIterBackUp = loopStmtIter;
			while (blocksIter.hasNext()) {
				Block blk = blocksIter.next();
				loopStmtIter = loopStmtIterBackUp;
				Iterator<Unit> blkUnitsIter = blk.iterator();
				while ((loopStmtIter.hasNext() && blkUnitsIter.hasNext())
						&& (loopStmtIter.next().equals(blkUnitsIter.next())))
					;
				if (!(loopStmtIter.hasNext() && blkUnitsIter.hasNext())) {
					break;
				}
			}

		}
	}

	private List<Block> failedCaseLoopConstruction(List<Stmt> missedloopStmts,
			List<Block> blocks) {
		int i = 0, pos = 0, j = 0;
		boolean matched = false;
		int failed_attempts = 0;
		List<Block> loopBlocks = new ArrayList<Block>();
		while (i < missedloopStmts.size()) {
			// reset matched
			matched = false;
			// resent the blocks position if j > blocks.size
			if (j >= blocks.size()) {
				j = 0;
				failed_attempts++;
				if (failed_attempts >= blocks.size()) {
					break;
				}
			}
			Block blk = blocks.get(j++);
			Iterator<Unit> blkUnitsIter = blk.iterator();
			pos = i;
			while ((blkUnitsIter.hasNext() && (i < missedloopStmts.size()))
					&& missedloopStmts.get(i++).equals(blkUnitsIter.next()))
				if (!blkUnitsIter.hasNext())
					matched = true;
			if (!blkUnitsIter.hasNext() && matched) {
				loopBlocks.add(blk);
			} else {
				// reset i = pos and check for other blocks
				i = pos;
			}
		}
		return loopBlocks;
	}

	private void updateLoops() {
		Iterator<Loop> loopsIter = loops.iterator();
		List<Loop> remove = new ArrayList<Loop>();
		while (loopsIter.hasNext()) {
			Loop loop = loopsIter.next();
			List<Block> loopBlocks = new ArrayList<Block>();
			List<Stmt> loopStatements = loop.getLoopStatements();
			int i = 0, pos = 0, j = 0;
			boolean matched = false;
			int failed_attempts = 0;
			List<Stmt> missedStmts = new ArrayList<Stmt>();
			List<Block> blocks = cGraph.getBlocks();
			// Iterator<Block> blocksIter = cGraph.iterator();
			while (i < loopStatements.size()) {
				// reset matched
				matched = false;
				// resent the blocks position if j > blocks.size
				if (j >= blocks.size()) {
					j = 0;
					// Need to find out better algorithm
					failed_attempts++;
					if (failed_attempts >= blocks.size()) {
						failed_attempts = 0;
						missedStmts.add(loopStatements.get(i++));
					}
				}
				Block blk = blocks.get(j++);
				Iterator<Unit> blkUnitsIter = blk.iterator();
				pos = i;
				while ((blkUnitsIter.hasNext() && (i < loopStatements.size()))
						&& loopStatements.get(i++).equals(blkUnitsIter.next()))
					if (!blkUnitsIter.hasNext())
						matched = true;
				if (!blkUnitsIter.hasNext() && matched) {
					loopBlocks.add(blk);
				} else {
					// reset i = pos and check for other blocks
					i = pos;
				}
			}
			// Falied loopstatements
			if (missedStmts.size() > 0) {
				List<Block> missed = failedCaseLoopConstruction(missedStmts, blocks);
				/*if (missed.size() == 0) {
					// disgard the loop
					//loops.remove(loop);
					remove.add(loop);
					continue;
				}*/
				loopBlocks.addAll(missed);
			}
			loop.setLoopBlcoks(loopBlocks);
		}
		
		loops.removeAll(remove);

	}

	public SootMethod method() {
		return method;
	}

	public Body methodBody() {
		return b;
	}

	public UnitGraph getMethodGraph() {
		return eug;
	}
	
	public DirectedGraph getDirectedGraph() {
		return graph;
	}
	
	public DirectedGraph<Block> getCleanGraph () {
		return cleanGraph;
	}

	public Iterator<Loop> inner(Loop root) {
		Collection<Loop> inner = new HashSet<Loop>();
		List<Stmt> stmts = root.getLoopStatements();
		for (Iterator<Stmt> stmtIter = stmts.iterator(); stmtIter.hasNext();) {
			Stmt stmt = stmtIter.next();
			// First statement in loopStatements is loop header, so skip it
			if (stmt.equals(root.getHead()))
				continue;
			Loop l;
			if ((l = nestedLoop(stmt)) != null)
				inner.add(l);
		}
		return inner.iterator();
	}

	public boolean isLoopHeader(Unit block) {
		for (Iterator<Loop> loopsIter = loops.iterator(); loopsIter.hasNext();) {
			Loop loop = loopsIter.next();
			if (loop.getHead().toString().equals(block.toString())) {
				return true;
			}
		}
		return false;
	}

	public boolean isLoopHeader(Block block) {
		for (Iterator<Loop> loopsIter = loops.iterator(); loopsIter.hasNext();) {
			Loop loop = loopsIter.next();
			if (loop.getLoopHeadBlock().toString().equals(block.toString())) {
				return true;
			}
		}
		return false;
	}

	public Loop returnContainingLoop(Unit block) {
		for (Iterator<Loop> loopsIter = loops.iterator(); loopsIter.hasNext();) {
			Loop loop = loopsIter.next();
			if (loop.isInLoop(block))
				return loop;
		}
		return null;
	}

	public Loop returnContainingLoop(Block block) {
		for (Iterator<Loop> loopsIter = loops.iterator(); loopsIter.hasNext();) {
			Loop loop = loopsIter.next();
			if (loop.isInLoop(block))
				return loop;
		}
		return null;
	}

	public Loop nestedLoop(Stmt block) {
		for (Iterator<Loop> loopsIter = loops.iterator(); loopsIter.hasNext();) {
			Loop loop = loopsIter.next();
			if (loop.getHead().equals(block)) {
				return loop;
			}
		}
		return null;
	}

	public boolean loopPreHeader(Block preHead) {
		for (Iterator<Loop> loopsIter = loops.iterator(); loopsIter.hasNext();) {
			Loop loop = loopsIter.next();
			Unit head = loop.getHead();
			Block loopHeadBlock;
			// Get the block to which loophead belongs to
			Iterator<Block> loopBlkIter = loop.loopIterator();
			while (loopBlkIter.hasNext()) {
				Block blk = loopBlkIter.next();
				List<Block> preds = getPredecessors(blk);
				List<Block> loopBlocks = loop.getLoopBlocks();
				List<Block> filtered_preds = new ArrayList<Block>();
				// Iterator<Block> loopBlockIter = loop.loopIterator();
				for (Iterator<Block> predIter = preds.iterator(); predIter
						.hasNext();) {
					Block predBlock = predIter.next();
					// preds.remove(loopBlock);
					if (!loopBlocks.contains(predBlock))
						filtered_preds.add(predBlock);
				}
				// preds.removeAll(loop.getLoopBlocks());
				if ((filtered_preds.size() == 1)
						&& (filtered_preds.get(0).toString().equals(preHead
								.toString())))
					return true;
			}
		}
		return false;
		/*
		 * if (blk.toString().equals(preHead.toString())) { Iterator<Unit>
		 * BlkUnitsIter = blk.iterator(); while (BlkUnitsIter.hasNext()) { if
		 * (BlkUnitsIter.next().toString().equals(head.toString())) { // Get all
		 * the predecessors of head List<Block> preds = getPredecessors(blk); //
		 * Need to get rid of all loop blocks from the preds list // Because
		 * prehead is the one which is the only // non-loop pred
		 * preds.removeAll(loop.getLoopBlocks()); // If the preHead is the only
		 * predecessor then it is // pre-header if ((preds.size() == 1) &&
		 * preds.contains(preHead)) return true; } } } } } return false;
		 */
	}

	/*
	 * private void buildloops (SootMethod method) {
	 * 
	 * }
	 * 
	 * public void caseIfStmt(IfStmt stmt){ SootLoopStmt loop;
	 * 
	 * if((loop = breakdownStdLoop(stmt, eug)) != null ){ loops.add(loop); } }
	 * 
	 * private SootLoopStmt breakdownStdLoop(IfStmt stmt, ExceptionalUnitGraph
	 * eug){ Unit start = null; IfStmt end; Unit inc; // Calculate loop body
	 * List<Unit> emptyList = new ArrayList<Unit>(); List<Unit> entireLoop =
	 * generateFullSuccessors( stmt, eug, emptyList); List<Unit> loopPreds =
	 * generateFullPredecessors(stmt, eug, emptyList); List<Unit> loopBody;
	 * entireLoop.retainAll(loopPreds);
	 * 
	 * if(entireLoop.contains(stmt)){ // Set end end = stmt;
	 * 
	 * // Set start which is predecessor of end NOT in the loop List<Unit>
	 * endPreds = eug.getPredsOf(end); // End must only have 2 preds --
	 * unconditional jump in and increment final int endPredsSize =
	 * endPreds.size(); if(endPredsSize != 2){
	 * //Logging.trace("Misformed std loop -- too many preds on end"); return
	 * null; } boolean found = false; int jumpInLocation = -1; for(int i = 0; i
	 * < endPredsSize && endPredsSize == 2; i++){ Unit jumpIn = endPreds.get(i);
	 * if(!entireLoop.contains(jumpIn)){ Unit loopEntry =
	 * getSuccessorInLoop(jumpIn, entireLoop, eug); if(loopEntry != null){
	 * jumpInLocation = i; if(loopEntry != end){ // If this is the case, it is
	 * not a for-loop
	 * //Logging.trace("Misformed std loop -- possible 2 entry points"); return
	 * null; } List<Unit> jumpInPreds = eug.getPredsOf(jumpIn); // Must only
	 * have 1 pred b/c it must be "start" condition if (jumpInPreds.size() ==
	 * 1){ start = jumpInPreds.get(0); found = true; break; } } } } if(!found){
	 * //Logging.trace("Misformed std loop -- couldn't find entry point");
	 * return null; }
	 * 
	 * // Set inc which is predecessor of end IN loop int incLocation =
	 * jumpInLocation == 0 ? 1 : 0; inc = endPreds.get(incLocation);
	 * 
	 * // Calculate loop body we care about loopBody = new
	 * ArrayList<Unit>(entireLoop); loopBody.remove(inc); loopBody.remove(end);
	 * 
	 * // if(SystemConstants.DEBUG){ // System.out.println("std Start - " +
	 * start.toString()); // System.out.println("std End   - " +
	 * end.toString()); // System.out.println("std Inc   - " + inc.toString());
	 * // System.out.println("std Body  - " + loopBody.toString()); // }
	 * 
	 * // TODO: Make sure body doesn't define i // TODO: make sure loop is
	 * affine -- // allow +1 // allow +n if n is not defined in loop
	 * 
	 * /* if(containsJumpsOutOfLoop(loopBody, entireLoop, eug)){
	 * //Logging.trace("Misformed std loop -- breaks"); return null; }
	 * 
	 * //TODO: Real values. return new SootLoopStmt(start, inc, end, loopBody);
	 * 
	 * } return null; }
	 */

	// public List<Unit> getFullSuccessors(Unit u) {
	// return getSuccessors(u);
	// // return generateFullSuccessors(u, eug, null);
	// }
	//
	// public List<Unit> getFullPredecessors(Unit u) {
	// return getPredecessors(u);
	// // return generateFullPredecessors(u, eug, null);
	// }
	//
	// public List<Unit> getSuccessors(Unit u) {
	// List<Unit> successors = new ArrayList<Unit>();
	// successors.addAll(eug.getSuccsOf(u));
	// return successors;
	// }
	//
	// public List<Unit> getPredecessors(Unit u) {
	// List<Unit> predecessors = new ArrayList<Unit>();
	// predecessors.addAll(eug.getPredsOf(u));
	// return predecessors;
	// }
	//
	// /**
	// * @param u
	// * @param eug
	// * @return
	// */
	// private List<Unit> generateFullSuccessors(Unit u, ExceptionalUnitGraph
	// eug,
	// List<Unit> toIgnore) {
	// List<Unit> successors = new ArrayList<Unit>();
	// List<Unit> worklist = new ArrayList<Unit>();
	// worklist.addAll(eug.getSuccsOf(u));
	// if (toIgnore != null)
	// worklist.removeAll(toIgnore);
	// while (!worklist.isEmpty()) {
	// Unit currUnit = worklist.get(0);
	// successors.add(currUnit);
	// worklist.remove(currUnit);
	// // if(currUnit.fallsThrough() || currUnit.branches()){
	// List<Unit> toAdd = new ArrayList<Unit>(eug.getSuccsOf(currUnit));
	// if (toIgnore != null)
	// toAdd.removeAll(toIgnore);
	// worklist.addAll(toAdd);
	// worklist.removeAll(successors);
	// }
	// return successors;
	// }

	// public Unit first() {
	// return eug.iterator().hasNext() ? eug.iterator().next() : null;
	// }

	public Block first() {
		return cGraph.iterator().hasNext() ? cGraph.iterator().next() : null;
	}

	public int size() {
		return eug.size();
	}

	/**
	 * @param u
	 * @param eug
	 * @return
	 */
	// private List<Unit> generateFullPredecessors(Unit u,
	// ExceptionalUnitGraph eug, List<Unit> toIgnore) {
	// List<Unit> predecessors = new ArrayList<Unit>();
	// List<Unit> worklist = new ArrayList<Unit>();
	// if (toIgnore != null)
	// if (!toIgnore.contains(u))
	// worklist.addAll(eug.getPredsOf(u));
	// while (!worklist.isEmpty()) {
	// Unit currUnit = worklist.get(0);
	// predecessors.add(currUnit);
	// worklist.remove(currUnit);
	// if (toIgnore != null)
	// if (toIgnore.contains(currUnit))
	// continue;
	// List<Unit> toAdd = new ArrayList<Unit>(eug.getPredsOf(currUnit));
	// // toAdd.removeAll(toIgnore);
	// worklist.addAll(toAdd);
	// worklist.removeAll(predecessors);
	// }
	// return predecessors;
	// }
	//
	// private Unit getSuccessorInLoop(Unit u, List<Unit> loop,
	// ExceptionalUnitGraph eug) {
	// List<Unit> possibleEntryNodes = eug.getSuccsOf(u); // entry node is
	// // x=succ(n) s.t. x
	// // in loop
	// Unit entryNode = possibleEntryNodes.get(0);
	// // if entry not in loop, cannot be entry node
	// if (!loop.contains(entryNode)) {
	// if (possibleEntryNodes.size() > 1) { // Entry node is actually the
	// // other
	// entryNode = possibleEntryNodes.get(1);
	// if (!loop.contains(entryNode)) {
	// entryNode = null;
	// }
	// } else {
	// entryNode = null;
	// }
	// }
	// return entryNode;
	// }
	//
	public int getNumSuccessors(Block u) {
		return getSuccessors(u).size();
	}

	public List<Block> getSuccessors(Block block) {
		return block.getSuccs();
	}

	public List<Block> getPredecessors(Block block) {
		return block.getPreds();
	}

	public Iterator<Loop> iterator() {
		return loops.iterator();
	}

	public Iterator<Unit> blockIterator() {
		return eug.iterator();
	}

	public UnitGraph getGraph() {
		return eug;
	}
	
	public Loop getLoop (Block head, Block tail) {
		for (Loop loop : loops) {
			Stmt headStmt = loop.header;
			Stmt tailStmt = loop.backJump;
			boolean hasHead = false, hasTail = false;
			Iterator<Unit> blkUnitsIter = head.iterator();
			while (blkUnitsIter.hasNext()) {
				Unit unit = blkUnitsIter.next();
				if (unit.toString().equals(headStmt.toString())) {
					hasHead = true;
					break;
				}
			}
			blkUnitsIter = tail.iterator();
			while (blkUnitsIter.hasNext()) {
				Unit unit = blkUnitsIter.next();
				if (unit.toString().equals(tailStmt.toString())) {
					hasTail = true;
					break;
				}
			}
			if (hasHead && hasTail)
				return loop;
		}
		return null;
	}

	public BriefBlockGraph getBlockGraph() {
		return cGraph;
	}
	
	public int loops () {
		return loops.size();
	}

}
