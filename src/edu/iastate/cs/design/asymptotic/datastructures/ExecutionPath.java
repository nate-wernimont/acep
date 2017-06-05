package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import soot.toolkits.graph.Block;

public class ExecutionPath {

	private Collection<Block> blocks;

	private static final String HOT = "hot";
	private static final String COLD = "cold";

	public ExecutionPath() {
		blocks = new ArrayList<Block>();
	}

	public Collection<Block> blocks() {
		return blocks;
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		for (Block b : blocks) {
			str.append(b.getIndexInMethod());
			str.append(", ");
		}
		return str.replace(str.lastIndexOf(","), str.lastIndexOf(" "), "")
				.toString();
	}

}
