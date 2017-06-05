package edu.iastate.cs.design.asymptotic.interfaces.impl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.interfaces.BranchHeuristics;
import edu.iastate.cs.design.asymptotic.interfaces.BranchHeuristicsInfo;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionInfo;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionPass;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * BranchPredictionPass - This class implement the branch predictor proposed
 * by Wu (1994). This class was designed as a function pass so it can be used
 * without the hassle of calculating full program branch prediction. It
 * inherits from EdgeProfileInfo to hold edges probabilities that ranges from
 * values between 0.0 and 1.0. Therefore it does not require a double
 * precision type.
 * @author gupadhyaya
 *
 */
public class BranchPredictionPassImpl implements BranchPredictionPass {

	BranchPredictionInfo BPI;
	BranchHeuristicsInfo BHI;
	LoopInfo LI;

	HashMap<Pair<Block, Block>, Double> EdgeProbabilities = new HashMap<Pair<Block, Block>, Double> ();

	//List<Pair<Unit, Unit>> something = new ArrayList<Pair<Unit, Unit>> ();
	
	public BranchPredictionPassImpl (LoopInfo li) {
		//LI = new LoopInfo (method);
		LI = li;
		//System.out.println("BranchPredictionPassImpl");
		BPI = new BranchPredictionInfoImpl (LI);
		//System.out.println("BranchPredictionPassImpl:before BranchHeuristicsInfoImpl");
		BHI = new BranchHeuristicsInfoImpl (LI, BPI);
		//System.out.println("BranchPredictionPassImpl:before init");
		init();
	}
	
	public void result () {
		System.out.println("***************Edge Probabilities****************");
		for (Iterator<Pair<Block, Block>> edgeIter = EdgeProbabilities.keySet().iterator(); edgeIter.hasNext();) {	
			Pair<Block, Block> edge = edgeIter.next();
			System.out.println("Edge : {{{"+edge.first()+"--------->"+edge.second()+"}}} =============== "+EdgeProbabilities.get(edge).doubleValue());
			
		}
	}

	/**
	 * This method calculates probability for each of the basic blocks
	 * and the method for this pass.
	 */
	private void init () {
		// Run over all basic blocks of a function calculating branch probabilities.
		Body b = LI.methodBody();
		// Run through all the basic blocks
		if( b!= null){
			BriefBlockGraph cGraph = new BriefBlockGraph(b);
			Iterator<Block> blocksIter = cGraph.iterator();
			while(blocksIter.hasNext()){
				Block block = blocksIter.next();
				CalculateBranchProbabilities(block);
			}
		}
	}
	// Methods

