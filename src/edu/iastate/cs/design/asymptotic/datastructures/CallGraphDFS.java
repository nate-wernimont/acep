package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphEdge;
import soot.util.dot.DotGraphNode;

/**
 * Special purpose class for traversing the call graph and collecting all the
 * unique method calls.
 * 
 * Requires that a call graph exists in the soot scene
 * 
 * @author Sean Mooney
 * 
 */
public class CallGraphDFS {
	private SootMethod entryPoint;
	private HashSet<String> markedMethods = new HashSet<String>();
	public CallGraph callGraph = null;
	private HashMap<String, List<SootMethod>> nodesMap = 
						new HashMap<String, List<SootMethod>>();
	
	private HashMap<String, List<String>> nodes = new HashMap<>();

	// DotGraph related
	private int cluster_id = 0;
	private DotGraph dot = null;

	// ACEP Methods
	List<SootMethod> acepMethods = new ArrayList<SootMethod>();

	public List<SootMethod> methodsAlongACEP = new ArrayList<SootMethod>();

	public CallGraphDFS(SootMethod entryPoint) {
		this.entryPoint = entryPoint;
		if (callGraph == null)
			callGraph = Scene.v().getCallGraph();
		/*
		 * if (callGraph != null) { //PurityAnalysis p = new
		 * PurityAnalysis(null); HashMap<String, String> opt = new
		 * HashMap<String, String>(); opt.put("dump-cg","true");
		 * opt.put("enabled","true"); opt.put("verbose","true");
		 * PurityAnalysis.v().transform("Purity", opt);
		 * 
		 * }
		 */
	}

	public void setACEPMethods(List<SootMethod> acepMethods) {
		this.acepMethods = acepMethods;
	}

	public SootMethod getFunction() {
		return entryPoint;
	}

	public void drawCG(String name) {
		if (acepMethods.size() > 0) {
			System.out.println("[Predicted methods] not empty");
		}
		dot = new DotGraph(name);
		dot.setGraphLabel(name);
		dot.setGraphAttribute("rankdir","LR");
		/*String orientation = "landscape";
		dot.setOrientation(orientation);*/
		// DotGraph sub = dot.createSubGraph("cluster" + this.cluster_id);
		/*
		 * DotGraphNode label = dot.drawNode(""); //sub.setGraphLabel("");
		 * label.setLabel(this.entryPoint.toString());
		 * label.setAttribute("fontsize", "18"); label.setShape("box");
		 */
		Predicate<SootMethod> compositeConjPredicate = Filters
				.compositeConjPredicate(
				/*Filters.noDefaultConstructor(),*/
						Filters.noInterfaceDefaultConstructor(),
						Filters.noApplyDeletesMethod());
		methodsAlongACEP.add(entryPoint);
		traverse1(entryPoint, compositeConjPredicate);
		dot.plot(name + ".dot");
	}

