package edu.iastate.cs.design.asymptotic.interfaces.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JSeparator;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.ExecutionPath;
import edu.iastate.cs.design.asymptotic.datastructures.Interpreter;
import edu.iastate.cs.design.asymptotic.datastructures.Log;
import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Method;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.datastructures.Path;
import edu.iastate.cs.design.asymptotic.datastructures.PathGenerator;
import edu.iastate.cs.design.asymptotic.datastructures.UnitInfo;
import edu.iastate.cs.design.asymptotic.interfaces.StaticProfilePass;
import edu.iastate.cs.design.asymptotic.tests.SideEffectAnalysisTest;
import edu.iastate.cs.design.asymptotic.tests.SideEffectAnalysisTest.UniqueRWSets;

import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.baf.BafBody;
import soot.baf.internal.AbstractInst;
import soot.baf.internal.BInterfaceInvokeInst;
import soot.baf.internal.BSpecialInvokeInst;
import soot.baf.internal.BStaticInvokeInst;
import soot.baf.internal.BVirtualInvokeInst;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.RWSet;
import soot.jimple.toolkits.pointer.SideEffectAnalysis;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.pdg.Region;
import soot.toolkits.graph.pdg.RegionAnalysis;
import soot.util.Chain;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

public class Analysis {

	HashMap<String, Double> _methodTime;
	//HashMap<SootMethod, Integer> _methodCycles;
	//HashMap<SootMethod, List<Path>> _methodPaths;
	//HashMap<SootMethod, List<Block>> _methodBlocks;
	//HashMap<SootMethod, List<Block>> _methodBafBlocks;
	//HashMap<SootMethod, HashMap<Block, Block>> _srcTobaf;
	//HashMap<SootMethod, HashMap<Block, Double>> _methodBafBlockCycles;
	//HashMap<SootMethod, HashMap<Block, Double>> _methodBlockTime;
	HashMap<String, Integer> _methodLoops;
	// HashMap<String, Method> _nameToMethod;
	// static Stack<String> methodStack = new Stack<String>();
	//List<SootMethod> _library;
	Interpreter _interpreter;
	StaticProfilePassImpl _staticProfile = null;
	PathGenerator pathGen;
	public static List<String> lib_methods = new ArrayList<String>();

	SootMethod _MAIN;
	CallGraphDFS _CG_MAIN;
	
	// List of marked ACEP methods
	List<SootMethod> markedACEPMethods = new ArrayList<SootMethod>();
	
	// Modules
	List<SootClass> modules = new ArrayList<SootClass>();
	
	List<SootClass> threadModules = new ArrayList<SootClass>();

	public Analysis() {

	}

	public Analysis(SootMethod main, CallGraphDFS callgraph) {
		_MAIN = main;
		_CG_MAIN = callgraph;

		// Initialize maps
		_methodTime = new HashMap<String, Double>();
		//_methodCycles = new HashMap<SootMethod, Integer>();
		//_methodPaths = new HashMap<SootMethod, List<Path>>();
		//_methodBlocks = new HashMap<SootMethod, List<Block>>();
		//_methodBafBlocks = new HashMap<SootMethod, List<Block>>();
		//_srcTobaf = new HashMap<SootMethod, HashMap<Block, Block>>();
		//_methodBafBlockCycles = new HashMap<SootMethod, HashMap<Block, Double>>();
		//_methodBlockTime = new HashMap<SootMethod, HashMap<Block, Double>>();
		//_library = new ArrayList<SootMethod>();
		_methodLoops = new HashMap<String, Integer>();
		// _nameToMethod = new HashMap<String, Method>();

		// Initialize static log class
		Log.init();

		_interpreter = new Interpreter();

		_staticProfile = new StaticProfilePassImpl(_CG_MAIN, _MAIN);

		timingAnalysis();

		markACEPMethods();
		
		CallGraphDFS cg = new CallGraphDFS(_MAIN);
		cg.setACEPMethods(markedACEPMethods);
		cg.drawCG(_MAIN.getDeclaringClass().toString());
		
		// R v done? Let's print the collected lib_methods now...
		/*
		 * System.out.println(
		 * "============================================Lib Methods Collected=============================================="
		 * ); for (String method : lib_methods) { System.out.println(method); }
		 */
		//display();
		System.out.println("Analysis Completed....");
		Log.close();
	}

	public Analysis(String modulename) {
		/*
		 * _MAIN = main; _CG_MAIN = callgraph;
		 */

		// Initialize maps
		_methodTime = new HashMap<String, Double>();
		//_methodCycles = new HashMap<SootMethod, Integer>();
		//_methodPaths = new HashMap<SootMethod, List<Path>>();
		//_methodBlocks = new HashMap<SootMethod, List<Block>>();
		//_methodBafBlocks = new HashMap<SootMethod, List<Block>>();
		//_srcTobaf = new HashMap<SootMethod, HashMap<Block, Block>>();
		//_methodBafBlockCycles = new HashMap<SootMethod, HashMap<Block, Double>>();
		//_methodBlockTime = new HashMap<SootMethod, HashMap<Block, Double>>();
		//_library = new ArrayList<SootMethod>();
		// _nameToMethod = new HashMap<String, Method>();
		_methodLoops = new HashMap<String, Integer>();

		// Initialize static log class
		Log.init();

		_interpreter = new Interpreter();

		_staticProfile = new StaticProfilePassImpl(modulename);

		_CG_MAIN = _staticProfile.CG;

		timingAnalysis();

		// R v done? Let's print the collected lib_methods now...
		/*
		 * System.out.println(
		 * "============================================Lib Methods Collected=============================================="
		 * ); for (String method : lib_methods) { System.out.println(method); }
		 */
		System.out.println("Analysis Completed....");
		Log.close();
	}

