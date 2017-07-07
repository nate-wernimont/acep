package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.Iterator;

import soot.SootMethod;
import soot.toolkits.graph.Block;

public class Method implements Iterable<Block>{

	private SootMethod _method;
	private String _methodString;
	/*private Body _b;
	private CompleteBlockGraph _cgraph;
	private List<Block> _blocks;
	private List<Loop> _loops;*/
	private double cycles = 0.0;
	
	/*public Method (SootMethod method) {
		_method = method;
		_b = _method.retrieveActiveBody();
		_cgraph = new CompleteBlockGraph(_b);
		_blocks = _cgraph.getBlocks();
	}*/
	
	public Method (SootMethod method, String methodString, double time) {
		_method = method;
		_methodString = methodString;
		cycles = time;
	}
	
	@Override
	public Iterator<Block> iterator() {
		return  null;//_blocks.iterator();
	}
	
	public SootMethod sootMethod () {
		return _method;
	}
	
	public double time () {
		return cycles;
	}
	
	static public String getMethodString (SootMethod method) {
		String methodString;
		methodString = method.getDeclaration();
		String className = method.getDeclaringClass().toString();
		String parts[] = methodString.split(" ");
		methodString = "";
		for (String string : parts) {
			if (string.contains("(")) {
				String subString = string;
				if (string.contains(")"))
						subString = string.substring(0, string.indexOf(')')+1);
				String temp = className + "." + subString;
				methodString += temp;
				if (string.contains(")"))
					return methodString;
			} else {
				methodString += string + " ";
			}
		}
		return methodString;
	}
	
	static public String getMethodString (SootMethod method, String forClassName) {
		String methodString;
		methodString = method.getDeclaration();
		//String className = method.getDeclaringClass().toString();
		String parts[] = methodString.split(" ");
		methodString = "";
		for (String string : parts) {
			if (string.contains("(")) {
				String subString = string;
				if (string.contains(")"))
						subString = string.substring(0, string.indexOf(')')+1);
				String temp = forClassName + "." + subString;
				methodString += temp;
				if (string.contains(")"))
					return methodString;
			} else if (string.equals("abstract")) {
				continue;
			} else {
				methodString += string + " ";
			}
		}
		return methodString;
	}
	
	
	/*static public String getMethodWithOnlyNameAndParams (SootMethod method) {
		String methodString;
		methodString = method.getDeclaration();
		String className = method.getDeclaringClass().toString();
		String parts[] = methodString.split(" ");
		methodString = "";
		for (String string : parts) {
			if (string.contains("(")) {
				String temp = className + "." + string;
				methodString += temp;
			}
		}
		return methodString;
	}*/
	
	
}
