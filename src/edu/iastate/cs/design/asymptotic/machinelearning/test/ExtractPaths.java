package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.FeatureStatistic;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.options.Options;
import soot.toolkits.graph.Block;

public class ExtractPaths {

	public static void main(String[] args) {
		String _classpath = "/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/rt.jar:/Library/Java/JavaVirtualMachines/1.6.0_37-b06-434.jdk/Contents/Home/lib/jce.jar:/Users/natemw/Documents/workspace/Test/bin";

		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		System.out.println(Options.v().soot_classpath());
		Scene.v().loadNecessaryClasses();
		Options.v().set_whole_program(true);
		SootClass sClass = Scene.v().forceResolve("Test.alg", SootClass.BODIES);
		sClass.setApplicationClass();

		// PathEnumerator pathFinder = new PathEnumerator(sClass);
		// pathFinder.run();
		//// System.out.println(sClass.getFields());
		//// for(Iterator<SootField> iter =
		// sClass.getFields().snapshotIterator(); iter.hasNext();){
		//// SootField sf = iter.next();
		//// }
		//
		// HashMap<SootMethod, List<Path<Unit>>> map = pathFinder.getUnitMap();
		//// Set<SootMethod> keys = map.keySet();
		// HashMap<Path<Unit>, FeatureStatistic> features =
		// pathFinder.getFeatureStatistics();
		for (SootMethod meth : sClass.getMethods()) {
			// List<Path<Unit>> pathForMeth = map.get(meth);
			System.out.println(meth.toString());
			// System.out.println(pathForMeth.toString());
			for (Unit unit : meth.retrieveActiveBody().getUnits()) {
				// FeatureStatistic fc = features.get(path);
				// System.out.println(path);
				// System.out.println(fc);
				// for (Unit unit : path.getElements()){
				System.out.println("====Unit====");
				System.out.println(unit.toString());
				// System.out.println(unit.getTags());
				System.out.println(unit.getClass());
				System.out.println(unit.getDefBoxes());
				System.out.println(unit.getUseAndDefBoxes());
				System.out.println(unit.getUnitBoxes());
				System.out.println(unit.getUseBoxes());
				for (ValueBox vb : unit.getUseBoxes()) {
					System.out.println(vb.getClass());
					System.out.println(vb.getValue());
					System.out.println(vb.getValue().getType());
					System.out.println(vb.getValue().getUseBoxes());
					System.out.println(vb.getValue().getClass());
					// for(Object useBox : vb.getValue().getUseBoxes()){
					// if(useBox.getClass().getName().equals("soot.jimple.internal.ImmediateBox")){
					// System.out.println("===Immediate Box===");
					// ImmediateBox ib = (ImmediateBox) useBox;
					// System.out.println(ib.getValue().getClass().getName());
					// }
					// }
				}
				if (unit instanceof InvokeStmt) {
					JInvokeStmt jis = (JInvokeStmt) unit;
					System.out.println(jis.getInvokeExpr().getMethod());
				}
				// }
			}
		}
	}

}