	/*
	 * private List<SootMethod> postOrderTraversal () { List<SootMethod> methods
	 * = new ArrayList<SootMethod>();
	 * 
	 * }
	 */

	long libMethodTime(SootMethod method) {
		long startTime = System.currentTimeMillis();
		// Execute the method here....
		long endTime = System.currentTimeMillis();
		return (endTime - startTime);
	}

	void displayEdgeProbabilities(HashMap<Pair<Block, Block>, Double> eFreq) {
		System.out
				.println("===================================== Edge Probabilities =========================");
		for (Entry entry : eFreq.entrySet()) {
			System.out.println(entry.getKey().toString() + " : "
					+ entry.getValue().toString());
		}
	}
	
	public void display () {    
        // display the list of application classes
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for (SootClass sootClass : classes) {
        	boolean isThread = false;
        	if (modules.contains(sootClass)) {
	        	if (threadModules.contains(sootClass))	isThread = true;
				System.out.println ("===============================================");
				System.out.println (sootClass.getName()+" isThread := "+isThread);
				System.out.println ("===============================================");
				List<SootMethod> methods = sootClass.getMethods();
				for (SootMethod method : methods) {
					String methodString = Method.getMethodString(method);
					System.out.println (method.getSignature()+"---"+_methodTime.get(methodString));
				}
				System.out.println();
        	}
		}
	}
	
	public boolean isThreadModule (SootClass module) {
		List<SootMethod> methods = module.getMethods();
		for (SootMethod sootMethod : methods) {
			if(sootMethod.getName().contains("run"))	return true;
		}
		return false;
	}

