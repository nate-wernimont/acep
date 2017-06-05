package edu.iastate.cs.design.asymptotic.tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.iastate.cs.design.asymptotic.datastructures.Interpreter;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.PathGenerator;
import edu.iastate.cs.design.asymptotic.datastructures.UnitInfo;
import edu.iastate.cs.design.asymptotic.interfaces.impl.StaticProfilePassImpl;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.baf.BafBody;
import soot.baf.Inst;
import soot.baf.internal.AbstractOpTypeInst;
import soot.baf.internal.BAddInst;
import soot.baf.internal.BLoadInst;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;

public class CycleCountTest {
	public static void main(String[] args) {
		int count = 0;
		
		System.out.println(System.getProperty("sun.arch.data.model"));
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		/*SootClass sClass = Scene.v().loadClassAndSupport("permute");
		sClass.setApplicationClass();
		*/
		LoadBenchmark benchmark = new LoadBenchmark();
		StaticProfilePassImpl spp = benchmark.getStaticProfile();//new StaticProfilePassImpl();
		Iterator<SootMethod> methodIt = spp.getLoadedModule().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("indexDocs"))
				continue;
			System.out
			.println("****************"+m.getName()+"*****************");
			
			LoopInfo LI = new LoopInfo(m);
			// display all paths for this method
			PathGenerator pathGen = new PathGenerator (m,LI);
			// Let's get the blockfrequencies

			// HashMap<Block, Double> frequencies = spp.getMethodBlockFrequencies(m);
			
			// Baf (bytecode) graph construction
			BafBody body = new BafBody(m.retrieveActiveBody(), (Map)null);
			CompleteBlockGraph c = new CompleteBlockGraph(body);
			
			// Block (source code) graph construction
			JimpleBody b = (JimpleBody) m.retrieveActiveBody();
			CompleteBlockGraph cGraph = new CompleteBlockGraph(b);
			/*
			List<Block> bafBlocks = c.getBlocks();
			List<Block> srcBlocks = cGraph.getBlocks();
			*/
			Iterator<Block> bafBlockIter = c.iterator();
			Iterator<Block> srcBlockIter = cGraph.iterator();
			Interpreter interpreter = new Interpreter();
			while(bafBlockIter.hasNext() && srcBlockIter.hasNext()) {
				Block bafBlock = bafBlockIter.next();
				Block srcBlock = srcBlockIter.next();
				double blockTime = 0.0;
				for (Iterator<Unit> unitIter = bafBlock.iterator(); unitIter.hasNext();) {
					Unit unit = unitIter.next();
					UnitInfo info = new UnitInfo(unit);
					double cycles = interpreter.process_unit(info);
					blockTime += cycles;
					// System.out.println ("Unit = "+unit.toString()+"---------- Cycles = "+cycles);
				}
				// Multiply blockTime with blockfrequency to get the effective time
				blockTime = spp.getMethodBlockFrequency(m, srcBlock) * blockTime;
				
				System.out.println ("\nBlock = "+srcBlock.toString()+ " Cycles = "+blockTime);
			}
			System.out.println("Done.... Congrats");
		}
	}
}