	private boolean collectMethod1(SootMethod tgt,
			Predicate<SootMethod> methodFilter, SootMethod src) {
		String sig = tgt.getSignature();
		if(nodes.get(src.getSignature()) == null)
			nodes.put(src.getSignature(), new ArrayList<>());
		if (methodFilter.apply(tgt) && !nodes.get(src.getSignature()).contains(tgt.getSignature())/* && !markedMethods.contains(sig)*/) {
			markedMethods.add(sig);
			// list.add(tgt);
			// Logging.trace(sig + " collected!");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Recursively traverse the call tree. Once a method has a been explored, do
	 * not re-explore.
	 * 
	 * @param m
	 * @param list
	 * @param methodFilter
	 *            -> A filter to remove unneeded methods, like jdk or any
	 *            library methods.
	 */

	private void traverse1(SootMethod m, Predicate<SootMethod> methodFilter) {

		// if(SystemConstants.DEBUG){
		// verboseMethod(m);
		// }
		//		
		// Logging.info("Exploring method: " + m.getName());

		Iterator<Edge> outEdges = callGraph.edgesOutOf(m);
		while (outEdges.hasNext()) {
			Edge e = outEdges.next();
			SootMethod tgt = e.tgt();
			// TODO: Clean this up. Make one call to collect, not two.
			if (collectMethod1(tgt, methodFilter, e.src())) {
				// Add the target node to dotGraph
				// DotGraph sub = dot.createSubGraph("cluster" +
				// this.cluster_id);
				/*
				 * DotGraphNode label = dot.drawNode("");
				 * //sub.setGraphLabel("");
				 * label.setLabel(this.entryPoint.toString());
				 * label.setAttribute("fontsize", "18"); label.setShape("box");
				 */

				// Add edge from node->targetNod
				nodes.get(e.src().getSignature()).add(tgt.getSignature());
					
				if (acepMethods.contains(m) && acepMethods.contains(tgt)
						&& methodsAlongACEP.contains(m)) {
					DotGraphEdge edge = dot.drawEdge(m.getSignature(), tgt
							.getSignature());
					System.out.println("[dotgraph] " + m.getSignature() + "--->"
							+ tgt.getSignature());
					edge.setStyle("bold");
					DotGraphNode first = dot.getNode(m.getSignature());
					DotGraphNode second = dot.getNode(tgt.getSignature());
					first.setAttribute("color", "turquoise");
					first.setStyle("filled");
					second.setAttribute("color", "turquoise");
					second.setStyle("filled");
					methodsAlongACEP.add(tgt);
				}
				
				else if (acepMethods.contains(tgt)) {
					DotGraphNode second = dot.getNode(tgt.getSignature());
					second.setAttribute("color", "yellow");
					second.setStyle("filled");
				}
				/*
				 * edge.setAttribute("ltail", "cluster" + m.getName());
				 * edge.setAttribute("lhead", "cluster" + tgt.getName());
				 */
				// traverse rest of the callgraph
				traverse1(tgt, methodFilter);
			}
		}
	}

	public List<SootMethod> buildDFSFunctionTree() {
		String entryPointSig = entryPoint.getSignature();
		if (nodesMap.containsKey(entryPointSig))
			return nodesMap.get(entryPointSig);
		ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
		markedMethods.clear(); // remove all marks, in case this is run multiple
								// times.
		methods.add(entryPoint);
		@SuppressWarnings("unchecked")
		Predicate<SootMethod> compositeConjPredicate = Filters
				.compositeConjPredicate(Filters.noLibraryFilter(),
				/*Filters.noDefaultConstructor(),*/
						Filters.noInterfaceDefaultConstructor(),
						Filters.noApplyDeletesMethod());

		traverse(entryPoint, methods, compositeConjPredicate);
		nodesMap.put(entryPointSig, methods);
		return methods;
	}

	private boolean collectMethod(SootMethod tgt, List<SootMethod> list,
			Predicate<SootMethod> methodFilter) {
		String sig = tgt.getSignature();
		if (methodFilter.apply(tgt) && !markedMethods.contains(sig)) {
			markedMethods.add(sig);
			list.add(tgt);
			// Logging.trace(sig + " collected!");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Recursively traverse the call tree. Once a method has a been explored, do
	 * not re-explore.
	 * 
	 * @param m
	 * @param list
	 * @param methodFilter
	 *            -> A filter to remove unneeded methods, like jdk or any
	 *            library methods.
	 */

	private void traverse(SootMethod m, List<SootMethod> list,
			Predicate<SootMethod> methodFilter) {

		// if(SystemConstants.DEBUG){
		// verboseMethod(m);
		// }
		//		
		// Logging.info("Exploring method: " + m.getName());

		Iterator<Edge> outEdges = callGraph.edgesOutOf(m);
		while (outEdges.hasNext()) {
			Edge e = outEdges.next();
			SootMethod tgt = e.tgt();
			// TODO: Clean this up. Make one call to collect, not two.
			if (collectMethod(tgt, list, methodFilter)) {
				traverse(tgt, list, methodFilter);
			}
		}
	}
	/*
	 * private static void verboseMethod(SootMethod m){
	 * //Logging.trace(m.getName()); if(m.hasActiveBody()){ Body b =
	 * m.getActiveBody(); PatchingChain<Unit> units = b.getUnits(); for(Unit u :
	 * units){ //Logging.trace(u.toString()); } }else{
	 * //Logging.trace("Has no active body"); } //Logging.trace(""); }
	 */
}