	double computeAverageMethodTime(SootMethod method, Collection<ExecutionPath> paths,
			HashMap<Block, Double> blockTimes) {
		//HashMap<Path, Double> pathProbabilities = new HashMap<Path, Double>();
		HashMap<Pair<Block, Block>, Double> efreqMap = null;
		HashMap<Pair<Block, Block>, Double> eProbMap = null;
		if (_staticProfile != null) {
			efreqMap = _staticProfile.getMethodEdgeInformation(method);// _staticProfile.getEdgeFrequencyMap(method);
			eProbMap = _staticProfile.getEdgeFrequencyMap(method);
		}
			
		if (efreqMap == null)
			System.out.println("Method: " + method.getName()
					+ ", EdgeFreqMap is null");
		else {
			// displayEdgeProbabilities (efreqMap);
		}
		System.out.println("Method frequecy of "+method.getSignature()+": "
				+_staticProfile.getMethodFrequency(method));
		// Check if the method has loops or not
		LoopInfo LI = _staticProfile.getMethodLoopInfo(method);
		boolean hasLoops = false;
		int Nloops = LI.loops();
		if ( Nloops > 0)
			hasLoops = true;
		// System.out.println("Number of blocks: " + blockTimes.size());
		//HashMap<ExecutionPath, Double> pathTimes = new HashMap<ExecutionPath, Double>();
		//ValueComparator bvc =  new ValueComparator(pathTimes);
		//TreeMap<Path,Double> sorted_map = new TreeMap<Path,Double>(bvc);
		//DEBUG
		/*Log.printPaths("******************* All Paths **********************\n");*/
		
		double methodTime = 0.0; // Cycles
		int numPaths = 1;
		double allPathProb = 0.0;
		for (ExecutionPath path : paths) {
			// Ignoring illegal paths
			/*if (numPaths++ > 500)
				break;*/

			double pathProbability = 0.0;
			double pathCycles = 0.0;
			double pathTime = 0.0;
			Collection<Block> nodes = path.blocks();
			Iterator<Block> first = nodes.iterator();
			Iterator<Block> second = nodes.iterator();
			// Move the second iterator to second element
			second.next();
			double edgeProbability = 1.0, edgeProb = 1.0;
			double edgeP = 1.0;
			while (first.hasNext() && second.hasNext()) {
				Block src = first.next();
				Block dst = second.next();
				Pair<Block, Block> edge = new Pair<Block, Block>(src, dst);
				edgeProb = edgeProbability;
				double blockTime = 0.0;
				// if (efreqMap.containsKey(edge)) {
				// edgeProbability = efreqMap.get(edge);
				if (efreqMap != null)
					edgeProbability = getEdgeFrequency(efreqMap, edge);
				
				if (eProbMap != null)
					edgeP = getEdgeFrequency(eProbMap, edge);
				
				/*Log.printBlocks(edge.first().toShortString() + "---->"
						+ edge.second().toShortString() + " : " + edgeP + ", "
						+ edgeProbability + "\n");*/

				/*if (edgeProbability > edgeProb)
					edgeProbability = edgeProb; */
				// }
				/*int sBound = 1, dBound = 1;
				for (Iterator<Loop> loopIter = LI.iterator(); loopIter.hasNext();) {
					Loop loop = loopIter.next();
					boolean containsSrc = loop.contains(src);
					//boolean containsDst = loop.contains(dst);
					int iBound = loop.getLoopBound();
					if (containsSrc && (iBound > 1))
						sBound *= iBound;
					if (containsDst && (iBound > 1))
						dBound *= iBound;
				}*/
				
				

				// Get the blockTime and add it to overall pathTime
				// if (blockTimes.containsKey(src)) {
				blockTime = getBlockTime(blockTimes, src);
				// }
				//for (int i = 0; i < sBound; i++) {
					pathCycles += blockTime;
					pathProbability += edgeProbability;
				//}
				
				// Track last node and add its time to total time
				if (!second.hasNext()) {
					blockTime = getBlockTime(blockTimes, dst);
					pathCycles += blockTime;
				}
			}

			// Case where there is only one node
			if (nodes.size() == 1) {
				/*
				 * System.out.println("Method: " + method.getName() +
				 * " has one block for this path");
				 */
				Block block = first.next();
				double blockTime = getBlockTime(blockTimes, block);
				pathCycles += blockTime;
			}

			/*
			 * if (pathProbability > 1) System.out.println ("");
			 */

			// Reset the path probability if zero or negative, error case
			if ((pathProbability == 0) || (pathProbability < 0)) {
				pathProbability = 1;
				// System.out.println("Probability error, check!!!");
			}

			// System.out.println (path.toString()+" : "+pathCycles);
			pathTime = pathProbability * pathCycles;
			methodTime += pathTime;
			allPathProb += pathProbability;
			
			//pathTimes.put(path, new Double(pathCycles));
			// pathProbabilities.put(path, pathProbability); //
			/*Log.printPaths(path.toString() + " : " + pathProbability + " : "+
			pathCycles + "\n");*/
		}

		// Effective methodtime
		//if (allPathProb < 1)
		methodTime = methodTime / allPathProb;
		double delta = 0.1 * methodTime;
		//DEBUG
		/*Log.printPaths("Method ACET: " + methodTime + ", 10% of ACET: " + delta + "\n");*/
		// Display the path times
		// System.out.println
		// ("*******************Various path times**********************");
		/*Log.printPaths("******************* ACET Paths **********************\n");*/
		/*
		for (Entry entry : pathTimes.entrySet()) {
			double time = ((Double) entry.getValue()).doubleValue();
					/// allPathProb;
			if ((time >= (methodTime - delta))
					&& ((time <= methodTime + delta)))
				Log.printPaths(entry.getKey().toString() + " : " + time + "\n");
		}*/
		
		/*sorted_map.putAll(pathTimes);
		double tenPercent = Math.ceil(paths.size()/10); 
		Iterator<Path> pathIter = sorted_map.descendingKeySet().iterator();
		for (int i = 0; i <= tenPercent; i++) {
			Path path = pathIter.next();
			double time = sorted_map.get(path).doubleValue();
			if (allPathProb < 1)
				time = time / allPathProb;
			Log.printPaths(path.toString() + " : " + time + "\n");
		}*/
		// return pathProbabilities;
		return methodTime;
	}

	double getBlockTime(HashMap<Block, Double> blockTimes, Block block) {
		for (Entry<Block, Double> entry : blockTimes.entrySet()) {
			if (entry.getKey().toString().equals(block.toString()))
				return entry.getValue().doubleValue();
		}
		return 0.0;
	}

	double getEdgeFrequency(
			HashMap<Pair<Block, Block>, Double> edgeFrequencies,
			Pair<Block, Block> edge) {
		for (Entry<Pair<Block, Block>, Double> entry : edgeFrequencies
				.entrySet()) {
			if (entry.getKey().toString().equals(edge.toString()))
				return entry.getValue();
		}
		return 1.0;
	}

	double getBlockFrequency(HashMap<Block, Double> bFreq, Block block) {
		for (Entry<Block, Double> entry : bFreq.entrySet()) {
			if (entry.getKey().toString().equals(block.toString()))
				return entry.getValue().doubleValue();
		}
		return 1.0;
	}

	/*
	 * String getMethodString (SootMethod method) { String methodString;
	 * methodString = method.getDeclaration(); String className =
	 * method.getDeclaringClass().toString(); String parts[] =
	 * methodString.split(" "); methodString = ""; for (String string : parts) {
	 * if (string.contains("(")) { String temp = className + "." + string;
	 * methodString += temp; } else { methodString += string + " "; } } return
	 * methodString; }
	 */

	/*
	 * public List<String> getInvokedMethods(String method) { List<String>
	 * invokedMethods = new ArrayList<String>(); while
	 * (!methodStack.peek().equals(method)) { String invokedMethod =
	 * methodStack.pop(); invokedMethods.add(invokedMethod); } // Pop the top to
	 * remove current method //methodStack.pop(); return invokedMethods; }
	 */

