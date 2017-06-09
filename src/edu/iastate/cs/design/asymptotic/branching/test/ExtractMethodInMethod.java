package edu.iastate.cs.design.asymptotic.branching.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.branching.BlockPath;
import edu.iastate.cs.design.asymptotic.branching.PathEnumerator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.InvokeExprBox;
import soot.jimple.internal.JInvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.Block;

public class ExtractMethodInMethod {

	public static void main(String[] args) {
		String _classpath = "/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/rt.jar:/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/jce.jar:/Users/natemw/Documents/acep/bin";

		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		System.out.println(Options.v().soot_classpath());
		Scene.v().loadNecessaryClasses();
		Options.v().set_whole_program(true);
		SootClass sClass = Scene.v().forceResolve("edu.iastate.cs.design.asymptotic.branching.test.example.MethodsIntermingling", SootClass.BODIES);
		sClass.setApplicationClass();
		
		
		PathEnumerator pathE = new PathEnumerator(sClass);
		pathE.findMethodPaths();
		HashMap<SootMethod, List<BlockPath>> map = pathE.getMap();
		Set<SootMethod> keys = map.keySet();
		for(SootMethod meth : keys ){
			List<BlockPath> pathForMeth = map.get(meth);
			System.out.println(meth.toString());
			System.out.println(pathForMeth.toString());
			System.out.println("===Block===");
			for(BlockPath blPath : pathForMeth){
				for(Block bl : blPath.toList()){
					System.out.println(bl.toString());
					System.out.println("==Units==");
					for(Iterator<Unit> unitIter = bl.iterator(); unitIter.hasNext();){
						Unit unit = unitIter.next();
						for(ValueBox vb : unit.getUseBoxes()){
							if(vb instanceof InvokeExprBox){
								JInvokeStmt st = new JInvokeStmt(vb.getValue());
								//What can I do with that?
							}
						}
//						System.out.println("Unit: "+unit.toString());
//						for(ValueBox vb : unit.getUseBoxes()){
//							System.out.println("\tValueBox tag: "+vb.getClass().getSimpleName());
//							if(vb.getClass().getSimpleName().equals("InvokeExprBox")){
//								InvokeExprBox ieb = (InvokeExprBox) vb;
//								System.out.println(ieb.getTags());
//							}
//						}
					}
				}
			}
			
		}
	}

}
