package edu.iastate.cs.design.asymptotic.tests;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import soot.SootMethod;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.interfaces.impl.Analysis;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Avrora;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Batik;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Eclipse;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Fop;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.H2;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Jython;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Luindex;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Lusearch;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Pmd;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Spec;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Sunflow;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Tomcat;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Tradebeans;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Tradesoap;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Xalan;

public class AnalysisTest {

	private static final String FS = File.separator;
	
	public static void main(String[] args) {
		//LoadBenchmark benchmark = new LoadBenchmark();
		//Avrora benchmark = new Avrora();
		//Batik batik = new Batik();
		//Fop fop = new Fop();
		//Lusearch lusearch = new Lusearch();
		//Xalan xalan = new Xalan();
		//Pmd pmd = new Pmd();
		//Specjvm2008 spec = new Specjvm2008();
		/*
		 * CallGraphDFS cg = benchmark.getCallGraphDFS(); List<SootMethod>
		 * methods = cg.buildDFSFunctionTree();
		 * 
		 * System.out.println(
		 * "===================================================================================="
		 * ); // display methods
		 * 
		 * Iterator<SootMethod> methodIt = methods.iterator(); while
		 * (methodIt.hasNext()) { SootMethod m = (SootMethod) methodIt.next();
		 * if (!m.getDeclaringClass().isApplicationClass())
		 * System.out.println(m.getName() + "(" + m.getDeclaringClass()+")");
		 * 
		 * }
		 */
		/*Analysis acet = new Analysis(benchmark.main(), benchmark
				.getCallGraphDFS());*/
		Benchmark benchmark;
		long startTime = System.currentTimeMillis();
		if (args.length < 1) {
			System.out.println ("No benchmark supplied....");
			return;
		}
		String b = args[0];
		if (b.equalsIgnoreCase("avrora")) {
			benchmark = new Avrora();
		} else if (b.equalsIgnoreCase("batik")) {
			benchmark = new Batik();
		} else if (b.equalsIgnoreCase("eclipse")) {
			benchmark = new Eclipse();
		} else if (b.equalsIgnoreCase("fop")) {
			benchmark = new Fop();
		} else if (b.equalsIgnoreCase("h2")) {
			benchmark = new H2();
		} else if (b.equalsIgnoreCase("jython")) {
			benchmark = new Jython();
		} else if (b.equalsIgnoreCase("luindex")) {
			benchmark = new Luindex();
		} else if (b.equalsIgnoreCase("lusearch")) {
			benchmark = new Lusearch();
		} else if (b.equalsIgnoreCase("pmd")) {
			benchmark = new Pmd();
		} else if (b.equalsIgnoreCase("sunflow")) {
			benchmark = new Sunflow();
		} else if (b.equalsIgnoreCase("tomcat")) {
			benchmark = new Tomcat();
		} else if (b.equalsIgnoreCase("tradebeans")) {
			benchmark = new Tradebeans();
		} else if (b.equalsIgnoreCase("tradesoap")) {
			benchmark = new Tradesoap();
		} else if (b.equalsIgnoreCase("xalan")) {
			benchmark = new Xalan();
		} else if (b.equalsIgnoreCase("spec")) {
			String bname = "";
			if (args.length > 1) {
				bname += args[1];
				if (args.length > 2) {
					bname += ".";
					bname += args[2];
				}
			}
			benchmark = new Spec(bname);
			
		} else if (b.equalsIgnoreCase("test")) {
			benchmark = new Test();
		} else {
			String config = b + FS + "config.xml";
			benchmark = new Test(config);
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
	    System.out.println("Spark time: "+elapsedTime);
		// Invoke analysis
	    startTime = System.currentTimeMillis();
		Analysis acet = new Analysis(benchmark.main(), benchmark
				.getCallGraph());
		stopTime = System.currentTimeMillis();
		elapsedTime = stopTime - startTime;
	    System.out.println("Analysis time: "+elapsedTime);
	}

}
