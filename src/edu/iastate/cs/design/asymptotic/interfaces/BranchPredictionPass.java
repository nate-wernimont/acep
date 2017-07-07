package edu.iastate.cs.design.asymptotic.interfaces;
import java.util.HashMap;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.toolkits.graph.Block;

/**
 * This class describe the branch prediction pass that calculates edges
 * probabilities for all edges of a given function. The probabilities
 * represents the likelihood of an edge to be taken.
 * @author gupadhyaya
 *
 */
public interface BranchPredictionPass {
	
	public BranchPredictionInfo getBPI ();
	public Double getEdgeProbability (Pair<Block, Block> edge);
	public HashMap<Pair<Block, Block>, Double> getEdgeProbabilities ();

}
