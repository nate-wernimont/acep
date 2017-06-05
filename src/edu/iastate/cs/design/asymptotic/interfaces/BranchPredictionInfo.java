package edu.iastate.cs.design.asymptotic.interfaces;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;

/*
 * This is an interface for branch prediction pass, responsible 
 * to find all back edges and exit edges of a function. Also it parses
 * basic blocks which contains calls and store instructions
 * This information will be used by the heuristics routines 
 * in order to calculate branch probabilities
 */
public interface BranchPredictionInfo {

	void FindBackAndExitEdges (SootMethod method);
	public void FindCallsAndStores (SootMethod method);
	public void BuildInfo (SootMethod method);
	public void Clear ();
	public int CountBackEdges (Block block);
	public Boolean CallsExit(Block basicBlock);
	public Boolean isBackEdge(Pair<Block, Block> edge);
	public Boolean isExitEdge(Pair<Block, Block> edge);
	public Boolean hasCall (Block basicBlock);
	public Boolean hasStore (Block basicBlock);
	public LoopInfo getLoopInformation ();
	public int backEdgeCount();
}
