package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import soot.Body;
import soot.ResolutionFailedException;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

/**
 * Statically enumerates all of the intra class paths within a given class
 * @author Nate Wernimont
 *
 */
public class PathEnumerator {

	/**
	 * The class whose paths will be enumerated
	 */
	private SootClass _class;
	
	/**
	 * A mapping from methods to a list of every path of type block through the method
	 */
	private HashMap<SootMethod, List<Path<Block>>> block_map;
	
	/**
	 * A mapping from methods to a list of every path of type unit through the method
	 */
	private HashMap<SootMethod, List<Path<Unit>>> unit_map;
	
	/**
	 * A mapping from paths to a feature count
	 */
	private HashMap<Path<Unit>, FeatureStatistic> features;
	
	private boolean _debug = true;
	
	/**
	 * Construct a PathEnumerator using a given class.
	 * @param _class
	 */
	public PathEnumerator(SootClass _class){
		this._class = _class;
		block_map = new HashMap<SootMethod, List<Path<Block>>>();
		unit_map = new HashMap<SootMethod, List<Path<Unit>>>();
		features = new HashMap<Path<Unit>, FeatureStatistic>();
	}
	
	/**
	 * Construct a PathEnumerator using a given class.
	 * @param _class
	 */
	public PathEnumerator(SootClass _class, boolean debug){
		this(_class);
		_debug = debug;
	}
	
	/**
	 * Generates the block map, unit map, and identifies all of the features along each unit path
	 */
	public void run(){
		if(_debug)
			System.out.println("===="+_class.getName()+"====");
		findIntraMethodPaths();
		blockToUnits();
//		for(SootMethod sm : _class.getMethods()){
//			System.out.println("==="+sm.getName()+"===");
//			for(Path<Block> path : block_map.get(sm)){
//				System.out.print("Start -> ");
//				for(Block b : path.getElements()){
//					System.out.print(b.toShortString() + " -> ");
//				}
//				System.out.println("End");
//			}
//			System.out.println(new BriefBlockGraph(sm.retrieveActiveBody()).getBlocks());
//		}
//		for(SootMethod sm : _class.getMethods()){
//			System.out.println(sm.getName()+": "+block_map.get(sm).size());
//		}
		findIntraClassPaths();
		calculateCounts(getPaths(), features);
	}
	
	/**
	 * Find all of the paths through the given class of this PathEnumerator
	 */
	private void findIntraMethodPaths(){
		for(Iterator<SootMethod> methIter = _class.methodIterator(); methIter.hasNext();){
			List<Path<Block>> methodPaths = new ArrayList<Path<Block>>();
			SootMethod method = methIter.next();
			Stack<Block> blockSt = new Stack<Block>();
			Body b = method.retrieveActiveBody();
			BlockGraph bg = new BriefBlockGraph(b);
			Block firstBlock = bg.getBlocks().get(0);
			blockSt.push(firstBlock);
			//Generates all of the possible paths through this method.
			DFS(blockSt, methodPaths);
			if(_debug)
				System.out.println("Finished method: "+method.toString());
			block_map.put(method, methodPaths);
		}
	}
	
	/**
	 * Generates the unit map from the block map
	 */
	private void blockToUnits(){
		for(SootMethod meth : _class.getMethods()){
			List<Path<Unit>> convertedList = new ArrayList<Path<Unit>>();
			for(Path<Block> toConvert : block_map.get(meth)){
				Path<Unit> converted = new Path<Unit>();
				for(Block block : toConvert.getElements()){
					for(Iterator<Unit> unitIter = block.iterator(); unitIter.hasNext();){
						converted.add(unitIter.next());
					}
				}
				convertedList.add(converted);
			}
			unit_map.put(meth, convertedList);
		}
	}
	
