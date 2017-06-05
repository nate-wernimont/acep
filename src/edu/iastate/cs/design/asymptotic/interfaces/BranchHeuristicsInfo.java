package edu.iastate.cs.design.asymptotic.interfaces;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.Unit;
import soot.toolkits.graph.Block;

/**
 * This is an interface to the branch heuristics class. It is responsible
 * to match the successors taken and not taken when a heuristic matches a basic
 * block.
 * @author gupadhyaya
 *
 */
public interface BranchHeuristicsInfo {
	
	public float getProbabilityTaken (int heuristic);
	Pair<Block, Block> MatchHeuristic (int bh, Block root);
	Pair<Block, Block> MatchLoopBranchHeuristic (Block rootBasicBlock);
	Pair<Block, Block> MatchPointerHeuristic (Block rootBasicBlock);
	public float getProbabilityNotTaken (int heuristic);

}