	public List<String> getConcreteMethods(List<String> allInvokedMethods,
			String hasName) {
		List<String> concreteMethods = new ArrayList<String>();
		for (String invokedMethod : allInvokedMethods) {
			if (invokedMethod.contains(hasName))
				concreteMethods.add(invokedMethod);
		}
		return concreteMethods;
	}

	public List<String> getSubClassMethods(
			/* List<String> invokedMethods, */SootMethod method) {
		List<String> subClassStrings = new ArrayList<String>();
		FastHierarchy hierarchy = Scene.v().getOrMakeFastHierarchy();
		if (hierarchy == null)
			return subClassStrings;

		Collection<SootClass> subClasses = hierarchy.getSubclassesOf(method
				.getDeclaringClass());

		for (SootClass subClass : subClasses) {
			String subClassName = subClass.getName();
			String subClassSignature = Method.getMethodString(method,
					subClassName);
			// if (invokedMethods.contains(subClassSignature))
			subClassStrings.add(subClassSignature);
		}

		return subClassStrings;
	}
	
	/*void collectSysInfo (SootMethod main) {
		JimpleBody b = (JimpleBody) main.retrieveActiveBody();
		ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
		if (b != null) {
			Iterator<Unit> unitIter = eug.iterator();
			while (unitIter.hasNext()) {
				Unit unit = unitIter.next();
				System.out.println(unit.toString());
				SootMethodRef mRef = null;
				if (unit instanceof JInvokeStmt) {
					JInvokeStmt invokeStmt = (JInvokeStmt) unit;
					InvokeExpr expr = invokeStmt.getInvokeExpr();
					if (expr instanceof JSpecialInvokeExpr) {
						JSpecialInvokeExpr invokeExpr = (JSpecialInvokeExpr) expr;
						mRef = invokeExpr.getMethodRef();
					} else if (expr instanceof JInterfaceInvokeExpr) {
						JInterfaceInvokeExpr invokeExpr = (JInterfaceInvokeExpr) expr;
						mRef = invokeExpr.getMethodRef();
					} else if (expr instanceof JStaticInvokeExpr) {
						JStaticInvokeExpr invokeExpr = (JStaticInvokeExpr) expr;
						mRef = invokeExpr.getMethodRef();
					} else if (expr instanceof JVirtualInvokeExpr) {
						JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) expr;
						mRef = invokeExpr.getMethodRef();
					}
				}
				
				if (mRef != null) {
					SootMethod method = mRef.resolve();
					System.out.println("Called method: "+method.toString()+
							" class: "+method.getDeclaringClass());
					//method.get
					if (method.getDeclaringClass().isApplicationClass()
							|| !method.getDeclaringClass().getPackageName()
									.startsWith("java.")) {
						
						modules.add(method.getDeclaringClass());
					}
					
					if (method.getDeclaringClass().isApplicationClass()
							&& method.getName().contains("run")) {
						threadModules.add(method.getDeclaringClass());
						System.out.println("Thread module: "+method.getDeclaringClass());
					}
				}
			}
		}
	}*/

