package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

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

/**
 * 
 * @author Nate Wernimont
 * Instruments a class and converts execution data into paths
 *
 */
public class DynamicProfiler {

	/**
	 * The main application class
	 */
	SootClass _class;
	
	/**
	 * Where to find the files
	 */
	private static String FILE_LOCATION = "";
	
	/**
	 * Initializes an instance of the profiler
	 * @param _class
	 */
	public DynamicProfiler(SootClass _class){
		this._class = _class;
	}
	
	/**
	 * Instruments the class of this profiler
	 * @param output_format The output format of the instrumented code
	 */
	public void addTransformer(int output_format){
		PackManager.v().getPack("jtp").add(new Transform("jtp.statementLogger", new Instrumenter()));
		Options.v().set_output_format(output_format);
//		File dir = new File("./profilingOutput/");
//		dir.mkdirs();
//		for(File f : dir.listFiles()){
//			f.delete();
//		}
		soot.Main.main(new String[]{_class.getName()});
	}
	
	/**
	 * Run the instrumented class
	 */
	public void runNewClass(){//TODO: try to get running
		Options.v().set_soot_classpath(Options.v().output_dir()+":"+Options.v().soot_classpath());
		try {
			Runtime.getRuntime().exec("./sootOutput/"+_class.getName().replace('.', '/')+".class");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Analyze the files that were generated from this profilers instrumented application class
	 */
	public void analyzeFiles(){
		FILE_LOCATION=PrintInfo.FILE_LOCATION;
		String line;
		
		ArrayList<Pair<Path<Unit>, Integer>> pathCounts = new ArrayList<>();
		Path<Unit> currPath = new Path<>();
		Stack<List<Object>> prevPaths = new Stack<>();
		Stack<SootMethod> callStack = new Stack<>();
		Set<Path<Unit>> backEdges = new HashSet<>();
		Path<Unit> currLoopSegment = new Path<>();
		int repCount = 0;
		Unit lastUnit = null;
		
		int count = 0;
		long startTime = System.currentTimeMillis();
		int unitDeletion = 0;
		
		File toRead = new File(PrintInfo.FILE_LOCATION+_class.getShortName()+"1.txt");
		int fileNumber = 1;
		
		while(toRead.exists()){
			try (BufferedReader reader = new BufferedReader(new FileReader(toRead))){
				line = reader.readLine();
				if(fileNumber == 1)
					callStack.push(Scene.v().getMethod(line.split(PrintInfo.DIVIDER)[0]));//push the first method on (We call main)
				while(line != null){
					boolean found = false;
					String methodSignature = line.split(PrintInfo.DIVIDER)[0];
					String unitString = line.split(PrintInfo.DIVIDER)[1];
					SootMethod meth = Scene.v().getMethod(methodSignature);
					for(Unit unit : meth.retrieveActiveBody().getUnits()){
						if(unit.toString().equals(unitString)){
							found = true;
							
							count++;
							if(count % 1000000 == 0){
									System.out.println("["+_class.getShortName()+"] Number of statements processed: "+count+
											", \n\tAmount of back paths: "+backEdges.size()+
											", \n\tCurrent repCount: "+repCount+
											", \n\tNumber of found paths: "+pathCounts.size()+
											", \n\tThe total length of those paths: "+pathSize(pathCounts)+
											", \n\tNumber of times those paths have been traversed: "+traversedPaths(pathCounts)+
											", \n\tThe amount of units used:"+(count-unitDeletion)+
											", \n\tThe amount of paths in the stack:"+prevPaths.size()+
											", \n\tTime elapsed:"+(System.currentTimeMillis()-startTime)/1000);
							}
							if(!meth.equals(callStack.peek())){
								if(lastUnit instanceof JReturnStmt || lastUnit instanceof JReturnVoidStmt || lastUnit instanceof JRetStmt){
									SootMethod method_ended = callStack.pop();
									if(!meth.equals(callStack.peek())){
										throw new Error("Returning to an unknown method: "+meth+", "+callStack.peek());
									}
									if(!method_ended.getDeclaringClass().equals(meth.getDeclaringClass())){
										//Path ends, go back to last path we were making
										boolean inMap = false;
										int location;
										for(location = 0; location < pathCounts.size(); location++){
											if(pathCounts.get(location).first().equals(currPath)){
												inMap = true;
												break;
											}
										}
										if(inMap){
											//System.out.println("["+originalName+"] Went along a previous path");
											pathCounts.get(location).setSecond(new Integer(pathCounts.get(location).second()+1+repCount+backEdges.size()));
										} else {
											//System.out.println("["+originalName+"] Found a new path");
											pathCounts.add(new Pair<>(currPath, new Integer(1+repCount+backEdges.size())));
										}
										List<Object> next = prevPaths.pop();
										currPath = (Path<Unit>) next.get(0);
										repCount = (int) next.get(1);
										backEdges = (Set<Path<Unit>>) next.get(2);
										currLoopSegment = (Path<Unit>) next.get(3);
									}
								} else if(!meth.getDeclaringClass().equals(callStack.peek().getDeclaringClass())){
									if(meth.isStatic() && meth.isEntryMethod() && meth.getName().equals("<clinit>") && getMethodCalled(lastUnit) != null){
										List<Object> prevInfo2 = new ArrayList<>();
										prevInfo2.add(currPath);
										prevInfo2.add(repCount);
										prevInfo2.add(backEdges);
										prevInfo2.add(currLoopSegment);
										prevPaths.push(prevInfo2);
										currPath = new Path<>();
										repCount = 0;
										backEdges = new HashSet<>();
										currLoopSegment = new Path<>();
										callStack.push(getMethodCalled(lastUnit));//static initializer, which is IMPLICIT
									}
									//Had to be an invoke, and it is to a different class
									List<Object> prevInfo = new ArrayList<>();
									prevInfo.add(currPath);
									prevInfo.add(repCount);
									prevInfo.add(backEdges);
									prevInfo.add(currLoopSegment);
									prevPaths.push(prevInfo);
									currPath = new Path<>();
									repCount = 0;
									backEdges = new HashSet<>();
									currLoopSegment = new Path<>();
									callStack.push(meth);
									
								} else {
									callStack.push(meth);
								}
							}
							//What is the point? All we are doing is modifying the call stack for what will eventually be returned with no added benefits.
//							} else {//we are in the same method. Check for recursion
//								if(lastUnit instanceof JReturnStmt || lastUnit instanceof JReturnVoidStmt || lastUnit instanceof JRetStmt){
//									callStack.pop();
//								} else if(lastUnit instanceof JInvokeStmt || lastUnit instanceof JAssignStmt){//Only checks for recursion
//									SootMethod method_called = getMethodCalled(lastUnit);
//									if(method_called != null){
//										if(Scene.v().getApplicationClasses().contains(method_called.getDeclaringClass()))
//											callStack.push(meth);
//									}
//								}
//							}
							
							if(!currPath.contains(unit)){
								currPath.add(unit);
								currLoopSegment = new Path<>();
							} else {
								unitDeletion++;
								if(currLoopSegment.contains(unit)){
									//this is looping on itself
									boolean foundPath = false;
									for(Path<Unit> loopedPath : backEdges){
										if(loopedPath.equals(currLoopSegment)){
											foundPath = true;
											repCount++;
											break;
										}
									}
									if(!foundPath){
										backEdges.add(currLoopSegment);
									}
									currLoopSegment = new Path<>();
								} else {
									currLoopSegment.add(unit);
								}
							}
							
							lastUnit = unit;
							break;
						}
					}
					if(found){
						line = reader.readLine();
						continue;
					}
					throw new Error("Unit not found"+unitString);
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new Error("The data file couldn't be found! "+fileNumber);
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error occured while reading the data!");
			}
			System.out.println("["+_class.getShortName()+"] Finished file "+fileNumber);
			fileNumber++;
			toRead = new File(PrintInfo.FILE_LOCATION+_class.getShortName()+fileNumber+".txt");
		}
		
		System.out.println("Finished reading from the files");
		System.out.println(currPath);
		if(!currPath.equals(new Path<>())){
			boolean inMap = false;
			int location;
			for(location = 0; location < pathCounts.size(); location++){
				if(pathCounts.get(location).first().equals(currPath)){
					inMap = true;
					break;
				}
			}
			if(inMap){
				System.out.println("["+_class.getShortName()+"] Went along a previous path");
				pathCounts.get(location).setSecond(new Integer(pathCounts.get(location).second()+1+repCount+backEdges.size()));
			} else {
				System.out.println("["+_class.getShortName()+"] Found a new path");
				pathCounts.add(new Pair<>(currPath, new Integer(1+repCount+backEdges.size())));
			}
		}
		System.out.println(prevPaths);
		for(List<Object> paths : prevPaths){
			currPath = (Path<Unit>) paths.get(0);
			repCount = ((Integer) paths.get(1)).intValue();
			backEdges = (Set<Path<Unit>>) paths.get(2);
			currLoopSegment = (Path<Unit>) paths.get(3);
			boolean inMap = false;
			int location;
			for(location = 0; location < pathCounts.size(); location++){
				if(pathCounts.get(location).first().equals(currPath)){
					inMap = true;
					break;
				}
			}
			if(inMap){
				System.out.println("["+_class.getShortName()+"] Went along a previous path");
				pathCounts.get(location).setSecond(new Integer(pathCounts.get(location).second()+1+repCount+backEdges.size()));
			} else {
				System.out.println("["+_class.getShortName()+"] Found a new path");
				pathCounts.add(new Pair<>(currPath, new Integer(1+repCount+backEdges.size())));
			}
		}
		File resultDir = new File(PrintInfo.FILE_LOCATION+"results/");
		resultDir.mkdir();
		File f = new File(PrintInfo.FILE_LOCATION+"results/results_"+_class.getShortName()+".txt");
		
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))){
			f.createNewFile();
			for(Pair<Path<Unit>, Integer> path : pathCounts){
				bw.write(path.first().toString()+PrintInfo.DIVIDER);
				bw.write(path.second()+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error occurred while writing to the results file!");
		}
	}
	
	/**
	 * Returns the method called by the given unit, or null if no method was called
	 * @param unit The unit to analyze
	 * @return The method called by the unit
	 */
	private SootMethod getMethodCalled(Unit unit){
		if(unit instanceof JInvokeStmt){
			return ((JInvokeStmt) unit).getInvokeExpr().getMethod();
		} else if(unit instanceof JAssignStmt){
			for(ValueBox vb : unit.getUseBoxes()){
				if(vb.getClass().getSimpleName().equals("LinkedRValueBox")){
					if(vb.getValue() instanceof InvokeExpr){
						return ((InvokeExpr) vb.getValue()).getMethod();
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Fetches the size of all of the given paths
	 * @param pathCounts The paths to analyze
	 * @return The unit count of all of the paths
	 */
	private int pathSize(List<Pair<Path<Unit>, Integer>> pathCounts){
		int total = 0;
		for(Pair<Path<Unit>, Integer> p : pathCounts){
			total += p.first().size();
		}
		return total;
	}
	
	/**
	 * Fetches the number of times the given paths have been traversed
	 * @param pathCounts The paths to analyze
	 * @return The total amount of times these paths have been walked over
	 */
	private int traversedPaths(List<Pair<Path<Unit>, Integer>>  pathCounts){
		int total = 0;
		for(Pair<Path<Unit>, Integer> p : pathCounts){
			total += p.second().intValue();
		}
		return total;
	}
	
}
