package edu.iastate.cs.design.asymptotic.branching.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.baf.BafBody;
import soot.baf.internal.BInterfaceInvokeInst;
import soot.baf.internal.BSpecialInvokeInst;
import soot.baf.internal.BStaticInvokeInst;
import soot.baf.internal.BVirtualInvokeInst;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

public class BlockTesting {

	public static void main(String[] args){
		String _classpath = "C:/Users/Nate/Desktop/Iowa State/Spring 2017/COM S 228/edu.iastate.cs228.hw4/bin";

		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		Scene.v().loadNecessaryClasses();
		Options.v().set_whole_program(true);
		Scene.v().addBasicClass("edu.iastate.cs228.hw4.Dictionary",SootClass.SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("edu.iastate.cs228.hw4.Dictionary");
		sClass.setApplicationClass();
		
		Iterator<SootMethod> methIter = sClass.methodIterator();
		while(methIter.hasNext()){
			SootMethod meth = methIter.next();
			System.out.println("==================Evaluating: "
					+ meth.toString() + "=====================");
			Body b = meth.getActiveBody();
			BlockGraph bg = new BriefBlockGraph(b);
			System.out.println("=======Blocks=======");
			for(Block block: bg.getBlocks()){
				System.out.println("===Block: "+block.toShortString()+"===");
				System.out.println(block.toString());
			}
			
			UnitGraph ug = new BriefUnitGraph(b);
			System.out.println("======Units======");
			Iterator<Unit> unitIter = ug.iterator();
			while(unitIter.hasNext()){
				Unit unit = unitIter.next();
				System.out.println("===Unit===");
				System.out.println(unit.toString());
			}
			
		}
		
	}
	
}
