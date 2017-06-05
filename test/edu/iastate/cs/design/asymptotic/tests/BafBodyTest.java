package edu.iastate.cs.design.asymptotic.tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;

public class BafBodyTest {
	
	public static void main(String[] args) {
		int count = 0;
		
		//System.out.println(System.getProperty("sun.arch.data.model"));
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("test");
		sClass.setApplicationClass();

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("main"))
				continue;
			System.out
			.println("****************"+m.getName()+"*****************");
			BafBody body = new BafBody(m.retrieveActiveBody(), (Map)null);
			
			CompleteBlockGraph c = new CompleteBlockGraph(body);
			Iterator<Block> blockIter = c.iterator();
			while(blockIter.hasNext()) {
				Block block = blockIter.next();
				for (Iterator<Unit> unitIter = block.iterator(); unitIter.hasNext();) {
					Unit unit = unitIter.next();
					System.out.println(unit.toString());
					/*Iterator<UnitBox> unitBoxIter = unit.getUnitBoxes().iterator();
					while (unitBoxIter.hasNext()) {
						UnitBox box = unitBoxIter.next();
						Unit innerUnit = box.getUnit();
						if (innerUnit instanceof Inst)
							System.out.println ("Inner instruction: "+innerUnit.toString());
					}
					if (unit instanceof BLoadInst) {
						BLoadInst unitInst = (BLoadInst) unit;
						System.out.println("Unit name: "+unitInst.getName());
						if (unit instanceof AbstractOpTypeInst) {
							AbstractOpTypeInst inst = (AbstractOpTypeInst) unit;
							System.out.println (inst.getOpType().toString());
						}
					} else if (unit instanceof BAddInst) {
						BAddInst inst = (BAddInst) unit;
						System.out.println();
					}*/
				}
				//System.out.println(block.toString());
				System.out.println("================================================================================");
			}
			// System.out.println(body.getLocalCount());
		}
	}

}
