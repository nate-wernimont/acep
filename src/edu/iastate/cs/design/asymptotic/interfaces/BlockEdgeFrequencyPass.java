package edu.iastate.cs.design.asymptotic.interfaces;

import java.util.HashMap;
import java.util.Iterator;

import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.Unit;
import soot.toolkits.graph.Block;

public interface BlockEdgeFrequencyPass {
	
	// public methods
	public boolean runOnFunction ();
	public double getEdgeFrequency (Block src, Block dst);
	public double getEdgeFrequency (Pair<Block, Block> edge);
	public double getBlockFrequency (Block basicBlock);
	public double getBackEdgeProbabilities (Pair<Block, Block> edge);
	public Iterator<Block> getBlockFreqIter ();
	public Iterator<Pair<Block, Block>> getEdgeFreqIter ();
	public LoopInfo getLoopInfo();
	public HashMap<Pair<Block, Block>, Double> getEdgeProbabilities ();
	public BranchPredictionPass getBPP ();

}
