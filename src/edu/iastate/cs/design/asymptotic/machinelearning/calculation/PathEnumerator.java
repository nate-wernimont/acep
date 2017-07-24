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
import java.util.function.Predicate;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Count;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic.Coverage;
import soot.Body;
import soot.PatchingChain;
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
	private HashMap<SootMethod, List<List<Block>>> block_map;
	
	/**
	 * A mapping from methods to a list of every path of type unit through the method
	 */
	private HashMap<SootMethod, List<Path>> unit_map;
	
	private boolean _debug = true;
	
	/**
	 * Construct a PathEnumerator using a given class.
	 * @param _class
	 */
	public PathEnumerator(SootClass _class){
		this._class = _class;
		block_map = new HashMap<SootMethod, List<List<Block>>>();
		unit_map = new HashMap<SootMethod, List<Path>>();
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
		findIntraMethodPaths(_class.getMethods());
		blockToUnits();
//		for(SootMethod sm : _class.getMethods()){
//			System.out.println("==="+sm.getName()+"===");
//			for(List<Block> path : block_map.get(sm)){
//				System.out.print("Start -> ");
//				for(Block b : path){
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
	}
	
	/**
	 * Find all of the paths through the given class of this PathEnumerator
	 */
	public void findIntraMethodPaths(List<SootMethod> meths){
		for(SootMethod method : meths){
			List<List<Block>> methodPaths = new ArrayList<List<Block>>();
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
	public void blockToUnits(){
		for(SootMethod meth : _class.getMethods()){
			List<Path> convertedList = new ArrayList<Path>();
			for(List<Block> toConvert : block_map.get(meth)){
				Set<Block> completed = new HashSet<>();
				Path converted = new Path();
				for(Block block : toConvert){
					if(toConvert.indexOf(block) != toConvert.lastIndexOf(block) && completed.contains(block)){
						converted.add(block.getHead());
					} else {
						for(Iterator<Unit> unitIter = block.iterator(); unitIter.hasNext();){
							converted.add(unitIter.next());
						}
					}
					completed.add(block);
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
		List<Pair<Path, List<Pair<Unit, SootMethod>>>> visitedMeths = new ArrayList<>();
		HashSet<Path> testSet = new HashSet<>();
		
		for(SootMethod meth : _class.getMethods()){
			toDo.add(meth);
		}
		
		int maxMethodsVisited = 0;
		
		while(!toDo.isEmpty()){
			SootMethod original_meth = toDo.poll();
			List<Path> original_paths = new ArrayList<Path>(unit_map.get(original_meth));
			boolean changed = false;
			int count = 0;
			nextPath:
			for(Path original_path : original_paths){
				count++;
				for(Iterator<Object> unitIter = original_path.iterator(); unitIter.hasNext();){
					Object o = unitIter.next();
					Unit unit = null;
					if(!(o instanceof Unit)){
						continue;
					} else {
						unit = (Unit) o;
					}
					SootMethod methodInvoked;
					if((methodInvoked = methodInvocation(unit)) != null){
						if(!methodInvoked.equals(original_meth) && methodInvoked.getDeclaringClass().equals(_class)){
							boolean visitedBefore = false;
							Pair<Path, List<Pair<Unit, SootMethod>>> currentPathvMethInfo = findPair(original_path, visitedMeths);
							if(currentPathvMethInfo == null){
								currentPathvMethInfo = new Pair(original_path, new ArrayList<>());
								visitedMeths.add(currentPathvMethInfo);
							}
							for(Pair<Unit, SootMethod> methodsCalled : currentPathvMethInfo.second()){
								if(methodsCalled.first().equals(unit)){
									visitedBefore = true;
								}
								if(methodsCalled.second().equals(methodInvoked)){
									visitedBefore = true;
								}
							}
							
							if(!visitedBefore){
								if(unit instanceof JAssignStmt){
									original_path.addBefore(unit, unit_map.get(methodInvoked));
								} else {
									original_path.addAfter(unit, unit_map.get(methodInvoked));
								}
								changed = true;
								currentPathvMethInfo.second().add(new Pair<Unit, SootMethod>(unit, methodInvoked));
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
	
	private Pair<Path, List<Pair<Unit, SootMethod>>> findPair(Path pathToFind, List<Pair<Path, List<Pair<Unit, SootMethod>>>> toSearch){
		for(Pair<Path, List<Pair<Unit, SootMethod>>> pair : toSearch){
			if(pair.first().equals(pathToFind))
				return pair;
		}
		return null;
	}
	
	/**
	 * Calculate all of the feature counts for each path
	 */
	public void calculateCounts(List<List<Unit>> paths, Map<List<Unit>, FeatureStatistic> featuresMap, boolean ignoreLocalInvocations){
		for(int i = 0; i < paths.size(); i++){
			featuresMap.put(paths.get(i), new FeatureStatistic());
		}
		//Get the invocations count
		int maxInvocations = 0;
		for(List<Unit> path : paths){
			FeatureStatistic feature = featuresMap.get(path);
			int invocations = 0;
			SootMethod initialMethod = null;
			if(!ignoreLocalInvocations){
				for(SootMethod sm : _class.getMethods()){
					if(unitEquals(sm.retrieveActiveBody().getUnits().getFirst(), path.get(0))){
						initialMethod = sm;
						break;
					}
				}
			}
			for(Unit unit : path){
				if(methodInvocationBool(unit)){
					invocations++;
					feature.increment(Count.INVOCATIONS);
					if(!ignoreLocalInvocations){
						Predicate<PatchingChain<Unit>> local = (v) -> {
							for(Unit u : v){
								if(u.equals(unit)){
									return true;
								}
							}
							return false;
						};
						if(local.test((initialMethod.retrieveActiveBody().getUnits()))){
							feature.increment(Count.LOCAL_INVOCATIONS);
						} else {
							feature.increment(Count.NON_LOCAL_INVOCATIONS);
						}
					} else {
						feature.increment(Count.NON_LOCAL_INVOCATIONS);
					}
				}
			}
			featuresMap.put(path, feature);
			maxInvocations = invocations > maxInvocations ? invocations : maxInvocations;
		}
		
		//Get the invocations coverages
		for(List<Unit> path : paths){
			FeatureStatistic feature = featuresMap.get(path);
			feature.setValue(Coverage.INVOCATIONS, (float)(feature.getValue(Count.INVOCATIONS)/maxInvocations));
		}
		
		
		int maxFieldsWritten = 0;
		int maxVariablesAccessed = 0;
		int maxParametersUsed = 0;
		int debugCount = 0;
		
		for(List<Unit> path : paths){
			debugCount++;
			if(debugCount % 1000 == 0){
				System.out.println("Finished finding "+debugCount+" statistics");
			}
			
			FeatureStatistic feature = featuresMap.get(path);
			if(feature == null)
				feature = new FeatureStatistic();
			Set<Value> localVariables = new HashSet<Value>();
			Set<Value> allVariables = new HashSet<Value>();
			for(Unit unit : path){
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
		for(List<Unit> path : paths){
			FeatureStatistic feature = featuresMap.get(path);
			feature.setValue(Coverage.FIELDS_WRITTEN, maxFieldsWritten > 0 ? (float)(feature.getValue(Count.FIELDS_WRITTEN)/maxFieldsWritten) : 0);
			feature.setValue(Coverage.LOCAL_VARIABLES, maxVariablesAccessed > 0 ? (float)(feature.getValue(Count.LOCAL_VARIABLES)/maxVariablesAccessed) : 0);
			feature.setValue(Coverage.PARAMETERS, maxParametersUsed > 0 ? (float)(feature.getValue(Count.PARAMETERS)/maxParametersUsed) : 0);
		}
	}
	
	public boolean methodInvocationBool(Unit unit){
		if(unit instanceof JInvokeStmt){
			return true;
		} else if(unit instanceof JAssignStmt){
			for(ValueBox vb : unit.getUseBoxes()){
				if(vb.getClass().getSimpleName().equals("LinkedRValueBox")){
					if(vb.getValue() instanceof InvokeExpr){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Find out if a unit has a method invocation in it
	 * @param unit The unit to examine
	 * @return The method that is invoked or null if there is no invocation
	 */
	public static SootMethod methodInvocation(Unit unit){
		if(unit instanceof JInvokeStmt){
			try {
				return ((JInvokeStmt) unit).getInvokeExpr().getMethod();
			} catch(ResolutionFailedException e){
				//This error only occurs when I use calculateCounts with my own paths, and I'm just checking for null or not
				return new SootMethod(null, null, null);
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
	 * @param methodPaths A list of all of the paths through the method
	 */
	private void DFS(Stack<Block> blockStack, List<List<Block>> methodPaths){
		DFSwithStarter(blockStack, methodPaths, blockStack.peek(), null);
	}
	
	private void DFSwithStarter(Stack<Block> blockStack, List<List<Block>> methodPaths, Block startBlock, Block dontTravel){
		List<Block> succs = startBlock.getSuccs();
		if(succs.isEmpty()){//It is an exit node
			List<Block> pathToAdd = new ArrayList<Block>(blockStack);
			methodPaths.add(pathToAdd);
			return;
		}
		for(Block succ : succs){
			if(!succ.equals(dontTravel)){
				if(!blockStack.contains(succ)){
					blockStack.push(succ);
					DFSwithStarter(blockStack, methodPaths, succ, null);
					blockStack.pop();
				} else if(getCount(blockStack, succ) == 1){
					blockStack.push(succ);
					DFSwithStarter(blockStack, methodPaths, succ, loopHead(blockStack, succ));
					blockStack.pop();
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
	public HashMap<SootMethod, List<List<Block>>> getBlockMap(){
		return block_map;
	}
	
	/**
	 * Extract the unit map
	 * @return The unit map
	 */
	public HashMap<SootMethod, List<Path>> getUnitMap(){
		return unit_map;
	}
	
	/**
	 * Extract a list of the unit paths
	 * @return A list of the unit paths
	 */
	public List<Path> getPaths(){
		List<Path> paths = new ArrayList<>();
		for(SootMethod sm : unit_map.keySet()){
			paths.addAll(unit_map.get(sm));
		}
		return paths;
	}
	
	public List<List<Unit>> getListPaths(){
		List<List<Unit>> convertedPaths = new ArrayList<>();
		for(Path path: getPaths()){
			convertedPaths.addAll(path.getAllPaths(null));
			//System.out.println(convertedPaths.size());
		}
		return convertedPaths;
	}
	
	public SootClass getDeclaredClass(){
		return _class;
	}
	
	public static boolean unitEquals(Unit unit1, Unit unit2){
		if(unit1 == null && unit2 == null)
			return true;
		if(unit1 == null || unit2 == null)
			return false;
		if(unit1.getTags().size() != unit2.getTags().size())
			return false;
		for(int i = 0; i < unit1.getTags().size(); i++){
			if(!(unit1.getTags().get(i).getName().equals(unit2.getTags().get(i).getName()) && unit1.getTags().get(i).toString().equals(unit2.getTags().get(i).toString())))
				return false;
		}
		
		return (unit1.branches() == unit2.branches()) &&
				(unit1.fallsThrough() == unit2.fallsThrough()) &&
				unit1.getUseAndDefBoxes().toString().equals(unit2.getUseAndDefBoxes().toString()) &&
				unit1.getBoxesPointingToThis().equals(unit2.getBoxesPointingToThis()) &&
				unit1.getUnitBoxes().equals(unit2.getUnitBoxes()) &&
				unit1.getClass().equals(unit2.getClass());
	}
	
}
