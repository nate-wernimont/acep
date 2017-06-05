/*package edu.iastate.cs.design.asymptotic.tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.RWSet;
import soot.jimple.toolkits.pointer.SideEffectAnalysis;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.Chain;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Log;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;

public class Elements extends Benchmark {

	// Configuration file contains mainClass, main method, library file location,
	// processing directories, and library jars
	private static String _config = "/home/ganeshau/Travis/config.xml";
	// Map containing list of soot stmt's for each method
	// this map is uploaded after analysis stage
	private HashMap<SootMethod, ArrayList<Stmt>> methodStmtsTokeep 
			= new HashMap<SootMethod, ArrayList<Stmt>>();
	// Map contains list of class fields for each class
	// the field's here point to UI elements
	private HashMap<SootClass, List<SootField>> classFields 
			= new HashMap<SootClass, List<SootField>>();
	// Map indicating if a method has target in it
	private HashMap<SootMethod, Boolean> hasTargetRWSet 
			= new HashMap<SootMethod, Boolean>();
	// List of UI elements which can be tracked
	private List<SootField> elements = new ArrayList<SootField>();
	// The UI element to track
	private SootField target;
	
	*//**
	 * Constructor reads the config file and prepares soot
	 * for spark points-to analysis and gets the callgraph
	 *//*
	public Elements() {
		super(_config);
		prepareSoot();
		// display();
	}

	*//**
	 * Prints the methodStmtsTokeep map values
	 *//*
	public void printAnalysisResults() {
		Log.printDependencies("======================================= "
				+ "Statements to keep ==================================="
				+ "\n");
		Set<Entry<SootMethod, ArrayList<Stmt>>> entrySet = methodStmtsTokeep
				.entrySet();
		for (Iterator<Entry<SootMethod, ArrayList<Stmt>>> mapIter = entrySet
				.iterator(); mapIter.hasNext();) {
			Entry<SootMethod, ArrayList<Stmt>> entry = mapIter.next();
			Log.printDependencies(entry.getKey().toString() + "-------"
					+ entry.getValue().toString() + "\n");
		}
	}
	
	*//**
	 * Sets the output directory and output format and then runs
	 * the transformation packages added to the soot package 
	 * manager and write output
	 *//*
	public void runJTPPackage() {
		Options.v().set_output_dir("myOut");
		Options.v().set_output_format(Options.output_format_c); // Options.output_format_d
		PackManager.v().runPacks();
		PackManager.v().writeOutput();
	}

	*//**
	 * Adds jtp custom package to package manager
	 * this function has to define the operations which has to be
	 * run on the class files based on the analysis result
	 *//*
	public void addJTPPackage() {
		// Define package which does the modification of class files
		PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {

					@Override
					protected void internalTransform(Body body, String phase,
							Map options) {
						int removedStmts = 0;
						SootMethod method = body.getMethod();
						Chain units = body.getUnits();
						Iterator stmtIt = units.snapshotIterator();
						if (methodStmtsTokeep.containsKey(method)) {
							ArrayList<Stmt> methodStmts = methodStmtsTokeep
									.get(method);
							if (methodStmts.size() > 0) {
								Log
										.printDependencies("[internalTransform] Method: "
												+ method.toString() + "\n");
								while (stmtIt.hasNext()) {
									Stmt stmt = (Stmt) stmtIt.next();
									// System.out.println(method.toString()+"---"+stmt.toString());
									// units.removeFirst();
									// units.removeLast();
									
									 * if (stmt.containsInvokeExpr()) {
									 * units.remove(stmt); }
									 
									boolean keep = false;
									for (Stmt methodStmt : methodStmts) {
										if (stmt.equals(methodStmt)) {
											keep = true;
										}
									}

									if (!keep) {
										removedStmts++;
										units.remove(stmt);
									}

								}
								Log.printDependencies("#Remove Stmts: "+removedStmts+"\n");
							}
						} else {
							// Remove this method
							//removedMethods++;
							Log
							.printDependencies("[internalTransform:Remove] Method: "
									+ method.toString() + "class: "
									+ method.getDeclaringClass().toString()+"\n");
							//method.getDeclaringClass().removeMethod(method);
						}

						// Log.printDependencies
						// ("\n [internalTransform]"+method.toString()+"\n");

					}
				}));
	}
	

	*//**
	 * Checks if a class is a JFrame class
	 * @param sootClass
	 * @return
	 *//*
	public boolean isJFrameSuperClass(SootClass sootClass) {
		if (sootClass.getSuperclass().getName()
				.startsWith("javax.swing.JFrame"))
			return true;
		return false;
	}

	*//**
	 * Checks if a sootField is an Swing UI Element
	 * @param sootField
	 * @return
	 *//*
	public boolean isSwingUIElement(SootField sootField) {
		if (sootField.getType().toString().startsWith("javax.swing"))
			return true;
		return false;
	}
	
	*//**
	 * 
	 * @param sootClass
	 * @return
	 *//*
	public boolean hasFields(SootClass sootClass) {
		if (classFields.containsKey(sootClass))
			return true;
		return false;
	}

	*//**
	 * 
	 * @param sootClass
	 * @param field
	 *//*
	public void addClassField(SootClass sootClass, SootField field) {
		if (hasFields(sootClass)) {
			classFields.get(sootClass).add(field);
		} else {
			ArrayList<SootField> fields = new ArrayList<SootField>();
			fields.add(field);
			classFields.put(sootClass, fields);
		}
	}

	*//**
	 * 
	 * @param sootClass
	 *//*
	public void displayClassFields(SootClass sootClass) {
		List<SootField> fields = classFields.get(sootClass);
		for (SootField sootField : fields) {
			System.out.println("Type: " + sootField.getType().toString()
					+ ", Name: " + sootField.getName());
		}
	}

	*//**
	 * Displays the source code line number of the soot unit
	 * @param unit
	 * @return
	 *//*
	public int getUnitLineNumber(Unit unit) {
		for (Iterator<Tag> j = unit.getTags().iterator(); j.hasNext();) {
			Tag tag = (Tag) j.next();
			if (tag instanceof LineNumberTag) {
				LineNumberTag lineNumberTag = (LineNumberTag) tag;
				return lineNumberTag.getLineNumber();
			}
		}
		return -1;
	}

	*//**
	 * Display list of UI elements available for tracking
	 * from the list of classes uploaded for analysis and
	 * asks user to select which element to track
	 *//*
	public void getTrackElement() {
		// Grab all the UI elements for this project
		Chain<SootClass> appClasses = Scene.v().getApplicationClasses();
		for (SootClass sootClass : appClasses) {
			if (isJFrameSuperClass(sootClass)) {
				Chain<SootField> fields = sootClass.getFields();
				for (SootField sootField : fields) {
					if (isSwingUIElement(sootField))
						elements.add(sootField);
				}
			}
		}

		System.out.println("Select UI element for analysis...");
		int option = 0;
		for (SootField element : elements) {
			System.out.println(option + ". " + element.toString());
			option++;
		}
		try {
			BufferedReader bufferRead = new BufferedReader(
					new InputStreamReader(System.in));
			String s = bufferRead.readLine();
			target = elements.get(Integer.parseInt(s));
			System.out.println("Processing " + target.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// UI element to use for further analysis
		Log.printDependencies("Target = " + target.toString() + "\n");
	}
	
	
	*//**
	 * Filter only those program statements which have dependencies
	 * with target. This method does sideEffectAnalysis to determine
	 * the statements which are relevant to the current target
	 * @param method
	 * @return
	 *//*
	public boolean hasSideEffect(SootMethod method) {

		// Scheme:
		// First find out if there are direct reference to the target field
		// Compute indirect reference via method invocations
		// Compute its Set<RWSet>
		// Do intersection with other RWSets and find out what can be removed

		if (!method.hasActiveBody())
			return false;

		Log.printDependencies("==================" + method.toString()
				+ "====================" + "\n");
		Body body = method.retrieveActiveBody();
		// Scene.v().releaseActiveHierarchy();
		SideEffectAnalysis sea = new SideEffectAnalysis(
				DumbPointerAnalysis.v(), Scene.v().getCallGraph());
		if (sea == null)
			System.out.println("Sea is null");
		sea.findNTRWSets(method);
		
		UniqueRWSets sets = this.new UniqueRWSets();
		ArrayList<RWSet> targetRWSet = new ArrayList();
		boolean hasTarget = false;

		ArrayList<Stmt> stmtsToKeep = new ArrayList<Stmt>();

		for (Iterator units = body.getUnits().iterator(); units.hasNext();) {
			Unit unit = (Unit) units.next();
			Object key = unit.toString();
			// Get readSet for this stmt
			RWSet readSet = sets.getUnique(sea.readSet(method, (Stmt) unit));
			// Get writeSet
			RWSet writeSet = sets.getUnique(sea.writeSet(method, (Stmt) unit));

			// Look for direct assignment to target
			for (Iterator boxes = unit.getDefBoxes().iterator(); boxes
					.hasNext();) {
				ValueBox box = (ValueBox) boxes.next();
				Value value = box.getValue();

				if (value instanceof FieldRef) {
					SootField referred = ((FieldRef) value).getField();
					if (referred == target) {
						Log.printDependencies("[FieldRef:Target]"
								+ referred.toString() + "\n");
						hasTarget = true;
						targetRWSet.add(readSet);
						targetRWSet.add(writeSet);
						stmtsToKeep.add((Stmt) unit);
					}
				}
			}

			// Look for any function call which has sideEffect
			for (Iterator boxes = unit.getUseBoxes().iterator(); boxes
					.hasNext();) {
				ValueBox box = (ValueBox) boxes.next();
				Value expr = box.getValue();

				if (expr instanceof InvokeExpr) {
					SootMethod invokedMethod = ((InvokeExpr) expr).getMethod();
					// All three cases of invokeExpr
					if (expr instanceof SpecialInvokeExpr) {

					} else if (expr instanceof InstanceInvokeExpr) {

					} else if (expr instanceof StaticInvokeExpr) {

					}
					if (hasTargetRWSet.containsKey(invokedMethod)) {
						// ArrayList<RWSet> invokedRWSet =
						// methodsRWSets.get(invokedMethod);
						// Add this invoke stmt to target list of current method
						Log.printDependencies("[Invocation:Target]"
								+ invokedMethod.toString() + "\n");
						hasTarget = true;
						targetRWSet.add(readSet);
						targetRWSet.add(writeSet);
						stmtsToKeep.add((Stmt) unit);
					}

				}
			}
		}
		// Again go through each stmt and do intersection of its
		// RWSet with target RWSet
		for (Iterator units = body.getUnits().iterator(); units.hasNext();) {
			Unit unit = (Unit) units.next();
			Object key = unit.toString();
			// Get readSet for this stmt
			RWSet readSet = sets.getUnique(sea.readSet(body.getMethod(),
					(Stmt) unit));
			// Get writeSet
			RWSet writeSet = sets.getUnique(sea.writeSet(body.getMethod(),
					(Stmt) unit));

			for (RWSet rwSet : targetRWSet) {
				if (rwSet == null)
					continue;
				if ((readSet != null) && rwSet.hasNonEmptyIntersection(readSet)) {
					Log.printDependencies("[SideEffect:Target]"
							+ unit.toString());
					stmtsToKeep.add((Stmt) unit);
				}
				if ((writeSet != null)
						&& rwSet.hasNonEmptyIntersection(writeSet)) {
					Log.printDependencies("[SideEffect:Target]"
							+ unit.toString());
					stmtsToKeep.add((Stmt) unit);
				}
			}
		}

		Log.printDependencies("Done with basic stmt filtering and #Stmt = "
				+ stmtsToKeep.size() + "\n");

		// Add RWSet to Map for this SootMethod
		if (hasTarget)
			hasTargetRWSet.put(method, hasTarget);

		// Add the stmtsTokeep to methodStmtsToKeep
		methodStmtsTokeep.put(method, stmtsToKeep);

		return true;
	}
	
	
	*//**
	 * Visit the callgraph in DFS order
	 * on each method call sideEffectAnalysis
	 *//*
	public void visit () {
		// Start with the main function.
		SootMethod root = _cg.getFunction();
		List<SootMethod> successors = _cg.buildDFSFunctionTree();

		System.out
				.println("=========================================" +
						"========================timingAnalysis=======" +
						"================================================================");

		// If main has no successors, i.e., calls no other functions.
		if (root == null || successors.size() == 0)
			return;

		// Successor is the method itself then return
		if ((successors.size() == 1) && successors.get(0).equals(root))
			return;

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
					// Indication of loop or recurrence. Don't do anything now
					// TODO: Visit later, if timing is not accurate enough
				}
			}

			if (!FoundNew) {
				// Obtain the function without new successors.
				current = visitStack.pop();
				methodCount++;
				
				String methodString;
				methodString = current.getDeclaration();
				String className = current.getDeclaringClass().toString();
				String parts[] = methodString.split(" ");
				methodString = "";
				for (String string : parts) {
					if (string.contains("(")) {
						String temp = className + "." + string;
						methodString += temp;
					} else {
						methodString += string + " ";
					}
				}

				System.out.println(methodString);

				// Let's invoke SideEffectAnalysis here...
				hasSideEffect(current);

				
				 * JimpleBody b = (JimpleBody) current.retrieveActiveBody();
				 * //ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
				 * 
				 * // BlockGraph bGraph = new BlockGraph(eug);
				 * CompleteBlockGraph cGraph = new CompleteBlockGraph(b);
				 * 
				 * //CFGToDotGraph graph = new CFGToDotGraph();
				 * soot.util.dot.DotGraph dotGraph = graphToDot.drawCFG(cGraph,
				 * b);
				 * 
				 * dotGraph.plot(current.getSignature()+".dot");
				 

				
				 * CallGraphDFS CGnode = new CallGraphDFS(current);
				 * process(current);
				 
				inStack.remove(current);
			} else {
				// Found a new function.
				// Go down one level if there is a unvisited successor.
				inStack.add(current);
				visitStack.push(current);
			}

		} while (!visitStack.empty());

	}

	*//**
	 * Class to track RWSets for SideEffectAnalysis
	 * @author ...
	 *
	 *//*
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
*/