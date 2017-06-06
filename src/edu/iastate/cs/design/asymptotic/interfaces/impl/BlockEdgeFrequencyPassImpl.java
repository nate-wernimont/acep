package edu.iastate.cs.design.asymptotic.interfaces.impl;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

//import com.sun.swing.internal.plaf.basic.resources.basic;

import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.datastructures.Path;
import edu.iastate.cs.design.asymptotic.interfaces.BlockEdgeFrequencyPass;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionInfo;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionPass;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;


/**
 *  
 This pass calculates basic block and edge frequencies based on the branch
 probability calculated previously. To calculate the block frequency, sum all
 predecessors edges reaching the block. If the block is the function entry
 block define the execution frequency of 1. To calculate edge frequencies,
 multiply the block frequency by the edge from this block to it's successors.

 To avoid cyclic problem, this algorithm propagate frequencies to edges by
 calculating a cyclic probability. More information can be found in Wu (1994).

 References:
 Youfeng Wu and James R. Larus. Static branch frequency and program profile
 analysis. In MICRO 27: Proceedings of the 27th annual international symposium
 on Microarchitecture. IEEE, 1994.
 *
 * @author gupadhyaya
 *
 */
public class BlockEdgeFrequencyPassImpl 
implements BlockEdgeFrequencyPass {

	// For loops that does not terminates, the cyclic_probability can have a
	// probability higher than 1.0, which is an undesirable condition. Epsilon
	// is used as a threshold of cyclic_probability, limiting its use below 1.0.
	static double epsilon = 0.000001;

	// Required pass to identify loop in functions.
	LoopInfo LI;

	// List of basic blocks not visited.
	Vector<Block> notVisited = new Vector<Block>();

	// List of loops visited
	Set<Loop> loopsVisited = new HashSet<Loop>();

	// Branch probabilities calculated.
	public BranchPredictionPass BPP;

	// Hold probabilities propagated to back edges
	HashMap<Pair<Block, Block>, Double> backEdgesProbabilities 
	= new HashMap<Pair<Block, Block>, Double> ();

	// Block and edge frequency information map
	HashMap<Pair<Block, Block>, Double> edgeFrequencies 
	= new HashMap<Pair<Block, Block>, Double> ();
	HashMap<Block, Double> blockFrequencies 
	= new HashMap<Block, Double> ();

	SootMethod method;
	// MarkReachable - Mark all blocks reachable from root block as not
	// visited

	public BlockEdgeFrequencyPassImpl (SootMethod method) {
		this.method = method;
		//System.out.println("BlockEdgeFrequencyPassImpl: before LoopInfo");
		LI = new LoopInfo (method);
		//System.out.println("BlockEdgeFrequencyPassImpl: after LoopInfo");
		BPP = new BranchPredictionPassImpl(LI);
		//System.out.println("BlockEdgeFrequencyPassImpl: before runOnFunction");
		runOnFunction ();
		//System.out.println("BlockEdgeFrequencyPassImpl: after runOnFunction");
		boolean verified = VerifyIntegrity(method);
		if (!verified)
			System.out.println ("Integrity Problem....");
	}
	
	public HashMap<Pair<Block, Block>, Double> getEdgeProbabilities () {
		return BPP.getEdgeProbabilities();
	}
	
	public LoopInfo getLoopInfo () {
		return LI;
	}
	
	public BranchPredictionPass getBPP () {
		return BPP;
	}

	public void display1 () {
		System.out.println ("============================================================================");
		System.out.println ("List of all edge frequencies");
		System.out.println ("============================================================================");

		Set<Entry<Pair<Block, Block>, Double>> mapEnteries = edgeFrequencies.entrySet();
		for (Iterator<Entry<Pair<Block, Block>, Double>> entryIter = 
			mapEnteries.iterator(); entryIter.hasNext();) {
			Entry<Pair<Block, Block>, Double> entry = entryIter.next();
			Pair<Block, Block> edge = entry.getKey();
			Double prob = entry.getValue();
			System.out.print(edge.first().toShortString());
			System.out.print(" -----> ");
			System.out.print(edge.second().toShortString());
			System.out.println(" = "+prob.doubleValue());
			System.out.println ("============================================================================");

		}
	}

	public void display2 () {
		System.out.println ("============================================================================");
		System.out.println ("List of all block frequencies");
		System.out.println ("============================================================================");

		Set<Entry<Block, Double>> mapEnteries = blockFrequencies.entrySet();
		for (Iterator<Entry<Block, Double>> entryIter = 
			mapEnteries.iterator(); entryIter.hasNext();) {
			Entry<Block, Double> entry = entryIter.next();
			Block block = entry.getKey();
			Double prob = entry.getValue();
			System.out.print(block.toShortString());
			System.out.println(" = "+prob.doubleValue());
			System.out.println ("============================================================================");
		}
	}

	// Methods

	public boolean runOnFunction () {
		clear ();

		// Find all loop headers of this function
		Iterator<Loop> loopsIter = LI.iterator();
		while (loopsIter.hasNext()) {
			Loop loop = loopsIter.next();
			// Since it is a loop head, add it to the list
			propagateLoop (loop);
		}
		//System.out.println("runOnFunction: after propagateLoop");
		// After calculating frequencies for all the loops, calculate the frequencies
		// for the remaining blocks by faking a loop for the function. Assuming that
		// the entry block of the function is a loop head, propagate frequencies.
		// Propagate frequencies assuming entry block is a loop head.
		Block entry = LI.first();
		markReachable(entry);
		//System.out.println("runOnFunction: after markReachable");
		propagateFreq(entry);
		//System.out.println("runOnFunction: after propagateFreq");
		// Clean up unnecessary information.
		notVisited.clear();
		loopsVisited.clear();
		backEdgesProbabilities.clear();

		return false;
	}

	/**
	 * Clear - Clear all stored information.
	 */
	private void clear () {
		notVisited.clear();
		loopsVisited.clear();
		backEdgesProbabilities.clear();
		edgeFrequencies.clear();
		blockFrequencies.clear();
	}

	public double getEdgeFrequency (Block src, Block dst) {
		return getEdgeFrequency(new Pair<Block, Block> (src, dst));
	}

	public double getEdgeFrequency (Pair<Block, Block> edge) {
		return edgeFrequencies.containsKey(edge)?
				edgeFrequencies.get(edge):0.0;
	}

	public double getBlockFrequency (Block basicBlock) {
		return blockFrequencies.containsKey(basicBlock)?
				blockFrequencies.get(basicBlock):0.0;
	}
	
	public HashMap<Block, Double> blockFrequencies () {
		return blockFrequencies;
	}
	
	public Iterator<Block> getBlockFreqIter () {
		return blockFrequencies.keySet().iterator();
	}
	
	public Iterator<Pair<Block, Block>> getEdgeFreqIter () {
		return edgeFrequencies.keySet().iterator();
	}

	public double getBackEdgeProbabilities (Pair<Block, Block> edge) {
		return backEdgesProbabilities.containsKey(edge)?
				backEdgesProbabilities.get(edge):BPP.getEdgeProbability(edge);
	}

	/**
	 * MarkReachable - Mark all blocks reachable from root block as not visited
	 * @param root
	 */
	private void markReachable (Block root) {
		// Clear the list first
		notVisited.clear();
		// Use an artificial stack
		Stack<Block> stack = new Stack();
		stack.push(root);
		// Visit all childs marking them as visited in depth-first order
		while (!stack.empty()) {
			Block block = stack.pop();
			if (notVisited.contains(block))
				continue;
			notVisited.add(block);
			// Put the new successors into the stack
			LoopInfo li = BPP.getBPI().getLoopInformation();
			List<Block> successors = li.getSuccessors(block);
			for (Iterator<Block> succIter = successors.iterator(); succIter.hasNext();) {
				stack.push(succIter.next());
			}
		}
	}

	/**
	 * PropagateLoop - Propagate frequencies from the inner most loop to the
	 * outer most loop
	 * @param loop
	 */
	private void propagateLoop (Loop loop) {
		// Check if we already processed this loop
		if (loopsVisited.contains(loop))
			return;
		// Mark the loop as visited
		loopsVisited.add(loop);
		// Find the most inner loops and process them first
		Iterator<Loop> innerIter = LI.inner(loop);
		while (innerIter.hasNext()) {
			Loop inner = innerIter.next();
			propagateLoop(inner);
		}
		//System.out.println("propagateLoop: after inner propagateLoop");
		// Find the header
		Unit head = loop.getHead();
		// Get the loop block to which the head belongs to
		Block loopHeadBlock = loop.getLoopHeadBlock ();
		// Mark as not visited all blocks reachable from the loop head
		markReachable(loopHeadBlock);
		//System.out.println("propagateLoop: after markReachable");
		// Propagate frequencies from the loop head
		propagateFreq(loopHeadBlock);
		//System.out.println("propagateLoop: after propagateFreq");
	}
	
	double getProbability (Block src, Block dst) {
		Loop loop = LI.getLoop(src, dst);
		if (loop == null) {
			return 1.0;
		}
		BranchPredictionInfo bpi = BPP.getBPI();
		double probability = 1.0;
		double succProbability = 0.0;
		for (Block block : loop.getLoopBlocks()) {
			succProbability = 0.0;
			if (block.toString().equals(dst.toString()))
				return probability;
			List<Block> succs = block.getSuccs();
			for (Block succBlock : succs) {
				if (!loop.isInLoop(succBlock))
					continue;
				Pair<Block, Block> edge = new Pair<Block, Block>(block, succBlock);
				succProbability += getBackEdgeProbabilities(edge);
			}
			probability *= succProbability;
		}
		return 1.0;
	}
	
	
	/**
	 * PropagateFreq - Compute basic block and edge frequencies by propagating
	 * frequencies
	 * @param head
	 */
	private void propagateFreq (Block head) {
		BranchPredictionInfo bpi = BPP.getBPI();
		// Use an artificial stack to avoid recursive calls to PropagateFreq
		Stack<Block> stack = new Stack<Block> ();
		stack.push(head);
		//notVisited.add(head);
		do {
			Block basicBlk = stack.pop();
			// If BB has been visited
			if (!(notVisited.contains(basicBlk)))
				continue;
			// Define the block frequency. If it's a loop head, assume it executes only once
			blockFrequencies.put(basicBlk, new Double(1.0f));
			// If it is not a loop head, calculate the block frequencies by summing all
			// edge frequencies reaching this block. If it contains back edges, take
			// into consideration the cyclic probability
			if (!basicBlk.toString().equals(head.toString())) {
				// We can't calculate the block frequency if there is a back edge still
				// not calculated
				boolean invalidEdge = false;
				List<Block> predecessors = LI.getPredecessors(basicBlk);
				for (Iterator<Block> predIter = predecessors.iterator(); predIter.hasNext();) {
					Block pred = predIter.next();
					if (notVisited.contains(pred) && !bpi.isBackEdge(new Pair<Block, Block> (pred, basicBlk))) {
						invalidEdge = true;
						//notVisited.remove(basicBlk);
						break;
					}
				}
				// There is an unprocessed predecessor edge
				if (invalidEdge)
					continue;
				// Sum the incoming frequencies edges for this block. Updated
				// the cyclic probability for back edges predecessors
				double bfreq = 0.0;
				double cyclic_probability = 0.0;
				// Verify if BB is a loop head.
				boolean isloopHead = LI.isLoopHeader(basicBlk);
				// Calculate the block frequency and the cyclic_probability in case
				// of back edges using the sum of their predecessor's edge frequencies.
				for (Iterator<Block> predIter = predecessors.iterator(); predIter.hasNext();) {
					Block pred = predIter.next();
					Pair<Block, Block> edge = new Pair<Block, Block> (pred, basicBlk);
					if (bpi.isBackEdge(edge) && isloopHead)
						cyclic_probability += (/*getProbability(basicBlk, pred) * */getBackEdgeProbabilities(edge));
					else
						bfreq += getEdgeFrequency(edge);//edgeFrequencies.get(edge);
				}
				// For loops that seems not to terminate, the cyclic probability can be
				// higher than 1.0. In this case, limit the cyclic probability below 1.0.
				if (cyclic_probability > (1.0 - epsilon))
					cyclic_probability = 1.0 - epsilon;
				// Calculate the block frequency.
				blockFrequencies.put(basicBlk, bfreq / (1.0 - cyclic_probability));
			}
			// Mark the block as visited.
			notVisited.remove(basicBlk);

			// Calculate the edges frequencies for all successor of this block.
			List<Block> successors = LI.getSuccessors(basicBlk);
			for (Iterator<Block> succIter = successors.iterator(); succIter.hasNext();) {
				Block successor = succIter.next();
				Pair<Block, Block> edge = new Pair<Block, Block> (basicBlk, successor);
				double prob = BPP.getEdgeProbability(edge);
				
				/*// Extra check to make the integrity sound
				Pair<Block, Block> backedge = new Pair<Block, Block> (basicBlk, successor);
				if ((successors.size() == 1) && bpi.isBackEdge(backedge))
					prob = 1.0;*/
				// The edge frequency is the probability of this edge times the block
				// frequency
				double efreq = prob * blockFrequencies.get(basicBlk);
				edgeFrequencies.put(edge, efreq);
				// If a successor is the loop head, update back edge probability.
				if (successor == head)
					backEdgesProbabilities.put(edge, efreq);
			}

			// Propagate frequencies for all successor that are not back edges.
			Vector<Block> backEdges = new Vector<Block>();
			for (Iterator<Block> succIter = successors.iterator(); succIter.hasNext();) {
				Block successor = succIter.next();
				Pair<Block, Block> edge = new Pair<Block, Block> (basicBlk, successor);
				if (!bpi.isBackEdge(edge))
					backEdges.add(successor);
			}
			// This was done just to ensure that the algorithm would process the
			// left-most child before, to simulate normal PropagateFreq recursive calls.
			// Forgot to reverse the backedge list
			ListIterator<Block> backEdgeListIter = backEdges.listIterator();
			while (backEdgeListIter.hasNext())
				backEdgeListIter.next();
			
			while (backEdgeListIter.hasPrevious()) {
				Block block = backEdgeListIter.previous();
				if (!stack.contains(block))
					stack.push(block);
			}
			/*
			for (Iterator<Block> backEdgeIter = backEdges.iterator(); backEdgeIter.hasNext();) {
				Block unit = backEdgeIter.next();
				if (!stack.contains(unit))
					stack.push(unit);
			}*/

		} while (!stack.empty());

	}

	/**
	 * VerifyIntegrity - The sum of frequencies of all edges leading to
	 * terminal nodes should match the entry frequency (that is always 1.0).
	 * @param method
	 * @return
	 */
	private boolean VerifyIntegrity (SootMethod method) {
		// The sum of all predecessors edge frequencies.
		double freq = 0.0;
		// If the function has only one block, then the input frequency matches
		// automatically the output frequency.
		if (LI.size() == 1)
			return true;
		// Find all terminator nodes.
		Iterator<Block> blockIter = LI.getblockIter();
		while (blockIter.hasNext()) {
			Block block = blockIter.next();
			// If the basic block has no successors, then it s a termination node.
			//List<Unit> successors = LI.getFullSuccessors(block);

			if (LI.getNumSuccessors(block) != 0) {
				// Find all predecessor edges leading to BB.
				List<Block> predecessors = LI.getPredecessors(block);
				for (Iterator<Block> predIter = predecessors.iterator(); predIter.hasNext();) {
					Block pred = predIter.next();
					// Sum the predecessors edge frequency.
					freq += getEdgeFrequency(pred, block);
				}
			}

		}
		// Check if frequency matches 1.0 (with a 0.01 slack).
		System.out.println(freq);
		return (freq > 0.99 && freq < 1.01);
	}
}
