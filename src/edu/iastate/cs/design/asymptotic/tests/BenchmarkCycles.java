package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.SootMethod;
import soot.Unit;
import soot.baf.BafBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Interpreter;
import edu.iastate.cs.design.asymptotic.datastructures.UnitInfo;

public class BenchmarkCycles {
	
	public static void main(String[] args) {
		LoadBenchmark benchmark = new LoadBenchmark();
		CallGraphDFS dfs = benchmark.getCallGraphDFS();
		List<SootMethod> methods = dfs.buildDFSFunctionTree();
		Iterator<SootMethod> methodIt = methods.iterator();
		
		while (methodIt.hasNext()) {
			System.out.println ("====================================================================================");
			SootMethod m = methodIt.next();
			System.out.println(m.getName()+"-------------------"+m.getDeclaringClass());
			
			if (!m.hasActiveBody())
				continue;
			
			// Baf (bytecode) graph construction
			BafBody body = new BafBody(m.retrieveActiveBody(), (Map)null);
			CompleteBlockGraph c = new CompleteBlockGraph(body);
			
			Iterator<Block> bafBlockIter = c.iterator();
			Interpreter interpreter = new Interpreter();
			while(bafBlockIter.hasNext()) {
				Block bafBlock = bafBlockIter.next();
				double blockTime = 0.0;
				for (Iterator<Unit> unitIter = bafBlock.iterator(); unitIter.hasNext();) {
					Unit unit = unitIter.next();
					UnitInfo info = new UnitInfo(unit);
					double cycles = interpreter.process_unit(info);
					blockTime += cycles;
					// System.out.println ("Unit = "+unit.toString()+"---------- Cycles = "+cycles);
				}
				System.out.println ("\nBlock = "+bafBlock.toString()+ " Cycles = "+blockTime);
			}
			
		}
	}

}
