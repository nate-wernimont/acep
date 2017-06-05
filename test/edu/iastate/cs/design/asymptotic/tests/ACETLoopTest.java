package edu.iastate.cs.design.asymptotic.tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

import edu.iastate.cs.design.asymptotic.datastructures.Interpreter;
import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.datastructures.Path;
import edu.iastate.cs.design.asymptotic.datastructures.UnitInfo;
import edu.iastate.cs.design.asymptotic.interfaces.impl.BlockEdgeFrequencyPassImpl;
import edu.iastate.cs.design.asymptotic.interfaces.impl.StaticProfilePassImpl;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.baf.BafBody;
import soot.baf.internal.BInterfaceInvokeInst;
import soot.baf.internal.BSpecialInvokeInst;
import soot.baf.internal.BStaticInvokeInst;
import soot.baf.internal.BVirtualInvokeInst;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;

public class ACETLoopTest {

	static StaticProfilePassImpl profile;
	static Interpreter interpreter;

	public static void main(String[] args) {
		profile = new StaticProfilePassImpl("loop");
		interpreter = new Interpreter();
		SootClass sClass = Scene.v().getApplicationClasses().getFirst();
		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod method = (SootMethod) methodIt.next();
			BlockEdgeFrequencyPassImpl befp = new BlockEdgeFrequencyPassImpl(
					method);
			if (!method.getName().equals("main"))
				continue;

			// Get block frequency map
			HashMap<Block, Double> blockFrequencies = profile
					.getBlockFrequencyMap(method);

			HashMap<Block, Double> blockTimes = new HashMap<Block, Double>();

			// Baf (bytecode) graph construction
			BafBody body = new BafBody(method.retrieveActiveBody(), (Map) null);
			BriefBlockGraph c = new BriefBlockGraph(body);

			// Block (source code) graph construction
			JimpleBody b = (JimpleBody) method.retrieveActiveBody();
			BriefBlockGraph cGraph = new BriefBlockGraph(b);

			Iterator<Block> bafBlockIter = c.iterator();
			Iterator<Block> srcBlockIter = cGraph.iterator();

			while (bafBlockIter.hasNext() && srcBlockIter.hasNext()) {
				Block bafBlock = bafBlockIter.next();
				Block srcBlock = srcBlockIter.next();

				double blockCyles = 0;
				double blockTime = 0.0;

				// Get block frequency from static profile
				double blockFrequency = 1.0;

				blockFrequency = blockFrequency (befp, srcBlock);//befp.getBlockFrequency(srcBlock);// getBlockFrequency(blockFrequencies,
																	// srcBlock);

				for (Iterator<Unit> unitIter = bafBlock.iterator(); unitIter
						.hasNext();) {
					Unit unit = unitIter.next();
					// Get the information about unit
					UnitInfo info = new UnitInfo(unit);
					double cycles = interpreter.process_unit(info);
					blockCyles += cycles;

				} // end of block units iterator

				blockTime = blockFrequency * blockCyles;
				blockTimes.put(srcBlock, new Double(blockTime));
			} // end of blocks iterator

			double acet = compute(befp, method, blockTimes);

			System.out.println("ACET = " + acet);
		}
	}
	
	static double blockFrequency (BlockEdgeFrequencyPassImpl BEFP, Block iblock) {
		HashMap<Block, Double> blockFreq = BEFP.blockFrequencies();
		for (Entry<Block, Double> entry : blockFreq.entrySet()) {
			Block block = entry.getKey();
			if (block.toString().equals(iblock.toString()))
				return entry.getValue();
		}
		return 0.0;
	}

	static double compute(BlockEdgeFrequencyPassImpl BEFP, SootMethod method,
			HashMap<Block, Double> blockTimes) {
		HashMap<Pair<Block, Block>, Double> efreqMap = profile
				.getEdgeFrequencyMap(method);
		double T0 = 0, T1 = 0; // Times for non-loop and loop blocks
		double p0 = 1, p1 = 1; // probabilities for non-loop and loop edges
		LoopInfo li = new LoopInfo(method);

		// Construct loop blocks values
		Loop loop = li.iterator().next();
		// Iterator<Block> loopBlocksIter = loop.loopIterator();
		Vector<Block> visited = new Vector<Block>();
		double probability = 1.0;
		double succProbability = 0.0;
		for (Block block : loop.getLoopBlocks()) {
			T1 += getBlockTime(blockTimes, block);
			succProbability = 0.0;
			if (visited.contains(block))
				continue;
			List<Block> succs = block.getSuccs();
			for (Block succBlock : succs) {
				if (!loop.isInLoop(succBlock))
					continue;
				Pair<Block, Block> edge = new Pair<Block, Block>(block,
						succBlock);
				succProbability += BEFP.getBackEdgeProbabilities(edge);
			}
			p1 *= succProbability;
			visited.add(block);
		}

		// Construct non-loop values
		// Block (source code) graph construction
		JimpleBody b = (JimpleBody) method.retrieveActiveBody();
		BriefBlockGraph cGraph = new BriefBlockGraph(b);

		Iterator<Block> blockIter = cGraph.iterator();
		while (blockIter.hasNext()) {
			Block block = blockIter.next();
			if (loop.isInLoop(block))
				continue;
			T0 += getBlockTime(blockTimes, block);
			succProbability = 0.0;
			List<Block> succs = block.getSuccs();
			for (Block succBlock : succs) {
				Pair<Block, Block> edge = new Pair<Block, Block>(block,
						succBlock);
				succProbability += BEFP.getBackEdgeProbabilities(edge);
			}
			// Probably some predecessors
			List<Block> preds = block.getPreds();
			for (Block predBlock : preds) {
				Pair<Block, Block> edge = new Pair<Block, Block>(predBlock,
						block);
				succProbability += BEFP.getBackEdgeProbabilities(edge);
			}
			
			p0 *= succProbability;
		}

		int loopIterations = 100000; // change this

		double value1 = 0.0, value2 = 0.0;
		for (int i = 1; i <= loopIterations; i++) {
			value1 += Math.pow(p1, i);
		}

		for (int i = 1; i <= loopIterations; i++) {
			value2 += i * Math.pow(p1, i);
		}
		// ACET computation
		double acet = p0 * (T0 * value1 + T1 * value2);

		return acet;
	}

	static double getBlockTime(HashMap<Block, Double> blockTimes, Block block) {
		for (Entry<Block, Double> entry : blockTimes.entrySet()) {
			if (entry.getKey().toString().equals(block.toString()))
				return entry.getValue().doubleValue();
		}
		return 0.0;
	}

	static double getEdgeFrequency(
			HashMap<Pair<Block, Block>, Double> edgeFrequencies,
			Pair<Block, Block> edge) {
		for (Entry<Pair<Block, Block>, Double> entry : edgeFrequencies
				.entrySet()) {
			if (entry.getKey().toString().equals(edge.toString()))
				return entry.getValue();
		}
		return 1.0;
	}

	static double getBlockFrequency(HashMap<Block, Double> bFreq, Block block) {
		for (Entry<Block, Double> entry : bFreq.entrySet()) {
			if (entry.getKey().toString().equals(block.toString()))
				return entry.getValue().doubleValue();
		}
		return 1.0;
	}

}
