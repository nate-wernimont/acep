package edu.iastate.cs.design.asymptotic.tests;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphBuilder;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.MethodRWSet;
import soot.jimple.toolkits.pointer.RWSet;
import soot.jimple.toolkits.pointer.SideEffectAnalysis;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.cfgcmd.CFGToDotGraph;

public class Track {

	HashMap<SootClass, List<SootField>> classFields = new HashMap<SootClass, List<SootField>>();
	static CallGraphDFS CG;
	static CFGToDotGraph graphToDot = new CFGToDotGraph();
	
	private static Map<SootMethod, MethodRWSet> METHODS_SUMMARY 
			= new HashMap<SootMethod, MethodRWSet>();

	public static void main(String[] args) {
		// Prepare soot for pointsToAnalysis
		prepare(args);
	}
	
	public static void addSummary(SootMethod sootMethod, Set<RWSet> readSet,
			Set<RWSet> writeSet) {
		MethodRWSet methodRWSet = new MethodRWSet();
		for (RWSet rwSet : readSet) {
			methodRWSet.union(rwSet);
		}
		for (RWSet rwSet : writeSet) {
			methodRWSet.union(rwSet);
		}
		METHODS_SUMMARY.put(sootMethod, methodRWSet);
	}
	
	public static void addSummary(SootMethod sootMethod, Set<RWSet> readWriteSet) {
		MethodRWSet methodRWSet = new MethodRWSet();
		for (RWSet rwSet : readWriteSet) {
			methodRWSet.union(rwSet);
		}
		METHODS_SUMMARY.put(sootMethod, methodRWSet);
	}

	/**
	 * 
	 * @return
	 */
	public static String construct_class_path() {
		String cp = "";
		String[] _jre_jars = { "rt.jar", "jce.jar" };
		//String JRE = "/usr/lib/jvm/java-6-openjdk/jre/lib";
		String HOME = "/home/ganeshau";
		String JRE = HOME;
		for (String jar : _jre_jars) {
			cp = cp + JRE + "/" + jar + ":";
		}
		cp = cp + HOME + "/" + "test12.jar" + ":";
		cp = cp + HOME;
		return cp;
	}

	/**
	 * 
	 * @param args
	 */
	public static void prepare(String[] args) {
		
		/*PackManager.v().getPack("jtp").add(
		        new Transform("jtp.myTransform", new BodyTransformer() {
					
					@Override
		          protected void internalTransform(Body body, String phase, Map options) {
		            System.out.println("IN METHOD: " + body.getMethod().getName());
		        	  
		          }
		        }));*/
		
		/*PackManager.v().getPack("wjtp").add(
				new Transform("wjtp.myTransform", new SceneTransformer() {
					
					@Override
					protected void internalTransform(String arg0, Map arg1) {
						// TODO Auto-generated method stub
						System.err.println(Scene.v().getApplicationClasses());
						
					}
				}));*/
		
		/*PackManager.v().getPack("cg").add(
				new Transform("cg.myTransform", new SceneTransformer() {
					
					@Override
					protected void internalTransform(String arg0, Map arg1) {
						// TODO Auto-generated method stub
						if (Scene.v().hasCallGraph())
							System.out.println("Yes, call graph found");
					}				
				}));*/
		
		/*PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {
					@Override
					protected void internalTransform(Body body, String phase, Map options) {
						SootMethod method = body.getMethod();
						Chain units = body.getUnits();
						Iterator stmtIt = units.snapshotIterator();
						while (stmtIt.hasNext()) {
							Stmt stmt = (Stmt)stmtIt.next();
							//System.out.println(method.toString()+"---"+stmt.toString());
							//units.removeFirst();
							//units.removeLast();
							if (stmt.containsInvokeExpr()) {
								units.remove(stmt);
							}
						}
						
					}			
				}));*/
		
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);
		// Processdir should specify the class file locations which
		// have to be considered for the analysis

		String classpath = construct_class_path();
		//System.out.println(classpath);
		Options.v().set_soot_classpath(classpath);

		// Setting the phase options accelerates the initial cg construction

		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");
		