	/**
	 * Using the unit map, generates all of the paths through the class.
	 */
	private void findIntraClassPaths(){
		Queue<SootMethod> toDo = new LinkedBlockingQueue<SootMethod>();
		List<Pair<Path<Unit>, List<Pair<Unit, SootMethod>>>> visitedMeths = new ArrayList<>();
		HashSet<Path<Unit>> testSet = new HashSet<>();
		
		for(SootMethod meth : _class.getMethods()){
			toDo.add(meth);
		}
		
		int maxMethodsVisited = 0;
		
		while(!toDo.isEmpty()){
			SootMethod original_meth = toDo.poll();
			List<Path<Unit>> original_paths = new ArrayList<Path<Unit>>(unit_map.get(original_meth));
			boolean changed = false;
			int count = 0;
			nextPath:
			for(Path<Unit> original_path : original_paths){
				count++;
				for(Iterator<Unit> unitIter = original_path.iterator(); unitIter.hasNext();){
					Unit unit = unitIter.next();
					SootMethod methodInvoked;
					if((methodInvoked = methodInvocation(unit)) != null){
						if(!methodInvoked.equals(original_meth) && methodInvoked.getDeclaringClass().equals(_class)){
							boolean visitedBefore = false;
							Pair<Path<Unit>, List<Pair<Unit, SootMethod>>> currentPathvMethInfo = findPair(original_path, visitedMeths);
							if(currentPathvMethInfo == null)
								currentPathvMethInfo = new Pair(new Path<>(), new ArrayList<>());
							for(Pair<Unit, SootMethod> methodsCalled : currentPathvMethInfo.second()){
								if(methodsCalled.first().equals(unit)){
									visitedBefore = true;
								}
								if(methodsCalled.second().equals(methodInvoked)){
									visitedBefore = true;
								}
							}
							
							if(!visitedBefore){
								List<Path<Unit>> originalMethPaths = unit_map.get(original_meth);
								boolean first = true;
								for(Path<Unit> methodInvokedPath : unit_map.get(methodInvoked)){
									if(first){
										first = false;
										originalMethPaths.remove(original_path);
										visitedMeths.remove(currentPathvMethInfo);
									}
									changed = true;
									Path<Unit> newPath = new Path<>(original_path);
									if(unit instanceof JAssignStmt){
										newPath.insertBefore(unit, methodInvokedPath);
									} else {
										newPath.insertAfter(unit, methodInvokedPath);
									}
									Pair<Path<Unit>, List<Pair<Unit, SootMethod>>> newVisitedMethInfo = null;
									newVisitedMethInfo = new Pair(new Path<>(currentPathvMethInfo.first()), new ArrayList<>(currentPathvMethInfo.second()));
									newVisitedMethInfo.second().add(new Pair(unit, methodInvoked));
									originalMethPaths.add(newPath);
									newVisitedMethInfo.setFirst(newPath);
									Pair<Path<Unit>, List<Pair<Unit, SootMethod>>> otherPathVisitedMethInfo = findPair(methodInvokedPath, visitedMeths);
									if(otherPathVisitedMethInfo != null)
										newVisitedMethInfo.second().addAll(otherPathVisitedMethInfo.second());
									visitedMeths.add(newVisitedMethInfo);
									//System.out.println("Adding a new path! "+originalMethPaths.size()+" : "+original_paths.size()+" : "+" : "+methodInvokedPath+" : "+count);
								}
								continue nextPath;
							}
						}
					}
				}
			}
			if(changed){
				toDo.add(original_meth);
			}
		}
		
	}
	
	private Pair<Path<Unit>, List<Pair<Unit, SootMethod>>> findPair(Path<Unit> pathToFind, List<Pair<Path<Unit>, List<Pair<Unit, SootMethod>>>> toSearch){
		for(Pair<Path<Unit>, List<Pair<Unit, SootMethod>>> pair : toSearch){
			if(pair.first().equals(pathToFind))
				return pair;
		}
		return null;
	}
	
