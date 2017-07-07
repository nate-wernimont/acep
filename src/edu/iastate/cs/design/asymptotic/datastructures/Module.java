package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class Module extends Object {

	private String _class;
	private Iterator _mIter;
	private HashSet<SootMethod> methods = new HashSet<SootMethod>();
	private SootMethod _MAIN;

	public Module(String classname) {
		_class = classname;
		//init();
	}
	
	public void setMethods (List<SootMethod> meths) {
		methods = new HashSet<SootMethod>(meths);
	}
	
	public void setMain (SootMethod main) {
		_MAIN = main;
	}

	public void init() {
		/*
		Options
				.v()
				.set_soot_classpath("sootclasses-2.3.0.jar:jasminclasses-2.3.0.jar:polyglotclasses-1.3.5.jar:/home/gupadhyaya/workspace/test12/bin:/usr/lib/jvm/java-6-openjdk/jre/lib/rt.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/jce.jar"); //
		*/				//".;C:\\Users\\gupadhyaya\\workspace\\test12\\bin;E:\\lib\\rt.jar");// .:C:\\Program
		// E:\\lib\\rt.jar;E:\\lib\\jce.jar																																						// Files\\Java\\jre6\\lib\\rt.jar:C:\\Program
		//SootClass sClass = CallGraphBuilder.loadClass(_class, true);																																																									// Files\\Java\\jre6\\lib\\jce.jar");
		// Options.v().set_soot_classpath("");
		//System.out.println(Scene.v().getSootClassPath());
		// SootClass sClass = CallGraphBuilder.loadClass(_class, true); // Scene.v().loadClassAndSupport(_class);
		// sClass.setApplicationClass();
		//CallGraphBuilder.setCHAPointsToAnalysis();
		//CallGraphBuilder.getBriefSparkAnalysisOptions();
        //Scene.v().loadBasicClasses();
		setupSoot();
		// Mention the starting point and the main method
		SootClass c = Scene.v().loadClassAndSupport(_class);
		SootMethod sootMethod = c.getMethodByName("main");
		c.setApplicationClass();
		Scene.v().setMainClass(c);
		// Important step, without which you will not be able to run spark
		// analysis
		Scene.v().loadNecessaryClasses();
		// Spark analysis, true for brief analysis and false for full
		CallGraphBuilder.setSparkPointsToAnalysis(false);
		/*SootClass mainClass = CallGraphBuilder.loadClass(_class, true); 
		
		try{
            SootMethod mainMethod = mainClass.getMethodByName("main");
            Scene.v().loadNecessaryClasses();
            List<SootMethod> mainMeth = Arrays.asList(mainMethod);
            Scene.v().setEntryPoints(mainMeth);

            // classesLoaded = true;
        }catch(RuntimeException ame){
            System.out.println("Ambiguous main method.");
            throw ame;
        }
        
		CallGraphBuilder.setSparkPointsToAnalysis(false);
		_mIter = mainClass.getMethods().iterator();
		while (_mIter.hasNext()) {
			SootMethod m = (SootMethod) _mIter.next();
			methods.add(m);
			if (m.getName().equals("main")) {
				_MAIN = m;
			}
		}*/
		// Options.v().set_whole_program(true);
	}
	
	private void setupSoot() {
		Options.v().set_keep_line_number(true);
		Options.v().set_whole_program(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);

		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance");
		Options.v().setPhaseOption("cg", "safe-forname");

		Options.v()
				.set_soot_classpath(
						"soot-2.5.0.jar:/Users/hridesh/Downloads/jlex.jar:/Users/gupadhyaya/workspace/test/bin:/Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/jre/lib/jce.jar");

		List<String> procs = new ArrayList<String>();
		String proc = "/Users/gupadhyaya/workspace/test/bin";
		procs.add(proc);
		Options.v().set_process_dir(procs);
		
		List<String> incs = new ArrayList<String>();
		// Common include directory for all the Java Grande benchmarks
		incs.add("JLex");

		// Ready to include stuffs
		Options.v().set_include(incs);

		// Add excludes,
		List<String> _excludes = new ArrayList<String>();
		_excludes.add("java");
		Options.v().set_exclude(_excludes);
		// Important step, without which you will not be able to run spark
		// analysis
	}

	public Iterator<SootMethod> iterator() {
		return (_mIter = methods.iterator());
	}

	public SootMethod main() {
		return _MAIN;
	}
	
}
