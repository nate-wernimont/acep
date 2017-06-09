package edu.iastate.cs.design.asymptotic.branching;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import soot.toolkits.graph.Block;

public class BlockPath {
	
	
	private class Node{
		
		private Block body;
		
		private Node next;
		
		private Node prev;
		
		private Node(Block body){
			this.body = body;
			prev = null;
			next = null;
		}
		
	}
	
	Node tail;
	
	Node root;
	
	public BlockPath(Stack<Block> st){
		this();
		Stack<Block> stack = new Stack<Block>();
		stack = (Stack<Block>) st.clone();
		while(!stack.isEmpty()){
			this.addToStart(stack.pop());
		}
	}
	
	public BlockPath(List<Block> li){
		this();
		for(Block b: li){
			this.addToEnd(b);
		}
	}
	
	public BlockPath(){
		root = new Node(null);
		tail = new Node(null);
		root.next = tail;
		tail.prev = root;
	}
	
	public void addToStart(Block b){
		addBefore(b, root.next);
	}
	
	private void addBefore(Block b, Node n){
		Node toAdd = new Node(b);
		toAdd.next = n;
		toAdd.prev = n.prev;
		n.prev = toAdd;
		toAdd.prev.next = toAdd;
	}
	
	public void addToEnd(Block b){
		addBefore(b, tail);
	}
	
	public List<Block> toList(){
		List<Block> result = new ArrayList<Block>();
		Node curr = root.next;
		while(curr.next != null){
			result.add(curr.body);
			curr = curr.next;
		}
		return result;
	}
	
	public String toString(){
		String result = "Start ->";
		Node curr = root.next;
		while(curr.next != null){
			result += curr.body.toShortString() + " ->";
			curr = curr.next;
		}
		return result += " End";
	}
	
	
}
