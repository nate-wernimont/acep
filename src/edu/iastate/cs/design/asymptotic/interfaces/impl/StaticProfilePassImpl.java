package edu.iastate.cs.design.asymptotic.interfaces.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

//import com.sun.org.apache.bcel.internal.generic.FNEG;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.EdgeWeight;
import edu.iastate.cs.design.asymptotic.datastructures.Log;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Module;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.interfaces.BlockEdgeFrequencyPass;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionInfo;
import edu.iastate.cs.design.asymptotic.interfaces.StaticProfilePass;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.toolkits.graph.Block;

/**
 * This pass staticly estimates execution counts for blocks, edges an function
 * calls invocations in compilation time. This algorithm is slightly modified
 * from the BlockEdgeFrequencyPass to calculate cyclic_frequencies for functions
 * in a call graph.
 * 
 * References: Youfeng Wu and James R. Larus. Static branch frequency and
 * program profile analysis. In MICRO 27: Proceedings of the 27th annual
 * international symposium on Microarchitecture. IEEE, 1994.
 * 
 * @author gupadhyaya
 * 
 */
public class StaticProfilePassImpl implements StaticProfilePass {

	CallGraphDFS CG;
	static double epsilon = 0.000001;

	// Required pass to identify loop in functions.
	//LoopInfo LI;
	
	HashMap<SootMethod, LoopInfo> methodLoopInfo = new HashMap<SootMethod, LoopInfo>();
	HashMap<SootMethod, BranchPredictionInfo> methodBPIMap = new HashMap<SootMethod, BranchPredictionInfo>();

	HashMap<Pair<SootMethod, SootMethod>, Double> LocalEdgeFrequency = new HashMap<Pair<SootMethod, SootMethod>, Double>();
	HashMap<Pair<SootMethod, SootMethod>, Double> GlobalEdgeFrequency = new HashMap<Pair<SootMethod, SootMethod>, Double>();
	HashMap<Pair<SootMethod, SootMethod>, Double> BackEdgeFrequency = new HashMap<Pair<SootMethod, SootMethod>, Double>();

	HashMap<SootMethod, Set<SootMethod>> Predecessors = new HashMap<SootMethod, Set<SootMethod>>();

	Set<Pair<SootMethod, SootMethod>> FunctionBackEdges = new HashSet<Pair<SootMethod, SootMethod>>();
	Set<SootMethod> FunctionLoopHeads = new HashSet<SootMethod>();
	Set<SootMethod> NotVisited = new HashSet<SootMethod>();

	HashMap<SootMethod, Double> FunctionInformation = new HashMap<SootMethod, Double>();
	// HashMap<SootMethod, Set<EdgeWeight>> EdgeInformation = new
	// HashMap<SootMethod, Set<EdgeWeight>>();
	HashMap<SootMethod, HashMap<Pair<Block, Block>, Double>> EdgeInformation = new HashMap<SootMethod, HashMap<Pair<Block, Block>, Double>>();
	HashMap<SootMethod, HashMap<Block, Double>> BlockInformation = new HashMap<SootMethod, HashMap<Block, Double>>();
	
	HashMap<SootMethod, HashMap<Pair<Block, Block>, Double>> EdgeProbabilities = new HashMap<SootMethod, HashMap<Pair<Block, Block>, Double>>();

	// Remove this later
	Module _module;

	Vector<SootMethod> DFS = new Vector<SootMethod>();

	SootMethod main;

	public StaticProfilePassImpl() {
		// Get the class on which you want to profile
		Module m = new Module("permute");
		m.init();
		_module = m;
		CG = new CallGraphDFS(m.main());
		runOnModule(m);
		System.out.println("Let's print some results now.....");
	}
	
	public StaticProfilePassImpl(String module) {
		// Get the class on which you want to profile
		Module m = new Module(module);
		m.init();
		_module = m;
		CG = new CallGraphDFS(m.main());
		runOnModule(m);
		// System.out.println("Let's print some results now.....");
	}

