package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.util.List;

import soot.SootMethod;
import soot.Unit;
import weka.core.Instance;

public class ListWrapper {

	private List<Unit> list;
	
	private boolean hot;
	
	private FeatureStatistic statistics;
	
	private SootMethod original_meth;
	
	private int count;
	
	private Instance instance;
	
	public ListWrapper(){
		hot = false;
		list = null;
		statistics = null;
		original_meth = null;
		count = 0;
		instance = null;
	}
	
	public ListWrapper(List<Unit> list, boolean hot, FeatureStatistic statistics , SootMethod original_meth, int count, Instance instance){
		this.list = list;
		this.hot = hot;
		this.statistics = statistics;
		this.original_meth = original_meth;
		this.count = count;
		this.instance = instance;
	}
	
	public void setInstance(Instance instance){
		this.instance = instance;
	}
	
	public Instance getInstance(){
		return instance;
	}
	
	public void incrementCount(){
		count++;
	}
	
	public int getCount(){
		return count;
	}
	
	public void setCount(int count){
		this.count = count;
	}
	
	public SootMethod getMeth(){
		return original_meth;
	}
	
	public void setMeth(SootMethod original_meth){
		this.original_meth = original_meth;
	}
	
	public void setHot(boolean hot){
		this.hot = hot;
	}
	
	public boolean getHot(){
		return hot;
	}
	
	public List<Unit> getList(){
		return list;
	}
	
	public void setList(List<Unit> list){
		this.list = list;
	}
	
	public void setFS(FeatureStatistic statistics){
		this.statistics = statistics;
	}
	
	public FeatureStatistic getFS(){
		return statistics;
	}
	
}