		/*HashMap<String, String> opt = null;
		opt = CallGraphBuilder.getBriefSparkAnalysisOptions();
		String[]  myArgs = 
	    {
				"verbose","true",
				"ignore-types","false",
				"force-gc","false",
				"pre-jimplify","false",
				"vta","false",
				"rta","false",
				"field-based","false",
				"types-for-sites","false",
				"merge-stringbuffer","true",
				"string-constants","false",
				"simulate-natives","true",
				"simple-edges-bidirectional","false",
				"on-fly-cg","true",
				"simplify-offline","false",
				"simplify-sccs","false",
				"ignore-types-for-sccs","false",
				"propagator","worklist",
				"set-impl","double",
				"double-set-old","hybrid",
				"double-set-new","hybrid",
				"dump-html","false",
				"dump-pag","false",
				"dump-solution","false",
				"topo-sort","false",
				"dump-types","true",
				"class-method-var","true",
				"dump-answer","false",
				"add-tags","false",
				"set-mass","false"
	    };
		Options.v().parse( myArgs);*/
		// Ready to include stuffs
		List<String> includes = new ArrayList<String>();
		includes.add("arc");
		//includes.add("wordSearch");
		Options.v().set_include(includes);
		
		Options.v().set_output_dir("myOut");
		Options.v().set_output_format(Options.output_format_d);

		// Mention the starting point and the main method
		String mainClass = "arc.A", mainMethod = "main";
		SootClass c = Scene.v().loadClassAndSupport(mainClass);
		SootMethod sootMethod = c.getMethodByName(mainMethod);
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		// Important step, without which you will not be able to run spark
		// analysis
		Scene.v().loadNecessaryClasses();
		// Spark analysis, true for brief analysis and false for full
		CallGraphBuilder.setSparkPointsToAnalysis(true);
		
		CG = new CallGraphDFS(sootMethod);
		/*Chain<SootClass> classes = Scene.v().getApplicationClasses();
		for (SootClass sootClass : classes) {
			System.out.println(sootClass.toString());
		}*/
		//CG.drawCG(mainClass);
		timingAnalysis();
		
		/*PackManager.v().runPacks();
	    PackManager.v().writeOutput();*/
		