	/**
	 * Calculate all of the feature counts for each path
	 */
	public void calculateCounts(List<Path<Unit>> paths, Map<Path<Unit>, FeatureStatistic> featuresMap){
		//Get the invocations count
		int maxInvocations = 0;
		for(Path<Unit> path : paths){
			FeatureStatistic feature = new FeatureStatistic();
			int invocations = 0;
			SootMethod initialMethod = null;
			for(SootMethod sm : _class.getMethods()){
				if(Path.unitEquals(sm.retrieveActiveBody().getUnits().getFirst(), path.getElements().get(0))){
					initialMethod = sm;
					break;
				}
					
			}
			for(Unit unit : path.getElements()){
				if(methodInvocation(unit) != null){
					invocations++;
					feature.increment(Count.INVOCATIONS);
					if(initialMethod.retrieveActiveBody().getUnits().contains(unit)){
						feature.increment(Count.LOCAL_INVOCATIONS);
					} else {
						feature.increment(Count.NON_LOCAL_INVOCATIONS);
					}
				}
			}
			featuresMap.put(path, feature);
			maxInvocations = invocations > maxInvocations ? invocations : maxInvocations;
		}
		
		//Get the invocations coverages
		for(Path<Unit> path : paths){
			FeatureStatistic feature = featuresMap.get(path);
			feature.setValue(Coverage.INVOCATIONS, (float)(feature.getValue(Count.INVOCATIONS)/maxInvocations));
		}
		
		
		int maxFieldsWritten = 0;
		int maxVariablesAccessed = 0;
		int maxParametersUsed = 0;
		
		for(Path<Unit> path : paths){
			FeatureStatistic feature = featuresMap.get(path);
			if(feature == null)
				feature = new FeatureStatistic();
			Set<Value> localVariables = new HashSet<Value>();
			Set<Value> allVariables = new HashSet<Value>();
			for(Unit unit : path.getElements()){
				feature.increment(Count.STATEMENTS);
				switch(unit.getClass().getSimpleName()){
				case "JAssignStmt":
					Value varValue = null;
					Pair<Value, Boolean> localVariable = new Pair<Value, Boolean>(null, false);
					for(ValueBox box : unit.getUseBoxes()){
						if(box.getClass().getSimpleName().equals("JimpleLocalBox")){
							localVariable.setFirst(box.getValue());
							localVariable.setSecond(true);
						} else if(box.getClass().getSimpleName().equals("LinkedRValueBox")){
							if(box.getValue().getUseBoxes().size() > 1){//More than one operand
								for(Object useBox : box.getValue().getUseBoxes()){
									if(useBox.getClass().getSimpleName().equals("ImmediateBox")){
										ImmediateBox value = (ImmediateBox) useBox;
										if(value.getValue().getClass().getSimpleName().equals("JimpleLocal")){
											localVariables.add(box.getValue());
											allVariables.add(box.getValue());
											feature.increment(Count.DEREFERENCES);
										}
									}
								}
							} else {
								if(box.getValue().getClass().getSimpleName().equals("JimpleLocal")){
									localVariables.add(box.getValue());
									allVariables.add(box.getValue());
									feature.increment(Count.DEREFERENCES);
								} else if(box.getValue().getClass().getSimpleName().equals("JInstanceFieldRef")){
									feature.increment(Count.DEREFERENCES);
									feature.increment(Count.THIS);
									allVariables.add(box.getValue());
								}
							}
							if(box.getValue().getClass().getSimpleName().equals("JNewExpr"))//new
								feature.increment(Count.NEW);
						}
					}
					for(ValueBox box : unit.getDefBoxes()){
						if(box.getClass().getSimpleName().equals("LinkedVariableBox")){
							varValue = box.getValue();
							allVariables.add(varValue);
							if(localVariable.second() && localVariable.first().equals(box.getValue())){
								localVariables.add(varValue);
							}
							
							if(varValue.getClass().getSimpleName().equals("JInstanceFieldRef")){
								feature.increment(Count.THIS);//This covers if statements as well
							}
						}
					}
					
					
					feature.increment(Count.ASSIGNMENTS);
					break;
				case "JBreakpointStmt":
					break;
				case "JEnterMonitorStmt":
					break;
				case "JExitMonitorStmt":
					break;
				case "JGotoStmt":
					feature.increment(Count.GOTO);
					break;
				case "JIdentityStmt":
					for(ValueBox vb : unit.getUseBoxes()){
						if(vb.getValue().getClass().getSimpleName().equals("ParameterRef")){
							feature.increment(Count.PARAMETERS);
						}
					}
					break;
				case "JIfStmt":
					feature.increment(Count.IF);
					for(ValueBox box : unit.getUseBoxes()){
						for(Object useBox : box.getValue().getUseBoxes()){
							if(useBox.getClass().getSimpleName().equals("ImmediateBox")){
								ImmediateBox value = (ImmediateBox) useBox;
								if(value.getValue().getClass().getSimpleName().equals("JimpleLocal")){
									localVariables.add(box.getValue());
									allVariables.add(box.getValue());
									feature.increment(Count.DEREFERENCES);
								}
							}
						}
						if(box.getClass().getSimpleName().equals("ConditionExprBox")){
							feature.increment(Count.COMPARISONS);
						}
					}
					break;
				case "JInvokeStmt":
					break;
				case "JLookupSwitchStmt":
					break;
				case "JNopStmt":
					break;
				case "JRetStmt":
				case "JReturnStmt":
				case "JReturnVoidStmt":
					feature.increment(Count.RETURN);
					break;
				case "JTableSwitchStmt":
					break;
				case "JThrowStmt":
					feature.increment(Count.THROW);
					break;
				default:
					System.out.println("Weird: "+unit.getClass().getName()+", "+unit.getClass().getSimpleName());
				}
			}
			
			feature.increment(Count.ALL_VARIABLES, allVariables.size());
			feature.increment(Count.LOCAL_VARIABLES, localVariables.size());
			
			Iterator<SootField> fieldsWrittenIter = _class.getFields().snapshotIterator();
			Set<SootField> fields = new HashSet<SootField>();
			while(fieldsWrittenIter.hasNext()){
				SootField eval = fieldsWrittenIter.next();
				for(Value v : allVariables){
					if(v.toString().contains(eval.toString())){
						fields.add(eval);
						feature.increment(Count.FIELDS_WRITTEN);
					}
				}
			}
			feature.setValue(Coverage.FIELDS, (float)fields.size()/_class.getFieldCount());
			feature.increment(Count.FIELDS, fields.size());
			
			if(feature.getValue(Count.FIELDS_WRITTEN) > maxFieldsWritten)
				maxFieldsWritten = feature.getValue(Count.FIELDS_WRITTEN);
			if(feature.getValue(Count.LOCAL_VARIABLES) > maxVariablesAccessed)
				maxVariablesAccessed = feature.getValue(Count.LOCAL_VARIABLES);
			if(feature.getValue(Count.PARAMETERS) > maxParametersUsed)
				maxParametersUsed = feature.getValue(Count.PARAMETERS);
			
			featuresMap.put(path, feature);
		}
		
		//coverage
		for(Path<Unit> path : paths){
			FeatureStatistic feature = featuresMap.get(path);
			feature.setValue(Coverage.FIELDS_WRITTEN, maxFieldsWritten > 0 ? (float)(feature.getValue(Count.FIELDS_WRITTEN)/maxFieldsWritten) : 0);
			feature.setValue(Coverage.LOCAL_VARIABLES, maxVariablesAccessed > 0 ? (float)(feature.getValue(Count.LOCAL_VARIABLES)/maxVariablesAccessed) : 0);
			feature.setValue(Coverage.PARAMETERS, maxParametersUsed > 0 ? (float)(feature.getValue(Count.PARAMETERS)/maxParametersUsed) : 0);
		}
	}
	
