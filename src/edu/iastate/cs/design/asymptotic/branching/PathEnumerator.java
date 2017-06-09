package edu.iastate.cs.design.asymptotic.branching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import edu.iastate.cs.design.asymptotic.datastructures.Path;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.UnitBox;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class PathEnumerator {

	public SootClass _class;
	HashMap<SootMethod, List<BlockPath>> _map;
	
	/**
	 * Construct a PathEnumerator using a given class.
	 * @param _class
	 */
	public PathEnumerator(SootClass _class){
		this._class = _class;
		_map = new HashMap<SootMethod, List<BlockPath>>();
	}
	
	/**
	 * Find all of the paths through the given class of this PathEnumerator
	 */
	public void findMethodPaths(){
		for(Iterator<SootMethod> methIter = _class.methodIterator(); methIter.hasNext();){
			List<BlockPath> methodPaths = new ArrayList<BlockPath>();
			SootMethod method = methIter.next();
			Stack<Block> blockSt = new Stack<>();
			Body b = null;
			if(method.hasActiveBody())
				b = method.getActiveBody();
			BlockGraph bg = new BriefBlockGraph(b);
			Block firstBlock = bg.getBlocks().get(0);
			blockSt.push(firstBlock);
			
			//Generates all of the possible paths through this method.
			while(!blockSt.isEmpty()){
				DFS(blockSt, methodPaths);
			}
			
			_map.put(method, methodPaths);
		}
	}
	
	/**
	 * Generate all paths through a method given a stack containing the first block of the method.
	 * @param blockStack A stack containing the first block of the method
	 * @param masterPaths A list of all of the paths through the method
	 */
	public void DFS(Stack<Block> blockStack, List<BlockPath> masterPaths){
		Block block = blockStack.peek();
		List<Block> succs = block.getSuccs();
		if(succs.isEmpty()){
			BlockPath pathToAdd = new BlockPath(blockStack);
			masterPaths.add(pathToAdd);
			blockStack.pop();
			return;
		}
		for(Block succ : block.getSuccs()){
			blockStack.push(succ);
			DFS(blockStack, masterPaths);
		}
	}
	
}
