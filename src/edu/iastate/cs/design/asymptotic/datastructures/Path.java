package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.SootMethod;
import soot.toolkits.graph.Block;

public class Path {

	private Block _start;
	private Block _end;
	List<Block> _nodes;
	List<Pair<Block, Block>> edges;
	
	// Method may be needed later
	private SootMethod _method;
	
	public Path () {
		_nodes = new ArrayList<Block>();
		edges = new ArrayList<Pair<Block,Block>>();
	}
	
	public void copy (Path path) {
		for (Block block : path.get_nodes()) {
			_nodes.add(block);
		}
		for (Pair<Block, Block> edge : path.edges) {
			edges.add(edge);
		}
		//this.set_nodes(path.get_nodes());
	}
	
	public boolean contains (Block block) {
		for (Block blk : _nodes) {
			if (blk.toString().equals(block.toString()))
				return true;
		}
		return false;
	}

	public Block get_start() {
		return _start;
	}

	public void set_start(Block _start) {
		this._start = _start;
	}

	public Block get_end() {
		return _end;
	}

	public void set_end(Block _end) {
		this._end = _end;
	}

	public List<Block> get_nodes() {
		return _nodes;
	}

	public void set_nodes(List<Block> _nodes) {
		this._nodes = _nodes;
	}
	
	public void addToPath (Block block) {
		_nodes.add(block);
	}
	
	public Iterator<Block> iterator () {
		return _nodes.iterator();
	}
	
	public void addEdge (Block src, Block dst) {
		edges.add(new Pair<Block, Block>(src, dst));
	}
	
	public boolean contains (Block src, Block dst) {
		for (Pair<Block, Block> edge : edges) {
			Pair<Block, Block> compareEdge = new Pair<Block, Block>(src, dst);
			if (compareEdge.toString().equals(edge.toString()))
				return true;
		}
		return false;
	}
	
	public String toString () {
		String returnString = "";
		Iterator<Block> blockIter = iterator();
		while (blockIter.hasNext()) {
			Block block = blockIter.next();
			returnString += block.toShortString();
			if (blockIter.hasNext())
				returnString += "------->";
		}
		return returnString;
	}
	
}
