package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Stack;
import java.util.Map.Entry;

import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;

public class CfgWalker {
	static final int LIMIT = 100;
	static HashMap<Block, Double> blockTimesMap;
	static HashMap<Pair<Block, Block>, Double> efreqMap1;
	static double time = 0.0;
	static double prob = 0.0;

	public static Collection<ExecutionPath> process(BriefBlockGraph cfg) {
		Collection<ExecutionPath> result = new ArrayList<ExecutionPath>();

		for (Block head : cfg.getHeads()) {
			Stack<Block> path = new Stack<Block>();

			Collection<ExecutionPath> allPaths = new ArrayList<ExecutionPath>();
			search(head, path, allPaths);

			result.addAll(allPaths);
		}
		return result;
	}

	private static void search(Block block, Stack<Block> path, Collection<ExecutionPath> allPaths) {
		if (allPaths.size() >= LIMIT)
			return;
		// Check we're not stuck in a cycle
		if (path.contains(block) && path.indexOf(block) != path.lastIndexOf(block)) {
			return;
		}

		// Add current block to path
		path.push(block);

		// Did we hit a tail of the graph?
		if (block.getSuccs().size() == 0) {
			ExecutionPath p = new ExecutionPath();
			p.blocks().addAll(Arrays.asList(path.toArray(new Block[1])));
			allPaths.add(p);
			path.pop(); // Required to get all paths correctly
			return;
		}

		// Loop over all children
		for (Block succs : block.getSuccs()) {
			search(succs, path, allPaths);
		}

		// No path found
		path.pop();
	}

	public static double process(BriefBlockGraph cfg, HashMap<Block, Double> blockTimes,
			HashMap<Pair<Block, Block>, Double> efreqMap) {
		blockTimesMap = blockTimes;
		efreqMap1 = efreqMap;
		time = 0.0;
		prob = 0.0;
		Collection<ExecutionPath> result = new ArrayList<ExecutionPath>();

		for (Block head : cfg.getHeads()) {
			Stack<Block> path = new Stack<Block>();

			Collection<ExecutionPath> allPaths = new ArrayList<ExecutionPath>();
			Stack<Pair<Double, Double>> pathValue = new Stack<Pair<Double, Double>>();

			double blockTime = getBlockTime(head);
			double edgeProb = 0.0;
			pathValue.push(new Pair<Double, Double>(blockTime, edgeProb));

			search1(head, path, pathValue, allPaths);

			result.addAll(allPaths);
		}
		return time / prob;
	}

	private static void search1(Block block, Stack<Block> path, Stack<Pair<Double, Double>> pathValue,
			Collection<ExecutionPath> allPaths) {
		if (allPaths.size() >= LIMIT)
			return;
		// Check we're not stuck in a cycle
		if (path.contains(block) && path.indexOf(block) != path.lastIndexOf(block)) {
			return;
		}

		// Add current block to path
		path.push(block);

		// Did we hit a tail of the graph?
		if (block.getSuccs().size() == 0) {
			ExecutionPath p = new ExecutionPath();
			p.blocks().addAll(Arrays.asList(path.toArray(new Block[1])));
			allPaths.add(p);
			path.pop(); // Required to get all paths correctly
			Pair<Double, Double> top = pathValue.pop();
			double second = top.second().doubleValue();
			if (second == 0.0)
				second = 1.0;
			time += top.first().doubleValue() * second;
			prob += second;
			return;
		}

		// Loop over all children
		for (Block succs : block.getSuccs()) {
			Pair<Double, Double> top = pathValue.peek();
			double dst = getBlockTime(succs) + top.first().doubleValue();
			Pair<Block, Block> edge = new Pair<Block, Block>(block, succs);
			double edgeProb = getEdgeFrequency(edge) + top.second().doubleValue();
			pathValue.push(new Pair<Double, Double>(dst, edgeProb));
			search1(succs, path, pathValue, allPaths);
		}

		// No path found
		path.pop();
		pathValue.pop();
	}

	static double getEdgeFrequency(Pair<Block, Block> edge) {
		for (Entry<Pair<Block, Block>, Double> entry : efreqMap1.entrySet()) {
			if (entry.getKey().toString().equals(edge.toString()))
				return entry.getValue();
		}
		return 1.0;
	}

	static double getBlockTime(Block block) {
		for (Entry<Block, Double> entry : blockTimesMap.entrySet()) {
			if (entry.getKey().toString().equals(block.toString()))
				return entry.getValue().doubleValue();
		}
		return 0.0;
	}
}