	// CalculateBranchProbabilities - Implementation of the algorithm proposed
	// by Wu (1994) to calculate the probabilities of all the successors of a basic block
	public void CalculateBranchProbabilities (Block basicBlock) {
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(basicBlock);
		int noOfSuccessors = successors.size();
		int noOfBackEdges = BPI.CountBackEdges(basicBlock);

		// The basic block should have some succesors to compute profile
		if (noOfSuccessors != 0) {
			// If a block calls exit, then assume that every successor of this
			// basic block is never going to be reached.
			if (BPI.CallsExit(basicBlock)) {
				// According to the paper, successors that contains an exit call have a
				// probability of 0% to be taken.
				for (Iterator<Block> succIter = successors.iterator(); succIter.hasNext();) {
					Block successorBlk = succIter.next();
					Pair<Block, Block> edge = new Pair<Block, Block> (basicBlock, successorBlk);
					EdgeProbabilities.put(edge, new Double(0.0f));
				}
			} else if (noOfBackEdges > 0 && noOfBackEdges <= noOfSuccessors) {
				// Has some back edges, but not all.
				for (Iterator<Block> succIter = successors.iterator(); succIter.hasNext();) {
					Block successorBlk = succIter.next();
					Pair<Block, Block> edge = new Pair<Block, Block> (basicBlock, successorBlk);
					if (BPI.isBackEdge(edge)) {
						float probabilityTaken = (float) BHI.getProbabilityTaken(
								BranchHeuristics.LOOP_BRANCH_HEURISTIC) / noOfBackEdges;
						EdgeProbabilities.put(edge, new Double(probabilityTaken));
					} else {
						// The other edge, the one that is not a back edge, is in most cases
						// an exit edge. However, there are situations in which this edge is
						// an exit edge of an inner loop, but not for the outer loop. So,
						// consider the other edges always as an exit edge.
						float probabilityNotTaken = (float) BHI.getProbabilityNotTaken(
								BranchHeuristics.LOOP_BRANCH_HEURISTIC) / (noOfSuccessors - noOfBackEdges);
						EdgeProbabilities.put(edge, new Double(probabilityNotTaken));
					}
				}
			} else if (noOfBackEdges > 0 || noOfSuccessors != 2) {
				// This part handles the situation involving switch statements.
				// Every switch case has a equal likelihood to be taken.
				// Calculates the probability given the total amount of cases clauses.
				for (Iterator<Block> succIter = successors.iterator(); succIter.hasNext();) {
					Block successorBlk = succIter.next();
					Pair<Block, Block> edge = new Pair<Block, Block> (basicBlock, successorBlk);
					float probability = 1.0f / (float) noOfSuccessors;
					EdgeProbabilities.put(edge, new Double(probability));
				}
			} else {
				// Here we can only handle basic blocks with two successors (branches).
				// Identify the two branch edges.
				Block trueSuccessor = successors.get(0);
				Block falseSuccessor = successors.get(1);

				Pair<Block, Block> trueEdge = new Pair<Block, Block> (basicBlock, trueSuccessor);
				Pair<Block, Block> falseEdge = new Pair<Block, Block> (basicBlock, falseSuccessor);

				// Initial branch probability. If no heuristic matches, than each edge
				// has a likelihood of 50% to be taken.
				EdgeProbabilities.put(trueEdge, new Double(0.5f));
				EdgeProbabilities.put(falseEdge, new Double(0.5f));

				// Run over all heuristics implemented in BranchHeuristics class.
				for (int bh = 0; bh < 9; bh++) {
					Pair<Block, Block> prediction = BHI.MatchHeuristic(bh, basicBlock);

					// Heuristic did not match
					if (prediction == null)
						continue;

					// Recalculate edge probability
					if (prediction.first() != null)
						addEdgeProbability (bh, basicBlock, prediction);
				}
			}
		}


	}
	
	public BranchPredictionInfo getBPI () {
		return BPI;
	}

	/**
	 * getEdgeProbability - Find the edge probability. If the edge is not
	 * found, return 1.0 (probability of 100% of being taken).
	 */
	public Double getEdgeProbability (Pair<Block, Block> edge) {
		// If edge was found, return it. Otherwise return the default value,
		// meaning that there is no profile known for this edge. The default value
		// is 1.0, meaning that the branch is taken with 100% likelihood.
		
		for (Iterator<Pair<Block, Block>> edgeIter = EdgeProbabilities.keySet().iterator(); edgeIter.hasNext();) {	
			Pair<Block, Block> stored_edge = edgeIter.next();
			if(stored_edge.toString().equals(edge.toString()))
				return EdgeProbabilities.get(stored_edge).doubleValue();
			
		}
		return new Double(1.0f);
	}

	/**
	 * addEdgeProbability - If a heuristic matches, calculates the edge
	 * probability combining previous predictions acquired.
	 * @param heuristic
	 * @param root
	 * @param prediction
	 */
	public void addEdgeProbability(int heuristic, Block root,
			Pair<Block, Block> prediction) {
		Block successorTaken = prediction.first();
		Block successorNotTaken = prediction.second();

		// Get the edges.
		Pair<Block, Block> edgeTaken = new Pair<Block, Block> (root, successorTaken);
		Pair<Block, Block> edgeNotTaken = new Pair<Block, Block> (root, successorNotTaken);

		// The new probability of those edges.
		Double probTaken = new Double(BHI.getProbabilityTaken(heuristic));
		Double probNotTaken = new Double(BHI.getProbabilityNotTaken(heuristic));
		
		// The old probability of those edges.
		Double oldProbTaken = getEdgeProbability(edgeTaken);
		Double oldProbNotTaken = getEdgeProbability(edgeNotTaken);
		
		// Combined the newly matched heuristic with the already given
		// probability of an edge. Uses the Dempster-Shafer theory to combine
		// probability of two events to occur simultaneously.
		Double d = oldProbTaken * probTaken + oldProbNotTaken * probNotTaken;
		
		EdgeProbabilities.put(edgeTaken, (float)(oldProbTaken * probTaken)/d);
		EdgeProbabilities.put(edgeNotTaken, (float)(oldProbNotTaken * probNotTaken)/d);
	}
	
	public HashMap<Pair<Block, Block>, Double> getEdgeProbabilities () {
		return EdgeProbabilities;
	}


}