	public StaticProfilePassImpl(CallGraphDFS dfs, SootMethod main) {
		CG = dfs;
		this.main = main;
		Module m = new Module(main.getName());
		List<SootMethod> methods = CG.buildDFSFunctionTree();
		m.setMethods(methods);
		m.setMain(this.main);
		_module = m;
		runOnModule(m);
	}

	public void print() {

	}

	public Module getLoadedModule() {
		return _module;
	}

	// Methods
	// Testing
	public boolean runOnModule(Module m) {
		System.out.println("[ENTRY] runOnModule");
		// LI = new LoopInfo(method);
		// // Create callgraph instance for the current method
		// CG = new CallGraphDFS(method);
		// Calculate necessary information before processing.
		preprocess();
		ListIterator<SootMethod> methodIter = DFS.listIterator();
		System.out.println("Total FunctionLoopHeads := "+FunctionLoopHeads.size());
		System.out.println("Total DFS nodes: "+DFS.size());
		/*while (methodIter.hasNext())
			methodIter.next();*/
		// Search for function loop heads in reverse depth-first order.
		while (methodIter.hasNext()/*hasPrevious()*/) {
			SootMethod method = methodIter.next();
			// If function is a loop head, propagate frequencies from it.
			if (FunctionLoopHeads.contains(method)) {
				// Mark all reachable nodes as not visited.
				markReachable(method);
				// Propagate call frequency starting from this loop head.
				propagateCallFrequency(method, false);
			}
		}

		// Release some unused memory.
		DFS.clear();
		//FunctionLoopHeads.clear();

		CallGraphDFS root = null;

		if (m != null) {
			// Obtain the main function.
			root = new CallGraphDFS(m.main());
		} else {
			root = new CallGraphDFS(main);
		}
		// Mark all functions reachable from the main function as not visited.
		markReachable(root.getFunction());

		// Propagate frequency starting from the main function.
		propagateCallFrequency(root.getFunction(), true);

		// With function frequency calculated, propagate it to block and edge
		// frequencies to achieve global block and edge frequency.
		//calculateGlobalInfo(m);
		System.out.println("[EXIT] runOnModule");
		return false;
	}

	/**
	 * Preprocess - From a call graph: (1) obtain functions in depth-first
	 * order; (2) find back edges; (3) find loop heads; (4) local block and edge
	 * profile information (per function); (5) local function edge frequency;
	 * (6) map of function predecessors.
	 */
	void preprocess() {
		System.out.println("[ENTRY] preprocess");
		// Start with the main function.
		SootMethod root = CG.getFunction();
		List<SootMethod> successors = CG.buildDFSFunctionTree();

		// If main has no successors, i.e., calls no other functions.
		if (root == null || successors.size() == 0)
			return;

		// Successor is the method itself then return
		/*if ((successors.size() == 1) && successors.get(0).equals(root))
			return;*/

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

			System.out.println("[S] method: " + method.getName() + " ("
					+ method.getDeclaringClass() + ")");
			// Use when found a successor not knew before.
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
					visited.add(current);
					FoundNew = true;
					break;
				}