	void process(SootMethod method) {
		if (!method.hasActiveBody())
			return;

		// RegionAnalysis and Depedence analysis
		// int methodRegionCount = 0;//regionAnalysis(method);
		//sideEffectAnalysis(method);
		// if (method.getName().equals("main"))	collectSysInfo(method);
		// Using path generator list all non-loop paths
		LoopInfo loopInfo = _staticProfile.getMethodLoopInfo(method);

		HashMap<Block, Double> blockFrequencies = null;
		// Get block frequency map
		if (_staticProfile != null)//Wouldn't this be redundant? If it is null, it should throw an error 2 commands up
			blockFrequencies = _staticProfile.getBlockFrequencyMap(method);
		// Baf (bytecode) graph construction
		BafBody body = new BafBody(method.retrieveActiveBody(), (Map) null);
		BriefBlockGraph c = new BriefBlockGraph(body);

		// Block (source code) graph construction
		JimpleBody b = (JimpleBody) method.retrieveActiveBody();
		BriefBlockGraph cGraph = new BriefBlockGraph(b);

		Iterator<Block> bafBlockIter = c.iterator();
		Iterator<Block> srcBlockIter = cGraph.iterator();
		/*
		HashMap<Block, Double> blockCycles = new HashMap<Block, Double>();*/
		HashMap<Block, Double> blockTimes = new HashMap<Block, Double>();/*
		HashMap<Block, Block> srcToBaf = new HashMap<Block, Block>();

		List<Block> methodbafBlocks = new ArrayList<Block>();
		List<Block> methodBlocks = new ArrayList<Block>();*/

		// Tracking method invokes
		List<String> invokedMethods = new ArrayList<String>();
		List<String> foundMethods = new ArrayList<String>();

		int methodLoop = 0;
		int libCalls = 0;

		String methodString = Method.getMethodString(method);
		// List<String> invokedMethodStrings = getInvokedMethods(methodString);

		/*Log.printBlocks("=========================" + methodString
				+ "=========================\n");*/
		//DEBUG
		/*
		Log.printPaths("=========================" + methodString + "has "
				+ pathGen.getPaths().size() + " paths"
				+ "=========================\n");*/

		long methodStmtCount = 0, methodBlockCount = 0;
		// Tracking how many method calls and how many of which are library calls
		int invokes = 0, libraryCalls = 0;
		while (bafBlockIter.hasNext() && srcBlockIter.hasNext()) {
			Block bafBlock = bafBlockIter.next();
			Block srcBlock = srcBlockIter.next();

			methodBlockCount++;

			/*methodbafBlocks.add(bafBlock);
			methodBlocks.add(srcBlock);
			srcToBaf.put(srcBlock, bafBlock);*/

			double blockCyles = 0;
			double blockTime = 0.0;

			// Get block frequency from static profile
			double blockFrequency = 1.0;

			if (blockFrequencies != null)
				blockFrequency = getBlockFrequency(blockFrequencies, srcBlock);

			for (Iterator<Unit> unitIter = bafBlock.iterator(); unitIter
					.hasNext();) {
				Unit unit = unitIter.next();
				methodStmtCount++;
				// Get the information about unit
				UnitInfo info = new UnitInfo(unit);
				double cycles = _interpreter.process_unit(info);
				blockCyles += cycles;

				// Method invocation has to be handled here...
				SootMethodRef mRef = null;
				double invokedMethodTime = 0.0;
				if (unit instanceof BSpecialInvokeInst) {
					BSpecialInvokeInst inst = (BSpecialInvokeInst) unit;
					mRef = inst.getMethodRef();
				} else if (unit instanceof BInterfaceInvokeInst) {
					BInterfaceInvokeInst inst = (BInterfaceInvokeInst) unit;
					mRef = inst.getMethodRef();
				} else if (unit instanceof BStaticInvokeInst) {
					BStaticInvokeInst inst = (BStaticInvokeInst) unit;
					mRef = inst.getMethodRef();
				} else if (unit instanceof BVirtualInvokeInst) {
					BVirtualInvokeInst inst = (BVirtualInvokeInst) unit;
					mRef = inst.getMethodRef();
				}

				if (mRef != null) {
					SootMethod m = mRef.resolve();
					if (m.getDeclaringClass().isApplicationClass()
							|| !m.getDeclaringClass().getPackageName()
									.startsWith("java.")) {
						invokes++;
						String methodS = Method.getMethodString(m);
						invokedMethods.add(methodS);
						int invokedMethodLoops = 0;
						if (_methodTime.containsKey(methodS)) {
							invokedMethodTime = _methodTime.get(methodS)
									.doubleValue();
							foundMethods.add(methodS + " = "
									+ invokedMethodTime);
							if (_methodLoops.containsKey(methodS))
								invokedMethodLoops = _methodLoops.get(methodS);
							System.out.println("Adding " + invokedMethodLoops
									+ " loops of " + methodS);
						} else if (m.getDeclaringClass().isAbstract()) {
							// Problem, do something quick
							/*
							 * String methodName = Method
							 * .getMethodWithOnlyNameAndParams(m);
							 */
							/*
							 * List<String> concreteMethods =
							 * getConcreteMethods( invokedMethodStrings,
							 * methodName);
							 */
							// Subclass methods which are present in invoked
							// method list
							List<String> subClassSignatures = getSubClassMethods(/* concreteMethods */m);
							double timeToAdd = 0.0;
							for (String subClass : subClassSignatures) {
								if (!_methodTime.containsKey(subClass))
									continue;
								timeToAdd = _methodTime.get(subClass)
										.doubleValue();
								if (timeToAdd > invokedMethodTime) {
									invokedMethodTime = timeToAdd;
									if (_methodLoops.containsKey(subClass)) {
										if (_methodLoops.containsKey(subClass))
											invokedMethodLoops = _methodLoops
												.get(subClass);
										System.out.println("Adding "
												+ invokedMethodLoops
												+ " loops of subclass " + subClass);
									}
								}
							}
							if (subClassSignatures.size() > 0)
								foundMethods.add("[Sub]"
										+ subClassSignatures.toString() + " = "
										+ invokedMethodTime);
						}
						methodLoop += invokedMethodLoops;
					} else {
						libCalls++;
					}
				}

				blockCyles += invokedMethodTime;
				// System.out.println
				// ("Unit = "+unit.toString()+"---------- Cycles = "+cycles);
			} // end of block units iterator

			blockTime = blockFrequency * blockCyles;
			//DEBUG
			/*Log.printBlocks(srcBlock.toString() + "----------" + blockTime
					+ "----------" + blockFrequency + "----------" + blockCyles
					+ "\n");*/

			//blockCycles.put(bafBlock, blockCyles);
			blockTimes.put(srcBlock, new Double(blockCyles)); // TODO:putting blockCyles instead of blockTime

		} // end of blocks iterator

		// Update maps accordingly
		//_methodBlocks.put(method, methodBlocks);
		//_methodBafBlocks.put(method, methodbafBlocks);
		//_srcTobaf.put(method, srcToBaf);
		//_methodBafBlockCycles.put(method, blockCycles);
		//_methodBlockTime.put(method, blockTimes);
		HashMap<Pair<Block, Block>, Double> efreqMap = null;
		if (_staticProfile != null) 
			efreqMap = _staticProfile.getMethodEdgeInformation(method);
		long startTime = System.currentTimeMillis();
		pathGen = new PathGenerator(method, loopInfo, blockTimes, efreqMap);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
	    System.out.println("Path generation time: "+elapsedTime);
		//_methodPaths.put(method, pathGen.getPaths());
		// System.out.println("Paths enumerated = " + pathGen.getPaths().size());
		
		// Print the statistics about the method
		if (invokes > 0)
			System.out
					.println("Total number of method invocations: " + invokes);
		System.out.println("Invoked methods: " + invokedMethods.size());//Has to be 0
		for (String invoked : invokedMethods) {
			System.out.println(invoked);
		}

		System.out.println("Found methods: " + foundMethods.size());
		for (String found : foundMethods) {
			System.out.println(found);
		}
		int loops = 0;
		if (loopInfo != null)
			loops = loopInfo.loops();
		System.out.println("Imported Loops: " + methodLoop
				+ " ---- Current method loop: " + loops);

		methodLoop += loops;

		_methodLoops.put(methodString, methodLoop);
		//startTime = System.currentTimeMillis();
		// Comupte Avg. method time and update it into method times map
		double methodTime = pathGen.getTime();//computeAverageMethodTime(method,pathGen.getPaths(), blockTimes);
		
		/*stopTime = System.currentTimeMillis();
		elapsedTime = stopTime - startTime;
	    System.out.println("ACET time: "+elapsedTime);*/
		System.out.println(methodString + ": Time: " + methodTime + " Cycles");
		//DEBUG
		/*Log.printTimes(method.getSignature() + "\t" + methodTime + "\t"
				+ methodLoop + "\t" + libCalls + "\n");*/
		_methodTime.put(methodString, methodTime);
		//DEBUG
		Log.printTimes(method.getSignature() + "\t" +methodStmtCount+ "\t" +
					methodLoop+"\n");
		/*Log.printMethodStmts(method.getSignature() + "\t" + methodRegionCount
				+ "\t" + methodBlockCount + "\t" + methodStmtCount + "\n");*/
		// Method methodObj = new Method(method, methodString, methodTime);
		// _nameToMethod.put(methodString, methodObj);

	}

