package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import soot.jimple.toolkits.pointer.nativemethods.NativeMethodNotSupportedException;

public class Path<E> implements List<E>, Serializable {
	
	
	private class Node implements Serializable{
		
		private E body;
		
		private Node next;
		
		private Node prev;
		
		private Node(E body){
			this();
			this.body = body;
		}
		
		private Node() {
			body = null;
			prev = null;
			next = null;
		}
		
	}
	
	private Node tail;
	
	private Node root;
	
	private int size;
	
	public Path(Stack<E> st){
		this();
		Stack<E> stack = new Stack<E>();
		stack = (Stack<E>) st.clone();
		while(!stack.isEmpty()){
			this.addToStart(stack.pop());
		}
	}
	
	public Path(List<E> li){
		this();
		for(E e: li){
			this.addToEnd(e);
		}
	}
	
	public Path(){
		root = new Node(null);
		tail = new Node(null);
		root.next = tail;
		tail.prev = root;
	}
	
	public void addToStart(E e){
		addBefore(e, root.next);
	}
	
	public boolean endsWith(E e){
		return tail.prev.body.equals(e);
	}
	
	public E getLast(){
		return tail.prev.body;
	}
	
	public boolean insertAfter(E addAfterThis, Path<E> toAdd){
		boolean found = false;
		Node curr = root.next;
		while(curr.next != null){
			if(curr.body.equals(addAfterThis)){
				found = true;
				break;
			} else {
				curr = curr.next;
			}
		}
		if(found){
			List<E> elementsToAdd = toAdd.getElements();
			Node afterCurr = curr.next;
			for(E eToAdd : elementsToAdd){
				addBefore(eToAdd, afterCurr);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean insertBefore(E addBeforeThis, Path<E> toAdd){
		boolean found = false;
		Node curr = root.next;
		while(curr.next != null){
			if(curr.body.equals(addBeforeThis)){
				found = true;
				break;
			} else {
				curr = curr.next;
			}
		}
		if(found){
			List<E> elementsToAdd = toAdd.getElements();
			for(E eToAdd : elementsToAdd){
				addBefore(eToAdd, curr);
			}
			return true;
		} else {
			return false;
		}
	}
	
	private void addBefore(E e, Node n){
		Node toAdd = new Node(e);
		toAdd.next = n;
		toAdd.prev = n.prev;
		n.prev = toAdd;
		toAdd.prev.next = toAdd;
		size++;
	}
	
	public void addToEnd(E e){
		addBefore(e, tail);
	}
	
	public List<E> getElements(){
		List<E> result = new ArrayList<E>();
		Node curr = root.next;
		while(curr.next != null){
			result.add(curr.body);
			curr = curr.next;
		}
		return result;
	}
	
	@Override
	public String toString(){
		String result = "Start -> ";
		Node curr = root.next;
		while(curr.next != null){
			result += curr.body.toString() + " -> ";
			curr = curr.next;
		}
		return result += " End";
	}
	
	public Path<E> copy(){
		Path<E> that = new Path<E>();
		Node curr = root.next;
		while(curr.next != null){
			that.addToEnd(curr.body);
			curr = curr.next;
		}
		return that;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return root.next.equals(tail);
	}

	@Override
	public boolean contains(Object o) {
		for(E e: getElements()){
			if(e.equals(o)){
				return true;
			}
		}
		return false;
	}
	
	private class PathIterator implements Iterator {
		
		Node curr = root;

		@Override
		public boolean hasNext() {
			return !curr.next.equals(tail);
		}

		@Override
		public Object next() {
			if(this.hasNext()){
				curr = curr.next;
				return curr;
			}
			throw new NoSuchElementException();
		}
		
	}

	@Override
	public Iterator iterator() {
		return new PathIterator();
	}

	@Override
	public Object[] toArray() {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public Object[] toArray(Object[] a) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public boolean add(Object o) {
		addToEnd((E) o);
		return true;
	}

	public void remove(Node n){
		n.next.prev = n.prev;
		n.prev.next = n.next;
	}
	
	@Override
	public boolean remove(Object o) {
		Node curr = root.next;
		while(curr.next != null){
			if(curr.body.equals(o)){
				remove(curr);
				return true;
			}
			curr = curr.next;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection c) {
		for(Object o: c){
			if(!contains(o))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection c) {
		for(Object o : c){
			add(o);
		}
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public void clear() {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public E get(int index) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public E set(int index, E element) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public void add(int index, E element) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public E remove(int index) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public int indexOf(Object o) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new NativeMethodNotSupportedException();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new NativeMethodNotSupportedException();
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null || !o.getClass().equals(this.getClass())){
			return false;
		}
		Path<E> that = (Path<E>) o;
		if(this.size != that.size)
			return false;
		if(this.size == 0)
			return true;
		Node thisNode = this.root.next;
		Node thatNode = that.root.next;
		while(thisNode.body != null){
			if(!thisNode.body.equals(thatNode.body)){
				return false;
			}
			thisNode = thisNode.next;
			thatNode = thatNode.next;
		}
		return true;
	}

}