	/**
	 * Find out if a unit has a method invocation in it
	 * @param unit The unit to examine
	 * @return The method that is invoked or null if there is no invocation
	 */
	public SootMethod methodInvocation(Unit unit){
		if(unit instanceof JInvokeStmt){
			try {
				return ((JInvokeStmt) unit).getInvokeExpr().getMethod();
			} catch(ResolutionFailedException e){
				//This error only occurs when I use calculateCounts with my own paths, and I'm just checking for null or not
				return _class.getMethods().get(0);
			}
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
	 * Generate all acyclic paths through a method given a stack containing the first block of the method.
	 * @param blockStack A stack containing the first block of the method
	 * @param masterPaths A list of all of the paths through the method
	 */
	private void DFS(Stack<Block> blockStack, List<Path<Block>> masterPaths){
		DFSwithStarter(blockStack, masterPaths, blockStack.peek(), null);
	}
	
	private void DFSwithStarter(Stack<Block> blockStack, List<Path<Block>> masterPaths, Block startBlock, Block dontTravel){
		List<Block> succs = startBlock.getSuccs();
		if(succs.isEmpty()){//It is an exit node
			Path<Block> pathToAdd = new Path<Block>(blockStack);
			masterPaths.add(pathToAdd);
			return;
		}
		for(Block succ : succs){
			if(!succ.equals(dontTravel)){
				if(!blockStack.contains(succ)){
					blockStack.push(succ);
					DFSwithStarter(blockStack, masterPaths, succ, null);
					blockStack.pop();
				} else if(getCount(blockStack, succ) == 1){
					DFSwithStarter(blockStack, masterPaths, succ, loopHead(blockStack, succ));
				}
			}
		}
	}
	
	private Block loopHead(Stack<Block> blockStack, Block loopedTo){
		Block lastBlock = blockStack.peek();
		Stack<Block> copy = (Stack<Block>) blockStack.clone();
		while(!copy.isEmpty()){
			if(loopedTo.equals(copy.peek()))
				return lastBlock;
			lastBlock = copy.pop();
		}
		return null;
	}
	
	private int getCount(Stack<Block> blockStack, Block block){
		int count = 0;
		List<Block> copy = new ArrayList<Block>(blockStack);
		for(Block b : copy){
			if(b.equals(block))
				count++;
		}
		return count;
	}
	
	/**
	 * Extract the block map
	 * @return The block map
	 */
	public HashMap<SootMethod, List<Path<Block>>> getBlockMap(){
		return block_map;
	}
	
	/**
	 * Extract the unit map
	 * @return The unit map
	 */
	public HashMap<SootMethod, List<Path<Unit>>> getUnitMap(){
		return unit_map;
	}
	
	/**
	 * Extract the feature counts for each path
	 * @return Hashmap mapping the paths to the feature statistics
	 */
	public HashMap<Path<Unit>, FeatureStatistic> getFeatureStatistics(){
		return features;
	}
	
	/**
	 * Extract a list of the unit paths
	 * @return A list of the unit paths
	 */
	public List<Path<Unit>> getPaths(){
		List<Path<Unit>> paths = new ArrayList<>();
		for(SootMethod sm : unit_map.keySet()){
			paths.addAll(unit_map.get(sm));
		}
		return paths;
	}
	
}