	/*private int regionAnalysis(SootMethod m) {
		Log.printRegions("****************" + m.getName() + "*****************"
				+ "\n");
		// LoopInfo LI = new LoopInfo(m);
		JimpleBody b = (JimpleBody) m.retrieveActiveBody();
		BriefUnitGraph graph = new BriefUnitGraph(b);
		// ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
		// HashMutablePDG pdg = new HashMutablePDG(graph);
		RegionAnalysis regionAnalysis = new RegionAnalysis(graph, m, m
				.getDeclaringClass());
		List<Region> regions = regionAnalysis.getRegions();
		int regionCount = regions.size();
		Iterator<Region> regionIter = regions.iterator();
		while (regionIter.hasNext()) {
			Region region = regionIter.next();
			Log.printRegions("============= Region: " + region.getID()
					+ "=============" + "\n");
			Log.printRegions(region.getBlocks().toString());
			Log
					.printRegions("====================================================="
							+ "\n");
		}
		return regionCount;
	}

	private void sideEffectAnalysis(SootMethod m) {
		Log.printDependencies("****************" + m.getName()
				+ "*****************" + "\n");
		JimpleBody body = (JimpleBody) m.retrieveActiveBody();

		// Body body = m.retrieveActiveBody();
		 * System.out.println("=======================================");
		 * System.out.println(body.toString());
		 * System.out.println("=======================================");
		 
		// DumbPointerAnalysis.v()
		SideEffectAnalysis sea = new SideEffectAnalysis(
				DumbPointerAnalysis.v(), Scene.v().getCallGraph());
		sea.findNTRWSets(body.getMethod());

		HashMap stmtToReadSet = new HashMap();
		HashMap stmtToWriteSet = new HashMap();

		HashMap<RWSet, Object> rwToStmt = new HashMap<RWSet, Object>();
		// ArrayList<RWSet> sets = new ArrayList<RWSet>();
		// SideEffectAnalysisTest seaTest = new SideEffectAnalysisTest();
		Analysis analysis = new Analysis();
		UniqueRWSets sets = analysis.new UniqueRWSets();
		for (Iterator stmtIt = body.getUnits().iterator(); stmtIt.hasNext();) {
			final Stmt stmt = (Stmt) stmtIt.next();
			Object key = stmt.toString();
			// RWSet rwSet = sea.readSet(m, stmt);
			if (!stmtToReadSet.containsKey(key)) {
				RWSet rwSet = sets.getUnique(sea
						.readSet(body.getMethod(), stmt));
				stmtToReadSet.put(key, rwSet);
				rwToStmt.put(rwSet, key);
			}
			// rwSet = sea.writeSet(m, stmt);
			if (!stmtToWriteSet.containsKey(key)) {
				RWSet rwSet = sets.getUnique(sea.writeSet(body.getMethod(),
						stmt));
				stmtToWriteSet.put(key, rwSet);
				rwToStmt.put(rwSet, key);
			}

		}

		for (Iterator outerIt = sets.iterator(); outerIt.hasNext();) {
			final RWSet outer = (RWSet) outerIt.next();

			for (Iterator innerIt = sets.iterator(); innerIt.hasNext();) {

				final RWSet inner = (RWSet) innerIt.next();
				if (inner == outer)
					break;
				if (outer.hasNonEmptyIntersection(inner)) {
					// System.out.println(outer.toString()+"---------"+inner.toString());
					Log.printDependencies(rwToStmt.get(outer) + "--->"
							+ rwToStmt.get(inner) + "\n");
				}
			}
		}
		Log
				.printDependencies("============================================================");
	}*/
	
