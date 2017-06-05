package edu.iastate.cs.design.asymptotic.datastructures;
import java.util.HashMap;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;

/**
 * Create a callgraph for the given main class
 * @author Sean Mooney
 *
 */
public class CallGraphBuilder {

	/**
	 * Get a set of options that causes spark to ignore jdk type classes.
	 * @return
	 */
	public static  HashMap<String, String> getBriefSparkAnalysisOptions(){

		soot.G.v().out.println("[spark] using brief options");

		HashMap<String, String> opt = new HashMap<String, String>();
		opt.put("enabled","true");
		opt.put("verbose","true");
		opt.put("ignore-types","false");
		opt.put("force-gc","false");
		opt.put("pre-jimplify","false");
		opt.put("vta","false");
		opt.put("rta","false");
		opt.put("field-based","false");
		opt.put("types-for-sites","false");
		opt.put("merge-stringbuffer","true");
		opt.put("string-constants","false");
		opt.put("simulate-natives","true");
		opt.put("simple-edges-bidirectional","false");
		opt.put("on-fly-cg","true");
		opt.put("simplify-offline","false");
		opt.put("simplify-sccs","false");
		opt.put("ignore-types-for-sccs","false");
		opt.put("propagator","worklist");
		opt.put("set-impl","double");
		opt.put("double-set-old","hybrid");
		opt.put("double-set-new","hybrid");
		opt.put("dump-html","false");
		opt.put("dump-pag","false");
		opt.put("dump-solution","false");
		opt.put("topo-sort","false");
		opt.put("dump-types","true");
		opt.put("class-method-var","true");
		opt.put("dump-answer","false");
		opt.put("add-tags","false");
		opt.put("set-mass","false");

		return opt;
	}

	/**
	 * Get a set of options that causes spark to analyse everything.
	 * @return
	 */
	public static HashMap<String, String> getFullSparkAnalysisOptions(){

		soot.G.v().out.println("[spark] using full options");

		HashMap<String, String> opt = new HashMap<String, String>();
		opt.put("enabled","true");
		opt.put("verbose","true");
		opt.put("ignore-types","false");          
		opt.put("force-gc","false");            
		opt.put("pre-jimplify","false");          
		opt.put("vta","false");                   
		opt.put("rta","false");                   
		opt.put("field-based","false");           
		opt.put("types-for-sites","false"); 
		//opt.put("types-for-sites","true"); 
		opt.put("merge-stringbuffer","true");   
		opt.put("string-constants","false");     
		opt.put("simulate-natives","true");      
		opt.put("simple-edges-bidirectional","false");
		opt.put("on-fly-cg","true");  
		//opt.put("on-fly-cg","false");  
		opt.put("simplify-offline","false");    
		opt.put("simplify-sccs","false"); 
		//opt.put("simplify-sccs","true"); 
		opt.put("ignore-types-for-sccs","false");
		opt.put("propagator","worklist");
		opt.put("set-impl","double");
		opt.put("double-set-old","hybrid");         
		opt.put("double-set-new","hybrid");
		opt.put("dump-html","false");           
		opt.put("dump-pag","false");             
		opt.put("dump-solution","false");        
		opt.put("topo-sort","false");           
		opt.put("dump-types","true");             
		opt.put("class-method-var","true");     
		opt.put("dump-answer","false");          
		opt.put("add-tags","false");             
		opt.put("set-mass","false"); 
		return opt;
	}

	public static void setCHAPointsToAnalysis(){
	    CHATransformer.v().transform();
	    System.out.println("[cha] Done!");
	}

	/**
	 * 
	 * @param isBrief true- use brief options, false- use full options
	 */
	public static void setSparkPointsToAnalysis(boolean isBrief) {
		System.out.println("[spark] Starting analysis ...");

		HashMap<String, String> opt = null;
		if(isBrief) opt = CallGraphBuilder.getBriefSparkAnalysisOptions();
		else opt = CallGraphBuilder.getFullSparkAnalysisOptions();


		SparkTransformer.v().transform("",opt);

		System.out.println("[spark] Done!");
	}

    /**
     * Load a class into soot.
     * @param name fully qualified class name
     * @param main if true, mark as the main(entry point) class
     * @return the {@link SootMethod} representing the main method.
     */
    public static SootClass loadClass(String name, boolean main) {
        SootClass c = Scene.v().loadClassAndSupport(name);
        c.setApplicationClass();
        if (main) Scene.v().setMainClass(c);
        
        //c.
        
        return c;
    }
}
