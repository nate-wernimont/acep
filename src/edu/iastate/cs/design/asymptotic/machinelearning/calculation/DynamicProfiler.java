package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.management.timer.Timer;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.options.Options;

public class DynamicProfiler {

	SootClass _class;
	
	public DynamicProfiler(SootClass _class){
		this._class = _class;
	}
	
	public void addTransformer(int output_format){
		PackManager.v().getPack("jtp").add(new Transform("jtp.statementLogger", new Instrumenter()));
		Options.v().set_output_format(output_format);
		File dir = new File("./profilingOutput/");
		dir.mkdirs();
		for(File f : dir.listFiles()){
			f.delete();
		}
		soot.Main.main(new String[]{_class.getName()});
	}
	
	public void runNewClass(){//TODO: try to get running
		Options.v().set_soot_classpath(Options.v().output_dir()+":"+Options.v().soot_classpath());
		try {
			Runtime.getRuntime().exec("./sootOutput/"+_class.getName().replace('.', '/')+".class");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void analyzeFile(File file){
		String line;
		
		HashMap<Path<Unit>, Integer> pathCounts = new HashMap<>();
		Path<Unit> currPath = new Path<>();
		Stack<Pair<Path<Unit>, Integer>> prevPaths = new Stack<>();
		Stack<SootMethod> currCallStack = new Stack<>();
		HashMap<Path<Unit>, List<Path<Unit>>> backEdges = new HashMap<>();
		int repCount = 0;
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file));){
			line = reader.readLine();
			currCallStack.push(Scene.v().getMethod(line.split(PrintInfo.DIVIDER)[0]));//The first method
			int count = 0;
			long startTime = System.currentTimeMillis();
			int unitDeletion = 0;
			Unit lastUnit = null;
			findAllUnits: 
				do {
					count++;
					if(count % 100000 == 0)
						System.out.println(count+": "+": "+pathCounts.size()+": "+pathSize(pathCounts)+": "+traversedPaths(pathCounts)+": "+(count-unitDeletion)+": "+prevPaths.size()+": "+(System.currentTimeMillis()-startTime)/1000);
					
					
					String[] info = line.split(PrintInfo.DIVIDER);
					SootMethod currMeth = Scene.v().getMethod(info[0]);
					for(Unit unit : currMeth.retrieveActiveBody().getUnits()){
						if(unit.toString().equals(info[1])){
							
							if(!currPath.contains(unit)){//First, add the unit on
								List<Path<Unit>> lastBackEdges = backEdges.get(currPath);
								backEdges.remove(currPath);
								currPath.add(unit);
								backEdges.put(currPath, lastBackEdges);
							} else {
								unitDeletion++;
								if(backEdges.get(currPath) == null){
									backEdges.put(currPath, new ArrayList<Path<Unit>>());
								}
								boolean found = false;
								for(Path<Unit> backPath : backEdges.get(currPath)){
									if(backPath.contains(unit)){
										if(backPath.getLast().equals(unit)){
											repCount++;
											found = true;
										}
										break;
									} else if(backPath.contains(lastUnit)){
										backPath.add(unit);
										found = true;
										break;
									}
								}
								if(!found){
									Path<Unit> toAdd = new Path<>();
									toAdd.add(unit);
									backEdges.get(currPath).add(toAdd);
								}
							}
							
							if(unit instanceof JRetStmt || unit instanceof JReturnStmt || unit instanceof JReturnVoidStmt){
								SootMethod lastMethod = currCallStack.pop();
								if(!currCallStack.peek().getDeclaringClass().equals(lastMethod.getDeclaringClass())){
									int backEdgeCount = 0;
									if(backEdges.get(currPath) != null){
										backEdgeCount = backEdges.get(currPath).size();
									}
									if(pathCounts.get(currPath) == null){
										pathCounts.put(currPath, new Integer(repCount+1+backEdgeCount));
										System.out.println("Found new path: "+repCount);
									} else {
										System.out.println("Found a path, adding to it's count");
										pathCounts.put(currPath, new Integer(pathCounts.get(currPath).intValue()+1+repCount+backEdgeCount));
									}
									Pair<Path<Unit>, Integer> nextPath = prevPaths.pop();
									System.out.println(":"+repCount);
									repCount = nextPath.second().intValue();
									System.out.println(repCount);
									currPath = nextPath.first();
								}
							} else if(unit instanceof JInvokeStmt){
								SootMethod methodCalled = ((JInvokeStmt) unit).getInvokeExpr().getMethod();
								if(!methodCalled.getDeclaringClass().equals(currCallStack.peek().getDeclaringClass())){
									prevPaths.push(new Pair<>(currPath, new Integer(repCount)));
									repCount = 0;
									currPath = new Path<>();
								}
								currCallStack.push(methodCalled);

							} else if(unit instanceof JAssignStmt){
								for(ValueBox vb : unit.getUseBoxes()){
									if(vb.getClass().getSimpleName().equals("LinkedRValueBox")){
										if(vb.getValue() instanceof InvokeExpr){
											SootMethod methodCalled = ((InvokeExpr) vb.getValue()).getMethod();
											if(!methodCalled.getDeclaringClass().equals(currCallStack.peek().getDeclaringClass())){
												prevPaths.push(new Pair<>(currPath, new Integer(repCount)));
												repCount = 0;
												currPath = new Path<>();
											}
											currCallStack.push(methodCalled);
										}
									}
								}
							}
							lastUnit = unit;
							continue findAllUnits;
						}
					}
					throw new Error("No unit matches the string: "+info[1]);
				} while((line=reader.readLine()) != null);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error while reading from the file");
		}
		
		System.out.println("Finished reading from the file");

		for(Path<Unit> path : pathCounts.keySet()){
			System.out.println(path);
			System.out.println(pathCounts.get(path));
		}
	}
	
	private int pathSize(HashMap<Path<Unit>, Integer> pathCounts){
		int total = 0;
		for(Path<Unit> p : pathCounts.keySet()){
			total += p.size();
		}
		return total;
	}
	
	private int traversedPaths(HashMap<Path<Unit>, Integer> pathCounts){
		int total = 0;
		for(Path<Unit> p : pathCounts.keySet()){
			total += pathCounts.get(p).intValue();
		}
		return total;
	}
	
}
