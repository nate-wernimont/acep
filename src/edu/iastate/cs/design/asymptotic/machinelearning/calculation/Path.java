package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import soot.Unit;

public class Path implements Iterable, Serializable {
	
	private ArrayList<Object> elemList;
	
	public Path(Path copy){
		init();
		for(Object o : copy.elemList){
			this.add(o);
		}
	}
	
	public Path(){
		init();
	}
	
	private void init(){
		elemList = new ArrayList<>();
	}
	
	public int size(){
		return elemList.size();
	}
	
	public void add(Object o){
		elemList.add(o);
	}
	
	public boolean addAfter(Unit where, List<Path> toInsert){
		int index = elemList.indexOf(where);
		if(index >= 0){
			elemList.add(index+1, toInsert);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean addBefore(Unit where, List<Path> toInsert){
		int index = elemList.indexOf(where);
		if(index >= 0){
			elemList.add(index, toInsert);
			return true;
		} else {
			return false;
		}
	}
	
	public Unit getFirst(){
		return (Unit) elemList.get(0);
	}
	
	public ArrayList<ArrayList<Unit>> getAllPaths(ArrayList<Path> dontSearch){
		ArrayList<ArrayList<Unit>> progress = new ArrayList<>();
		progress.add(new ArrayList<>());
		
		if(dontSearch == null){
			dontSearch = new ArrayList<>();
			dontSearch.add(this);
		} else if(!dontSearch.contains(this)){
			dontSearch.add(this);
		}
		
		for(int i = 0; i < elemList.size(); i++){
			Object next = elemList.get(i);
			if(next instanceof Unit){
				progress.forEach((v) -> {
					v.add((Unit) next);
				});
			} else {
				int originalSize = progress.size();
				List<Path> castedNext;
				if(((List<Path>) next).size() > 2){
					castedNext = ((List<Path>) next).subList(0, 2);
				} else {
					castedNext = (List<Path>) next;
				}
				for(Path path : castedNext){
					if(dontSearch.contains(path))
						continue;
					for(ArrayList<Unit> newPath : path.getAllPaths(dontSearch)){
						for(int j = 0; j < originalSize; j++){
							ArrayList<Unit> toAdd = new ArrayList<>(progress.get(j));
							toAdd.addAll(newPath);
							progress.add(toAdd);
						}
					}
				}
				System.out.println("Path count: "+progress.size());
			}
		}
		
		return progress;
	}
	
	private class PathIterator implements Iterator<Object> {
		
		int cursor;
		
		public PathIterator(){
			cursor = 0;
		}

		@Override
		public boolean hasNext() {
			if(cursor >= elemList.size())
				return false;
			return true;
		}

		@Override
		public Object next() {
			if(hasNext())
				return elemList.get(cursor++);
			else
				return null;
		}
		
	}

	@Override
	public Iterator iterator() {
		return new PathIterator();
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null || !o.getClass().equals(this.getClass()))
			return false;
		Path p = (Path) o;
		if(p.elemList.size() != this.elemList.size())
			return false;
		for(int i = 0; i < this.elemList.size(); i++){
			if(!this.elemList.get(i).equals(p.elemList.get(i)))
				return false;
		}
		return true;
	}

}