	private void markACEPMethods () {
		double parentMethodTime = 0.0, methodTime = 0.0;
		int parentLoops = 0, loops = 0;
		markedACEPMethods.add(_MAIN);
		// Start with the main function.
		SootMethod root = _CG_MAIN.getFunction();
		List<SootMethod> successors = _CG_MAIN.buildDFSFunctionTree();
		
		System.out
				.println("=================================================================timingAnalysis=======================================================================");
		// If main has no successors, i.e., calls no other functions.
		if (root == null || successors.size() == 0)
			return;

		// Successor is the method itself then return
		/*
		 * if ((successors.size() == 1) && successors.get(0).equals(root))
		 * return;
		 */

		// Auxiliary data structures.
		Set<SootMethod> visited = new HashSet<SootMethod>();
		Set<SootMethod> inStack = new HashSet<SootMethod>();
		Stack<SootMethod> visitStack = new Stack<SootMethod>();

		// Initiallize the data structures
		visited.add(root);
		inStack.add(root);
		visitStack.push(root);

		int methodCount = 0;
		SootMethod current = null;
		do {
			SootMethod method = visitStack.peek();
			// Get the current method time
			String parentMethodString = Method.getMethodString(method);
			parentMethodTime = _methodTime.get(parentMethodString);
			double fiftyPercentOfParent = 0.5 * parentMethodTime;
			parentLoops = _methodLoops.get(parentMethodString);
			double fiftyPercent = 0.5 * parentLoops;
			
			CallGraphDFS node = new CallGraphDFS(method);

			// methodStack.add(Method.getMethodString(method));

			boolean FoundNew = false;

			// Search function successors.
			List<SootMethod> succs = node.buildDFSFunctionTree();

			for (Iterator<SootMethod> succIter = succs.iterator(); succIter
					.hasNext();) {
				current = succIter.next();

				// If the successor is method itself then do nothing
				if (current.toString().equals(method.toString()))
					continue;

				// Try to insert the function into the visit list.
				// In case of success, a new function was found.
				if (!visited.contains(current)) {
					String methodString = Method.getMethodString(current);
					if (current.getName().equals("<init>")
							&& current.getDeclaringClass().getName().equals(
									"org.apache.lucene.index.MultiSegmentReader"))
						continue;
					if (current.getName().equals("processCommandLine")
							&& current.getDeclaringClass().getName().equals(
									"org.eclipse.core.runtime.adaptor.EclipseStarter"))
						continue;
					methodTime = _methodTime.get(methodString);
					loops = _methodLoops.get(methodString);
					int succCount = getSuccessorCount(current);
					// if (succCount == 0)	succCount = 1;
					double loopProportion = parentLoops;
					if (succCount != 0)
						loopProportion = (double) parentLoops / succCount;
					
					if (methodTime >= fiftyPercentOfParent) {
						if ((loops >= fiftyPercent)
								|| (loops >= loopProportion)) {
							if (loops > 1) {
								markedACEPMethods.add(current);
								System.out.println("[ACEP] method: "
										+ current.getName() + " ("
										+ current.getDeclaringClass() + ")"
										+ "loops: " + loops + " fiftyPercent: "
										+ fiftyPercent + " loopProportion: "
										+ loopProportion + " #succs: "
										+ succCount);
							}
						}
					} else if ((loops >= fiftyPercent)
							|| (loops >= loopProportion)) {
						if (loops > 1) {
							markedACEPMethods.add(current);
							System.out.println("[ACEP-loops] method: "
									+ current.getName() + " ("
									+ current.getDeclaringClass() + ")"
									+ "loops: " + loops + " fiftyPercent: "
									+ fiftyPercent + " loopProportion: "
									+ loopProportion + " #succs: " + succCount);
						}
					}
					visited.add(current);
					FoundNew = true;
					break;
				}

				// If successor is in VisitStack, it is a back edge.
				if (inStack.contains(current)) {
					// Indication of loop or recurrence. Don't do anything now
					// TODO: Visit later, if timing is not accurate enough
				}
			}

			if (!FoundNew) {
				// Obtain the function without new successors.
				current = visitStack.pop();
				
				/*
				if (markedACEPMethods.contains(current)) {
					// Generate intra-procedural cfgs if the method is along ACEP
					JimpleBody b = (JimpleBody) current.retrieveActiveBody();
					CompleteBlockGraph cGraph = new CompleteBlockGraph(b);
					CFGToDotGraph graph = new CFGToDotGraph();
					DotGraph dotGraph = graph.drawCFG(cGraph, b);
					dotGraph.plot(current.getSignature()+".dot");
					
					// do region and sideeffect analysis for finding the dependencies
					/*int regions = regionAnalysis(current);
					sideEffectAnalysis(current);*/
				//}
				
				inStack.remove(current);
			} else {
				// Found a new function.
				// Go down one level if there is a unvisited successor.
				inStack.add(current);
				visitStack.push(current);
			}

		} while (!visitStack.empty());
	}
	
