package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import soot.Body;
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
		/*for(SootMethod sm : _class.getMethods()){
			System.out.println("==="+sm.getName()+"===");
			for(Path<Block> path : block_map.get(sm)){
				System.out.print("Start -> ");
				for(Block b : path.getElements()){
					System.out.print(b.toShortString() + " -> ");
				}
				System.out.println("End");
			}
		}*/
		findIntraClassPaths();
		calculateCounts();
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
		Queue<SootMethod> initialPass = new LinkedBlockingQueue<SootMethod>();
		HashMap<Path<Unit>, List<Unit>> visitedMeths = new HashMap<>();
		
		for(SootMethod meth : _class.getMethods()){
			initialPass.add(meth);
		}
		
		int maxMethodsVisited = 0;
		
		//This does a first pass through, which collects the method invocations only from the method that it is analyzing
		while(!initialPass.isEmpty()){
			SootMethod original_meth = initialPass.poll();
			List<Path<Unit>> original_paths = new ArrayList<Path<Unit>>(unit_map.get(original_meth));
			boolean changed = false;
			for(Path<Unit> original_path : original_paths){
				for(Unit unit : original_path.getElements()){
					SootMethod method_called = methodInvocation(unit);
					if(method_called != null){
						FeatureStatistic updateMethodsCalled = features.get(original_path);
						if(updateMethodsCalled == null){//It didnt have one yet
							updateMethodsCalled = new FeatureStatistic();
							features.put(original_path, updateMethodsCalled);
						}
						List<Unit> oldVisitedMeths = visitedMeths.get(original_path);
						if(oldVisitedMeths == null){
							oldVisitedMeths = new ArrayList<Unit>();
							visitedMeths.put(original_path, oldVisitedMeths);
						}
						if(method_called.equals(original_meth)){//local invocation of itself
							//I do not want this path - it involves an infinite loop and I only want one element of each
							unit_map.get(original_meth).remove(original_path);
							features.remove(original_path);
						} else if(method_called.getDeclaringClass().equals(_class) && !oldVisitedMeths.contains(unit)){
							for(Path<Unit> path_in_path : unit_map.get(method_called)){
								if(original_path.contains(path_in_path.getElements().get(0))){//local invocation of a method that has already been called
									unit_map.get(original_meth).remove(original_path);
									features.remove(original_path);
									continue;
								}
								//remake the path
								updateMethodsCalled = new FeatureStatistic(updateMethodsCalled);
								Path<Unit> newPath = original_path.copy();
								if(unit instanceof JAssignStmt){
									newPath.insertBefore(unit, path_in_path);
								} else {
									newPath.insertAfter(unit, path_in_path);
								}
								List<Path<Unit>> paths = unit_map.get(original_meth);
								paths.remove(original_path);
								paths.add(newPath);
								unit_map.put(original_meth, paths);
								List<Unit> updateVisitedMeths = new ArrayList<>(oldVisitedMeths);
								updateVisitedMeths.add(unit);
								visitedMeths.remove(original_path);
								visitedMeths.put(newPath, updateVisitedMeths);
								changed = true;
								
								//Update featurecount
								features.remove(original_path);
								updateMethodsCalled.increment(Count.INVOCATIONS);
								updateMethodsCalled.increment(Count.LOCAL_INVOCATIONS);
								features.put(newPath, updateMethodsCalled);
							}
						} else {
							updateMethodsCalled.increment(Count.INVOCATIONS);
							updateMethodsCalled.increment(Count.NON_LOCAL_INVOCATIONS);
						}
						if(((int)updateMethodsCalled.getValue(Count.INVOCATIONS)) > maxMethodsVisited)
							maxMethodsVisited = (int) updateMethodsCalled.getValue(Count.INVOCATIONS);
					}
				}
			}
			if(changed){
				toDo.add(original_meth);
			}
		}
		
		//Get the invocations coverage
		for(SootMethod sm : _class.getMethods()){
			for(Path<Unit> p : unit_map.get(sm)){
				FeatureStatistic fc = features.get(p);
				if(fc == null)
					fc = new FeatureStatistic();
				if(maxMethodsVisited > 0)
					fc.setValue(Coverage.INVOCATIONS, ((int)fc.getValue(Count.INVOCATIONS))/maxMethodsVisited);
				else
					fc.setValue(Coverage.INVOCATIONS, 0);
				features.put(p, fc);
			}
		}
		
		//Finish flattening out the class
		while(!toDo.isEmpty()){
			SootMethod original_meth = toDo.poll();
			List<Path<Unit>> original_paths = new ArrayList<Path<Unit>>(unit_map.get(original_meth));
			boolean changed = false;
			for(Path<Unit> original_path : original_paths){
				for(Unit unit : original_path.getElements()){
					SootMethod method_called = methodInvocation(unit);
					if(method_called != null){
						List<Unit> oldVisitedMeths = visitedMeths.get(original_path);
						if(oldVisitedMeths == null){
							oldVisitedMeths = new ArrayList<Unit>();
							visitedMeths.put(original_path, oldVisitedMeths);
						}
						if(method_called.getDeclaringClass().equals(_class) && !method_called.equals(original_meth) && !oldVisitedMeths.contains(unit)){
							for(Path<Unit> path_in_path : unit_map.get(method_called)){
								if(original_path.contains(path_in_path.getElements().get(0)))//local invocation of a method that has already been called
									continue;
								
								//remake the path
								Path<Unit> newPath = original_path.copy();
								if(unit instanceof JAssignStmt){
									newPath.insertBefore(unit, path_in_path);
								} else {
									newPath.insertAfter(unit, path_in_path);
								}
								List<Path<Unit>> paths = unit_map.get(original_meth);
								paths.remove(original_path);
								paths.add(newPath);
								unit_map.put(original_meth, paths);
								changed = true;
								List<Unit> updateVisitedMeths = new ArrayList<>(oldVisitedMeths);
								updateVisitedMeths.add(unit);
								visitedMeths.remove(original_path);
								visitedMeths.put(newPath, updateVisitedMeths);
								
								//Propagate the features count
								FeatureStatistic feature = features.get(original_path);
								features.put(newPath, new FeatureStatistic(feature));
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
	
	/**
	 * Calculate all of the feature counts for each path
	 */
	private void calculateCounts(){
		int maxFieldsWritten = 0;
		int maxVariablesAccessed = 0;
		int maxParametersUsed = 0;
		
		for(SootMethod sm : _class.getMethods()){
			for(Path<Unit> path : unit_map.get(sm)){
				FeatureStatistic feature = features.get(path);
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
				
				if(((int)feature.getValue(Count.FIELDS_WRITTEN)) > maxFieldsWritten)
					maxFieldsWritten = (int) feature.getValue(Count.FIELDS_WRITTEN);
				if(((int)feature.getValue(Count.LOCAL_VARIABLES)) > maxVariablesAccessed)
					maxVariablesAccessed = (int) feature.getValue(Count.LOCAL_VARIABLES);
				if(((int)feature.getValue(Count.PARAMETERS)) > maxParametersUsed)
					maxParametersUsed = (int) feature.getValue(Count.PARAMETERS);
				
				features.put(path, feature);
			}
		}
		//coverage
		for(SootMethod sm : _class.getMethods()){
			for(Path<Unit> p : unit_map.get(sm)){
				FeatureStatistic feature = features.get(p);
				feature.setValue(Coverage.FIELDS_WRITTEN, ((int)feature.getValue(Count.FIELDS_WRITTEN))/maxFieldsWritten);
				feature.setValue(Coverage.LOCAL_VARIABLES, ((int)feature.getValue(Count.LOCAL_VARIABLES))/maxVariablesAccessed);
				feature.setValue(Coverage.PARAMETERS, ((int)feature.getValue(Count.PARAMETERS))/maxParametersUsed);
				features.put(p, feature);
			}
		}
	}
	
	/**
	 * Find out if a unit has a method invocation in it
	 * @param unit The unit to examine
	 * @return The method that is invoked or null if there is no invocation
	 */
	public SootMethod methodInvocation(Unit unit){
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
	 * Generate all acyclic paths through a method given a stack containing the first block of the method.
	 * @param blockStack A stack containing the first block of the method
	 * @param masterPaths A list of all of the paths through the method
	 */
	private void DFS(Stack<Block> blockStack, List<Path<Block>> masterPaths){
		Block block = blockStack.peek();
		List<Block> succs = block.getSuccs();
		if(succs.isEmpty()){//It is an exit node
			Path<Block> pathToAdd = new Path<Block>(blockStack);
			masterPaths.add(pathToAdd);
			return;
		}
		for(Block succ : succs){
			if(!blockStack.contains(succ)){
				blockStack.push(succ);
				DFS(blockStack, masterPaths);
				blockStack.pop();
			}
		}
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
