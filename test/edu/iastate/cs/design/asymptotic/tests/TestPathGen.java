package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import java.util.List;

import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.PathGenerator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

public class TestPathGen {

	public static void main(String[] args) {
		int count = 0;
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		LoadBenchmark benchmark = new LoadBenchmark();
		// SootClass sClass = Scene.v().loadClassAndSupport("DocumentsWriter");
		Chain<SootClass> classes = Scene.v().getApplicationClasses();
		SootClass sClass = null;
		for (SootClass sootClass : classes) {
			//System.out.println (sootClass.getName());
			if (sootClass.getName().equals("org.apache.lucene.index.IndexWriter")) {
				sClass = sootClass;
				break;
			}
		}
		
		if (sClass == null)
			return;

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("applyDeletes"))
				continue;
			LoopInfo LI = new LoopInfo(m);
			PathGenerator pathGen = new PathGenerator (m, LI);
		}
	}
}