	private final int getSuccessorCount (SootMethod method) {
		CallGraph cg = Scene.v().getCallGraph();
		Iterator<Edge> outEdges = cg.edgesOutOf(method);//succs.size() - 1;
		int succCount = 0;//outEdges.
		while (outEdges.hasNext()){
			Edge e = outEdges.next();
			SootMethod tgt = e.tgt();
			// Avoid possible recursion
			if (tgt.equals(method))	continue;
			boolean isValid = false;
			isValid = tgt.getDeclaringClass().isApplicationClass();
			if (isValid)	succCount++;
		}
		return succCount;
	}

	private void timingAnalysis() {
		// Start with the main function.
		SootMethod root = _CG_MAIN.getFunction();
		List<SootMethod> successors = _CG_MAIN.buildDFSFunctionTree();

		System.out
				.println("=================================================================timingAnalysis=======================================================================");
		// If main has no successors, i.e., calls no other functions.
		if (root == null || successors.size() == 0)
			return;

		// Successor is the method itself then return
		/*
		 * if ((successors.size() == 1) && successors.get(0).equals(root))
		 * return;
		 */

		// Auxiliary data structures.
		Set<SootMethod> visited = new HashSet<SootMethod>();
		Set<SootMethod> inStack = new HashSet<SootMethod>();
		Stack<SootMethod> visitStack = new Stack<SootMethod>();

		// Initiallize the data structures
		visited.add(root);
		inStack.add(root);
		visitStack.push(root);

		int methodCount = 0;
		SootMethod current = null;
		do {
			SootMethod method = visitStack.peek();
			CallGraphDFS node = new CallGraphDFS(method);

			System.out.println("[T] method: " + method.getName() + " ("
					+ method.getDeclaringClass() + ")");

			// methodStack.add(Method.getMethodString(method));

			boolean FoundNew = false;

			// Search function successors.
			List<SootMethod> succs = node.buildDFSFunctionTree();

			for (Iterator<SootMethod> succIter = succs.iterator(); succIter
					.hasNext();) {
				current = succIter.next();

				// If the successor is method itself then do nothing
				if (current.toString().equals(method.toString()))
					continue;

				// If successor is library method then do not process
				/*
				 * if (!current.getDeclaringClass().isApplicationClass() &&
				 * !(current.getDeclaringClass().getPackageName()
				 * .startsWith("org.apache.lucene") || current
				 * .getDeclaringClass().getPackageName()
				 * .startsWith("org.dacapo"))) { SootMethod libraryMethod =
				 * visitStack.pop(); _library.add(libraryMethod); continue; }
				 */

				// Try to insert the function into the visit list.
				// In case of success, a new function was found.
				if (!visited.contains(current)) {
					visited.add(current);
					FoundNew = true;
					break;
				}

				// If successor is in VisitStack, it is a back edge.
				if (inStack.contains(current)) {
					// Indication of loop or recurrence. Don't do anything now
					// TODO: Visit later, if timing is not accurate enough
				}
			}

			if (!FoundNew) {
				// Obtain the function without new successors.
				current = visitStack.pop();
				methodCount++;
				System.out.println("[T-" + methodCount + "] Processing "
						+ current.getName() + " ("
						+ current.getDeclaringClass().getName() + ")");

				/*
				 * if (current.getName().equals("rehashPostings")) {
				 * inStack.remove(current); continue; }
				 */
				if (current.getName().equals("<init>")
						&& current.getDeclaringClass().getName().equals(
								"org.apache.lucene.index.MultiSegmentReader"))
					continue;
				
				if (current.getName().equals("processCommandLine")
						&& current.getDeclaringClass().getName().equals(
								"org.eclipse.core.runtime.adaptor.EclipseStarter"))
					continue;

				CallGraphDFS CGnode = new CallGraphDFS(current);
				process(current);
				inStack.remove(current);
			} else {
				// Found a new function.
				// Go down one level if there is a unvisited successor.
				inStack.add(current);
				visitStack.push(current);
			}

		} while (!visitStack.empty());

	}

	public class UniqueRWSets {
		public ArrayList<RWSet> l = new ArrayList();

		RWSet getUnique(RWSet s) {
			if (s == null)
				return s;
			for (Iterator retIt = l.iterator(); retIt.hasNext();) {
				final RWSet ret = (RWSet) retIt.next();
				if (ret.isEquivTo(s))
					return ret;
			}
			l.add(s);
			return s;
		}

		Iterator iterator() {
			return l.iterator();
		}

		short indexOf(RWSet s) {
			short i = 0;
			for (Iterator retIt = l.iterator(); retIt.hasNext();) {
				final RWSet ret = (RWSet) retIt.next();
				if (ret.isEquivTo(s))
					return i;
				i++;
			}
			return -1;
		}
	}
}

class ValueComparator implements Comparator<Path> {

    Map<Path, Double> base;
    public ValueComparator(Map<Path, Double> base) {
        this.base = base;
    }

    public int compare(Path a, Path b) {
        return base.get(a).compareTo(base.get(b));
    }
}