package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphBuilder;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.interfaces.impl.StaticProfilePassImpl;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class LoadBenchmark {

	private final String TEMP = "/home/gupadhyaya/dacapo";
	private final String SOOTLIB = "/home/gupadhyaya/sootLibs";
	private final String JRE = "/usr/lib/jvm/java-6-openjdk/jre/lib";//"/usr/lib/jvm/java-6-openjdk/jre/lib";
	private final String JAR = TEMP+"/jar";
	private final String AVRORA = TEMP+"/avrora";
	private final String[] dacapo_jars = {
			"bootstrap.jar",
			"commons-httpclient.jar",
			"serializer.jar",
			"xalan-benchmark.jar",
			"xalan.jar",
			"avrora-cvs-20091224.jar",
			"antlr-3.1.3.jar",
			"asm-3.1.jar",
			"asm-commons-3.1.jar",
			"commons-codec.jar",
			"commons-daemon.jar",
			"commons-io-1.3.1.jar",
			"commons-logging-1.0.4..jar",
			"commons-logging.jar",
			"constantine-0.4.jar",
			"crimson-1.1.3.jar",
			"dacapo-tomcat.jar",
			"tomcat-juli.jar",
			"luindex.jar",
			"lucene-core-2.4.jar",
			"lusearch.jar",
			"batik-all-1.7.jar"
	};
	private final String[] soot_jars  = {
			"sootclasses-2.4.0.jar",
			"jasminclasses-2.4.0.jar",
			"polyglotclasses-1.3.5.jar"
	};
	private final String[] jre_jars = {
			"rt.jar",
			"jce.jar",
			"jsse.jar"
	};
	
	private StaticProfilePassImpl spp;
	private CallGraphDFS dfs;
	private SootMethod main;
	
	public LoadBenchmark () {
		setUpSoot();
	}
	
	String construct_class_path() {
		String cp = "";
		for (String jar : jre_jars) {
			cp = cp + JRE + "/" + jar + ":";
		}
		for (String jar : soot_jars) {
			cp = cp + SOOTLIB + "/" + jar + ":";
		}
		for (String jar : dacapo_jars) {
			cp = cp + JAR+ "/"+ jar + ":";
		}
		cp = cp + TEMP;
		//cp = cp + ":"+ AVRORA;
		// cp = cp + TEMP + "/org/apache/luindex";
		return cp;
	}
	
	void setUpSoot () {
		Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);
        // Set the process_dir
        List<String> process_dir = new ArrayList<String>();
        process_dir.add(TEMP);
        //Options.v().set_process_dir(process_dir);
        // Set soot-class-path
        String classpath = construct_class_path ();
        Options.v().set_soot_classpath(classpath);
        //DO NOT SET APP MODE. CAUSES SOOT TO BREAK
        //soot.options.Options.v().set_app(true);
        Options.v().setPhaseOption("cg","verbose:true");
        Options.v().setPhaseOption("cg","safe-newinstance");
        Options.v().setPhaseOption("cg","safe-forname");
        // Set main-class
        // Options.v().set_main_class("org.dacapo.luindex.Index");
        // Set inlcude packages 
        List<String> includes = new ArrayList<String>();
        includes.add("org.dacapo");
        includes.add("org.apache.lucene");
        //includes.add("org");
        /*includes.add("cck");
        includes.add("jintgen");*/
        Options.v().set_include(includes);
        
        //SootClass c = Scene.v().loadClassAndSupport("org.dacapo.lusearch.Search");//
        SootClass c = Scene.v().loadClassAndSupport("CallMain");
        main = c.getMethodByName("main");
        c.setApplicationClass();
        Scene.v().setMainClass(c);
        // Let's load some classes
        Scene.v().loadNecessaryClasses();
        
        CallGraphBuilder.setSparkPointsToAnalysis(true);
        //CallGraphBuilder.setCHAPointsToAnalysis();
        
        // display the list of application classes
        /*Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for (SootClass sootClass : classes) {
			System.out.println (sootClass.getName());
		}*/
        
        // CallGraph CG = Scene.v().getCallGraph();
        dfs = new CallGraphDFS (main);
        // spp = new StaticProfilePassImpl(dfs, main);
        //spp.runOnModule(null);
        List<SootMethod> methods = dfs.buildDFSFunctionTree();
        
        System.out.println ("====================================================================================");
        // display methods
        
        Iterator methodIt = methods.iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			System.out.println(m.getName()+"-------------------"+m.getDeclaringClass());
			
		}	
        
	}
	
	public StaticProfilePassImpl getStaticProfile () {
		return spp;
	}
	
	public CallGraphDFS getCallGraphDFS () {
		return dfs;
	}
	
	public SootMethod main () {
		return main;
	}
}
