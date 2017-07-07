package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import soot.util.Chain;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphBuilder;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;

public class CallGraphTest {
	
	static CallGraphDFS CG;
	private static final String JRE = "/usr/lib/jvm/java-6-openjdk/jre/lib";
	private static final String LIB = "/home/gupadhyaya/sootLibs";
	
	static String construct_class_path() {
		String cp = "";
		List<String> _jre_jars = new ArrayList<String>();
		_jre_jars.add("rt.jar");
		_jre_jars.add("jce.jar");
		for (String jar : _jre_jars) {
			cp = cp + JRE + "/" + jar + ":";
		}
		List<String> _lib_jars = new ArrayList<String>();
		// soot jars
		_lib_jars.add("sootclasses-2.4.0.jar");
		_lib_jars.add("jasminclasses-2.4.0.jar");
		_lib_jars.add("polyglotclasses-1.3.5.jar");
		
		// Application classes jar
		_lib_jars.add("test.jar");
		for (String jar : _lib_jars) {
			cp = cp + LIB + "/" + jar + ":";
		}
		/*List<String> _processdir = new ArrayList<String>();
		_processdir.add(LIB);
		for (String processdir : _processdir) {
			cp = cp + processdir + ":";
		}*/
		cp = cp + LIB;
		return cp;
	}
	
	public static void main(String[] args) {
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);
		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");
		/*List<String> _processdir = new ArrayList<String>();
		_processdir.add("/home/gupadhyaya/workspace/test12/bin");
		Options.v().set_process_dir(_processdir);*/
		String _classpath = construct_class_path();
		System.out.println (_classpath);
		Options.v().set_soot_classpath(_classpath);
		List<String> _includes = new ArrayList<String>();
		_includes.add("iastate.edu");
		Options.v().set_include(_includes);
		/*Module m = new Module("DynamicLoading");
		m.init();*/
		//_module = m;
		SootClass c = Scene.v().loadClassAndSupport("DynamicLoading");
		SootMethod _sootMethod = c.getMethodByName("main");
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		// Important step, without which you will not be able to run spark
		// analysis
		Scene.v().loadNecessaryClasses();
		// Spark analysis, true for brief analysis and false for full
		CallGraphBuilder.setSparkPointsToAnalysis(true);
		CG = new CallGraphDFS(_sootMethod);
		//display();
		timingAnalysis();
	}
	
	static void display () {    
        // display the list of application classes
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for (SootClass sootClass : classes) {
			System.out.println (sootClass.getName());
		}
        
        // CallGraph CG = Scene.v().getCallGraph();
        //dfs = getCallGraph();
        // spp = new StaticProfilePassImpl(dfs, main);
        //spp.runOnModule(null);
        List<SootMethod> methods = CG.buildDFSFunctionTree();
        
        System.out.println ("====================================================================================");
        // display methods
        
        Iterator methodIt = methods.iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			System.out.println(m.getSignature());
			
		}	
        
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
				
				System.out.println (methodString);

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

}