		//soot.Main.main(args);
	    //CG = new CallGraphDFS(sootMethod);
	}

	/**
	 * 
	 * @param sootClass
	 * @return
	 */
	public boolean isJFrameSuperClass(SootClass sootClass) {
		if (sootClass.getSuperclass().getName()
				.startsWith("javax.swing.JFrame"))
			return true;
		return false;
	}

	/**
	 * 
	 * @param sootField
	 * @return
	 */
	public static boolean isSwingUIElement(SootField sootField) {
		if (sootField.getType().toString().startsWith("javax.swing"))
			return true;
		return false;
	}

	/**
	 * 
	 * @param sootClass
	 * @return
	 */
	public boolean hasFields(SootClass sootClass) {
		if (classFields.containsKey(sootClass))
			return true;
		return false;
	}

	/**
	 * 
	 * @param sootClass
	 * @param field
	 */
	public void addClassField(SootClass sootClass, SootField field) {
		if (hasFields(sootClass)) {
			classFields.get(sootClass).add(field);
		} else {
			ArrayList<SootField> fields = new ArrayList<SootField>();
			fields.add(field);
			classFields.put(sootClass, fields);
		}
	}

	/**
	 * 
	 * @param sootClass
	 */
	public void displayClassFields(SootClass sootClass) {
		List<SootField> fields = classFields.get(sootClass);
		for (SootField sootField : fields) {
			System.out.println("Type: " + sootField.getType().toString()
					+ ", Name: " + sootField.getName());
		}
	}

	/**
	 * 
	 * @param unit
	 * @return
	 */
	public static int getUnitLineNumber(Unit unit) {
		for (Iterator<Tag> j = unit.getTags().iterator(); j.hasNext();) {
			Tag tag = j.next();
			if (tag instanceof LineNumberTag) {
				LineNumberTag lineNumberTag = (LineNumberTag) tag;
				return lineNumberTag.getLineNumber();
			}
		}
		return -1;
	}
	
	public static void sideEffectAnalysis(SootMethod m) {
		
		JimpleBody body = (JimpleBody) m.retrieveActiveBody();
		System.out.println ("========================="+m.toString()+"=========================");
		// Body body = m.retrieveActiveBody();
		/*
		 * System.out.println("=======================================");
		 * System.out.println(body.toString());
		 * System.out.println("=======================================");
		 */
		// DumbPointerAnalysis.v()
		PointsToAnalysis pointsToAnalysis;
		if (Scene.v().hasPointsToAnalysis()) {
			pointsToAnalysis = Scene.v().getPointsToAnalysis();
		} else {
			System.out.println("No pointsToset ...");
			pointsToAnalysis = DumbPointerAnalysis.v();
		}
		SideEffectAnalysis sea = new SideEffectAnalysis(
				DumbPointerAnalysis.v(), Scene.v().getCallGraph());
		sea.findNTRWSets(body.getMethod());

		HashMap stmtToReadSet = new HashMap();
		HashMap stmtToWriteSet = new HashMap();
		
		Set<RWSet> rwSets = new HashSet<RWSet>();

		HashMap<RWSet, Object> rwToStmt = new HashMap<RWSet, Object>();
		// ArrayList<RWSet> sets = new ArrayList<RWSet>();
		// SideEffectAnalysisTest seaTest = new SideEffectAnalysisTest();
		//Analysis analysis = new Analysis();
		Track track = new Track();
		UniqueRWSets sets = track.new UniqueRWSets();
		for (Iterator stmtIt = body.getUnits().iterator(); stmtIt.hasNext();) {
			final Stmt stmt = (Stmt) stmtIt.next();
			Object key = stmt.toString();
			if (!stmtToReadSet.containsKey(key)) {
				RWSet rwSet = sets.getUnique(sea
						.readSet(body.getMethod(), stmt));
				stmtToReadSet.put(key, rwSet);
				rwToStmt.put(rwSet, key);
				rwSets.add(rwSet);
			}
			// rwSet = sea.writeSet(m, stmt);
			if (!stmtToWriteSet.containsKey(key)) {
				RWSet rwSet = sets.getUnique(sea.writeSet(body.getMethod(),
						stmt));
				stmtToWriteSet.put(key, rwSet);
				rwToStmt.put(rwSet, key);
				rwSets.add(rwSet);
			}

		}

		//addSummary(m, rwSets);
		for (Iterator outerIt = sets.iterator(); outerIt.hasNext();) {
			final RWSet outer = (RWSet) outerIt.next();
			Set<SootField> fields = outer.getGlobals();
			for (SootField sootField : fields) {
				if (sootField.getDeclaringClass().getPackageName().startsWith("arc"))
					System.out.println(sootField.toString());
			}
		}
		
		System.out.println("*******************************************");
		
		for (Iterator outerIt = sets.iterator(); outerIt.hasNext();) {
			final RWSet outer = (RWSet) outerIt.next();

			for (Iterator innerIt = sets.iterator(); innerIt.hasNext();) {

				final RWSet inner = (RWSet) innerIt.next();
				/*if (inner == outer)
					break;*/
				if (inner.toString().equals(outer.toString()))
					continue;
				
				if (outer.hasNonEmptyIntersection(inner)) {
					// System.out.println(outer.toString()+"---------"+inner.toString());
					System.out.println (rwToStmt.get(outer) + "--->"
							+ rwToStmt.get(inner) + "\n");
				}
			}
		}
		/*System.out.println("Fields referred in SideEffectAnalysis...");
		for (Iterator<RWSet> setIter = rwSets.iterator(); setIter.hasNext();) {
			RWSet rwSet = setIter.next();
			if (rwSet == null) continue;
			Set<SootField> fields = rwSet.getFields();
			for (SootField sootField : fields) {
				System.out.println(rwSet.toString()+"----"+sootField.toString());
			}
		}
		System.out.println("SideEffectAnalysis done...");*/
	
	}
	
	static void timingAnalysis() {
		// Start with the main function.
		SootMethod root = CG.getFunction();
		List<SootMethod> successors = CG.buildDFSFunctionTree();

		System.out
				.println("=================================================================timingAnalysis=======================================================================");
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

			/*System.out.println("[T] method: " + method.getName() + " ("
					+ method.getDeclaringClass() + ")");*/

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
				/*System.out.println("[T-" + methodCount + "] Processing "
						+ current.getName() + " ("
						+ current.getDeclaringClass().getName() + ")");*/
				//System.out.println (current.getSignature());
				
				//methodString += Modifier.toString(current.getModifiers()) + " "+current.getDeclaringClass()+ "."+current.getName()+current.getParameterTypes();
			/*	System.out.println(current.toString());
				System.out.println(current.getDeclaration());
				System.out.println(current.getSignature());
				System.out.println(current.getSubSignature());*/
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
				
				//System.out.println (methodString);
				
				// Let's invoke SideEffectAnalysis here...
				sideEffectAnalysis(current);
				
				/*JimpleBody b = (JimpleBody) current.retrieveActiveBody();
				//ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
				
				// BlockGraph bGraph = new BlockGraph(eug);
				CompleteBlockGraph cGraph = new CompleteBlockGraph(b);
				
				//CFGToDotGraph graph = new CFGToDotGraph();
				soot.util.dot.DotGraph dotGraph = graphToDot.drawCFG(cGraph, b);
				
				dotGraph.plot(current.getSignature()+".dot");*/

				/*CallGraphDFS CGnode = new CallGraphDFS(current);
				process(current);*/
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
