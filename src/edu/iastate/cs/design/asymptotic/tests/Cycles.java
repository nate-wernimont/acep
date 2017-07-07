package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import java.util.Map;

import edu.iastate.cs.design.asymptotic.datastructures.Interpreter;
import edu.iastate.cs.design.asymptotic.datastructures.UnitInfo;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.baf.BafBody;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;

public class Cycles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
/*		StaticProfilePassImpl spp = new StaticProfilePassImpl("examples.Average");
		Iterator methodIt = spp.getLoadedModule().iterator();*/
		Options.v().set_keep_line_number(true);
		SootClass sClass = Scene.v().loadClassAndSupport("examples.Complex");
		sClass.setApplicationClass();

		// Scene.v().getPointsToAnalysis().

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			/*if (!m.getName().equals("main"))
				continue;*/
			System.out.println("==========================="+ m.getName()+"============================");
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
					System.out.println ("Unit = "+unit.toString()+"---------- Cycles = "+cycles);
				}
/*				// Multiply blockTime with blockfrequency to get the effective time
				double freq = spp.getMethodBlockFrequency(m, srcBlock);
				blockTime = freq * blockTime;
				
				System.out.println ("\nBlock = "+srcBlock.toString()+ " Cycles = "+blockTime+" frequency: "+freq);
				System.out.println ("Freq: "+freq);*/
			}
			System.out.println("Done.... Congrats");
		}

	}

}