				// If successor is in VisitStack, it is a back edge.
				if (inStack.contains(current)) {
					// Save the function back edge.
					FunctionBackEdges.add(new Pair<SootMethod, SootMethod>(
							method, current));
					// Consider a loop head the function pointing by this back
					// edge.
					FunctionLoopHeads.add(current);
				}
			}
			// System.out.println ("OK1");
			// Found no new function, process it.
			if (!FoundNew) {
				// Obtain the function without new successors.
				current = visitStack.pop();
				methodCount++;
				System.out.println("[S-"+methodCount+"] Processing " + current.getName() + " ("
						+ current.getDeclaringClass().getName() + ")");
				
				if (current.getName().equals("<init>")
						&& current.getDeclaringClass().getName().equals(
								"org.apache.lucene.index.MultiSegmentReader"))
					continue;
				
				if (current.getName().equals("parseVersionInt")
						&& current.getDeclaringClass().getName().equals(
								"org.eclipse.osgi.framework.internal.core.Framework"))
					continue;
				
				if (current.getName().equals("processDelta")
						&& current.getDeclaringClass().getName().equals(
								"org.eclipse.osgi.framework.internal.core.PackageAdminImpl"))
					continue;
				
				if (current.getName().equals("main")
						&& current.getDeclaringClass().getName().equals(
								"org.apache.xalan.xslt.Process"))
					continue;
				
				if (current.getName().equals("processCommandLine")
						&& current.getDeclaringClass().getName().equals(
								"org.eclipse.core.runtime.adaptor.EclipseStarter"))
					continue;
				
				//Log.print(current.getSignature()+"\n");
				/*if (current.getName().equals("getThreadState")) {
					System.out.println();
				}*/
				CallGraphDFS CGnode = new CallGraphDFS(current);
				// Save this function ordering position (in depth-first order).
				DFS.add(current);
				// Only process if it has a function body.
				if (current.hasActiveBody()) {
					// Calculate local block and edge frequencies.
					BlockEdgeFrequencyPass BEFP = new BlockEdgeFrequencyPassImpl(
							current);
					// Plain edge probabilities
					EdgeProbabilities.put(current, BEFP.getEdgeProbabilities());
					
					// Find all block frequencies.
					for (Iterator<Block> blockIter = BEFP.getBlockFreqIter(); blockIter
							.hasNext();) {
						Block blk = blockIter.next();
						if (!BlockInformation.containsKey(current)) {
							BlockInformation.put(current,
									new HashMap<Block, Double>());
						}
						BlockInformation.get(current).put(blk,
								BEFP.getBlockFrequency(blk));
					}
					// Find all edge frequencies.
					for (Iterator<Pair<Block, Block>> edgeIter = BEFP
							.getEdgeFreqIter(); edgeIter.hasNext();) {
						Pair<Block, Block> edge = edgeIter.next();
						if (!EdgeInformation.containsKey(current)) {
							EdgeInformation.put(current,
									new HashMap<Pair<Block, Block>, Double>());
						}
						EdgeInformation.get(current).put(edge,
								BEFP.getEdgeFrequency(edge));
					}
					//System.out.println("OK3");
					// Update call information.
					updateCallInfo(current, BEFP);
				}

				inStack.remove(current);
				
				// Special check to make sure looping exit
				if (current.toString().equals(root.toString()))	
					return;

			} else {
				// Found a new function.
				// Go down one level if there is a unvisited successor.
				inStack.add(current);
				visitStack.push(current);
			}

			//System.out.println("OK4");

		} while (!visitStack.empty());
		System.out.println("[EXIT] preprocess");
	}

	/**
	 * UpdateCallInfo - Calculates local function edges (function invocations)
	 * and a map of function predecessors.
	 * 
	 * @param method
	 * @param BEFP
	 */

	void updateCallInfo(SootMethod method, BlockEdgeFrequencyPass BEFP) {
		System.out.println("[ENTRY] updateCallInfo");
		// Search for function invocations inside basic blocks.
		LoopInfo LI;
		LI = BEFP.getLoopInfo();
		methodLoopInfo.put(method, LI);
		methodBPIMap.put(method, BEFP.getBPP().getBPI());
		Iterator<Block> blockIter = LI.getblockIter();
		while (blockIter.hasNext()) {
			Block block = blockIter.next();
			double bfreq = BEFP.getBlockFrequency(block);
			SootMethod called = null;
			// Run over through all basic block searching for call instructions.
			// Unit nextUnit = units.next();
			Iterator<Unit> blkUnitsIter = block.iterator();
			while (blkUnitsIter.hasNext()) {
				Unit unit = blkUnitsIter.next();
				for (ValueBox vb : (List<ValueBox>) unit.getUseAndDefBoxes()) {
					Value v = vb.getValue();
					// Reset called value
					called = null;
					// Needs to check if something more has to be done here
					if (v instanceof InvokeExpr) {
						called = ((InvokeExpr) v).getMethod();
					}

					Pair<SootMethod, SootMethod> fedge;
					if (called != null) {
						fedge = new Pair<SootMethod, SootMethod>(method, called);
						// The local edge frequency is the sum of block
						// frequency from
						// all
						// blocks that calls another function.
						if (!LocalEdgeFrequency.containsKey(fedge)) {
							LocalEdgeFrequency.put(fedge, bfreq);
						} else {
							LocalEdgeFrequency.put(fedge, LocalEdgeFrequency
									.get(fedge)
									+ bfreq);
						}

						// Define the predecessor of this function.
						if (!Predecessors.containsKey(called))
							Predecessors.put(called, new HashSet<SootMethod>());

						Predecessors.get(called).add(method);
						// Predecessors.put(called, method);
					}
				}
			}
		}
		System.out.println("[EXIT] updateCallInfo");
	}

	/**
	 * MarkReachable - Mark all blocks reachable from root function as not
	 * reachable
	 */
	void markReachable(SootMethod root) {
		System.out.println("[ENTRY] markReachable: "+root.toString());
		// Clear the list first.
		NotVisited.clear();
		// Use a stack to search function successors.
		Stack<CallGraphDFS> stack = new Stack<CallGraphDFS>();
		stack.push(new CallGraphDFS(root));
		while (!stack.empty()) {
			// Retrieve a function from the stack to process.
			CallGraphDFS node = stack.pop();
			SootMethod method = node.getFunction();
			// If it is already added to the not visited list, continue.
			if (NotVisited.contains(method)) {
				continue;
			}
			NotVisited.add(method);
			// Should only process function with a body.
			if (!method.hasActiveBody()) {
				continue;
			}
			// Add successors to the stack for future processing.
			List<SootMethod> methodList = node.buildDFSFunctionTree();
			for (Iterator<SootMethod> methodIter = methodList.iterator(); methodIter
					.hasNext();) {
				SootMethod successor = methodIter.next();
				if (successor.equals(node.getFunction()))
					continue;
				stack.push(new CallGraphDFS(successor));
			}
		}
		System.out.println("[EXIT] markReachable");
	}

	/**
	 * PropagateCallFrequency - Calculate function call and invocation
	 * frequencies.
	 * 
	 * @param root
	 * @param end
	 */

	void propagateCallFrequency(SootMethod root, boolean end) {
		System.out.println("[ENTRY] propagateCallFrequency: "+root.toString());
		SootMethod head = root;
		Stack<CallGraphDFS> stack = new Stack<CallGraphDFS>();
		stack.push(new CallGraphDFS(root));
		do {
			CallGraphDFS node = stack.pop();
			SootMethod method = node.getFunction();

			// Check if already visited function.
			if (!NotVisited.contains(method)) {
				continue;
			}

			boolean invalidEdge = false;
			// Run over all predecessors of this function.
			Set<SootMethod> predSet = predecessors(method);
			for (Iterator<SootMethod> predIter = predSet.iterator(); predIter
					.hasNext();) {
				SootMethod pred = predIter.next();
				Pair<SootMethod, SootMethod> fedge = new Pair<SootMethod, SootMethod>(
						pred, method);
				// Check if we have calculated all predecessors edge previously.
				if (NotVisited.contains(pred)
						&& !FunctionBackEdges.contains(fedge)) {
					invalidEdge = true;
					break;
				}
			}
			// There is an unprocessed predecessor edge.
			if (invalidEdge)
				continue;
			// Calculate all incoming edges frequencies and cyclic_frequency for
			// loops
			double cfreq = (method == head) ? 1.0 : 0.0;
			double cyclic_frequency = 0.0;

			for (Iterator<SootMethod> predIter = predSet.iterator(); predIter
					.hasNext();) {
				SootMethod pred = predIter.next();
				Pair<SootMethod, SootMethod> fedge = new Pair<SootMethod, SootMethod>(
						pred, method);

				// Is the edge a back edge.
				boolean backedge = FunctionBackEdges.contains(fedge);
				// Consider the cyclic_frequency only in the last call to
				// propagate freqency
				if (end && backedge)
					cyclic_frequency += getBackEdgeFrequency(fedge);
				else if (!backedge)
					cfreq += getGlobalEdgeFrequency(fedge);
			}

			// For loops that seems not to terminate, the cyclic frequency can
			// be
			// higher than 1.0. In this case, limit the cyclic frequency below
			// 1.0.
			if (cyclic_frequency > (1.0 - epsilon))
				cyclic_frequency = 1.0 - epsilon;

			// Calculate invocation frequency.
			cfreq = (float) cfreq / (1.0 - cyclic_frequency);
			FunctionInformation.put(method, cfreq);
			// Need to update FunctionInformation for Global frequency estimate,
			// will do it later

			// Mark the function as visited.
			NotVisited.remove(method);

			// Do not process successors for function without a body.
			if (!method.hasActiveBody()) {
				continue;
			}

			// Calculate global function edge invocation frequency for
			// successors
			// Add successors to the stack for future processing.
			List<SootMethod> methodList = node.buildDFSFunctionTree();
			for (Iterator<SootMethod> methodIter = methodList.iterator(); methodIter
					.hasNext();) {
				SootMethod successor = methodIter.next();

				if (successor.equals(node.getFunction()))
					continue;

				Pair<SootMethod, SootMethod> fedge = new Pair<SootMethod, SootMethod>(
						method, successor);
				// Calculate the global frequency for this function edge.
				double gfreq = getLocalEdgeFrequency(fedge) * cfreq;
				GlobalEdgeFrequency.put(fedge, gfreq);

				// Update back edge frequency in case of loop.
				if (!end && successor == head)
					BackEdgeFrequency.put(fedge, gfreq);
			}

			// Call propagate call frequency for function edges that are not
			// back edges.
			Vector<SootMethod> backedges = new Vector<SootMethod>();
			for (Iterator<SootMethod> methodIter = methodList.iterator(); methodIter
					.hasNext();) {
				SootMethod successor = methodIter.next();
				// Check if it is a back edge.
				if (!FunctionBackEdges
						.contains(new Pair<SootMethod, SootMethod>(method,
								successor)))
					backedges.add(successor);
			}

			// This was done just to ensure that the algorithm would process the
			// left-most child before, in order to simulate normal
			// PropagateCallFreq
			// recursive calls
			// TO-DO: you need to find a way to reverse the methods in
			// backedges.
			// List<SootMethod> someList = new ArrayList<SootMethod>();

			Collections.reverse(backedges);

			for (Iterator<SootMethod> methodIter = backedges.iterator(); methodIter
					.hasNext();) {
				SootMethod meth = methodIter.next();
				stack.push(new CallGraphDFS(meth));
			}

		} while (!stack.empty());
		System.out.println("[EXIT] propagateCallFrequency");
	}

	/**
	 * CalculateGlobalInfo - With calculated function frequency, recalculate
	 * block and edge frequencies taking it into consideration.
	 * 
	 * @param m
	 */

	void calculateGlobalInfo(Module m) {
		System.out.println("[ENTRY] calculateGlobalInfo");
		Iterator<SootMethod> moduleIter = m.iterator();
		while (moduleIter.hasNext()) {
			SootMethod method = moduleIter.next();
			// Obtain call frequency.
			double cfreq = getMethodFrequency(method);
			// Update edge frequency considering function call frequency.
			HashMap<Pair<Block, Block>, Double> edgeWts = getMethodEdgeInformation(method);

			for (Iterator<Pair<Block, Block>> edgeIter = edgeWts.keySet()
					.iterator(); edgeIter.hasNext();) {
				Pair<Block, Block> edge = edgeIter.next();
				Double curVal = edgeWts.get(edge);
				edgeWts.put(edge, curVal.doubleValue() * cfreq);
			}
			// EdgeInformation.put(method, updated);
		}
		System.out.println("[EXIT] calculateGlobalInfo");
	}

	public double getMethodBlockFrequency(SootMethod method, Block block) {
		HashMap<Block, Double> bfreqMap = null;
		if (BlockInformation.containsKey(method))
			bfreqMap = BlockInformation.get(method);
		if (bfreqMap == null)
			return 1.0;

		for (Block blk : bfreqMap.keySet()) {
			if (block.toString().equals(blk.toString()))
				return bfreqMap.get(blk).doubleValue();
		}
		return 1.0;
	}

	public HashMap<Block, Double> getBlockFrequencyMap(SootMethod method) {
		HashMap<Block, Double> bfreqMap = null;
		if (BlockInformation.containsKey(method))
			bfreqMap = BlockInformation.get(method);
		return bfreqMap;
	}

	public HashMap<Pair<Block, Block>, Double> getEdgeFrequencyMap(
			SootMethod method) {
		HashMap<Pair<Block, Block>, Double> efreqMap = null;
		if (EdgeProbabilities.containsKey(method))
			efreqMap = EdgeProbabilities.get(method);
		return efreqMap;
	}

	/**
	 * getBackEdgeFrequency - Get updated back edges frequency. In case of not
	 * found, use the local edge frequency.
	 * 
	 * @param fedge
	 * @return
	 */

	double getBackEdgeFrequency(Pair<SootMethod, SootMethod> fedge) {
		if (BackEdgeFrequency.containsKey(fedge))
			return BackEdgeFrequency.get(fedge);
		return getLocalEdgeFrequency(fedge);
	}
	
	double getLocalEdgeFrequency(Pair<SootMethod, SootMethod> fedge) {
		if (LocalEdgeFrequency.containsKey(fedge))
			return LocalEdgeFrequency.get(fedge);
		return 0.0;
	}

	/**
	 * getGlobalEdgeFrequency - Get updated global edge frequency. In case of
	 * not found, use the local edge frequency.
	 * 
	 * @param fedge
	 * @return
	 */

	double getGlobalEdgeFrequency(Pair<SootMethod, SootMethod> fedge) {
		if (GlobalEdgeFrequency.containsKey(fedge))
			return GlobalEdgeFrequency.get(fedge);
		return getLocalEdgeFrequency(fedge);
	}

	/**
	 * 
	 * @param method
	 * @return
	 */
	double getMethodFrequency(SootMethod method) {
		if (FunctionInformation.containsKey(method)) {
			return FunctionInformation.get(method);
		}
		return 1.0;
	}

	/**
	 * 
	 * @param method
	 * @return
	 */
	HashMap<Pair<Block, Block>, Double> getMethodEdgeInformation(
			SootMethod method) {
		if (EdgeInformation.containsKey(method)) {
			return EdgeInformation.get(method);
		}
		return new HashMap<Pair<Block, Block>, Double>();
	}

	/**
	 * 
	 * @param method
	 * @return
	 */
	private Set<SootMethod> predecessors(SootMethod method) {
		if (Predecessors.containsKey(method))
			return Predecessors.get(method);
		return new HashSet<SootMethod>();
	}
	
	public LoopInfo getMethodLoopInfo (SootMethod method) {
		return methodLoopInfo.get(method);
	}
	
	public BranchPredictionInfo getMethodBPI (SootMethod method) {
		return methodBPIMap.get(method);
	}
}
