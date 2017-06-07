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
		while(!st.isEmpty()){
			this.addToEnd(st.pop());
		}
	}
	
	public BlockPath(){
		root = new Node(null);
		tail = new Node(null);
		root.next = tail;
		tail.prev = root;
	}
	
	public void addToEnd(Block b){
		Node toAdd = new Node(b);
		toAdd.next = tail;
		toAdd.prev = tail.prev;
		tail.prev = toAdd;
		toAdd.prev.next = toAdd;
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
	
	
}
