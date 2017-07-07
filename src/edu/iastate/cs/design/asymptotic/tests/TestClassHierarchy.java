package edu.iastate.cs.design.asymptotic.tests;

import java.util.Collection;

import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class TestClassHierarchy {
	
	public static void main(String[] args) {
		// Load all three classes
		SootClass sClassA = Scene.v().loadClassAndSupport("A");
		SootClass sClassB = Scene.v().loadClassAndSupport("B");
		SootClass sClassC = Scene.v().loadClassAndSupport("C");
		// Set all the three classes as application classes
		sClassA.setApplicationClass();
		sClassB.setApplicationClass();
		sClassC.setApplicationClass();
		// Get fastHierarchy
		
		Scene instance = Scene.v();
		FastHierarchy hierarchy = Scene.v().getOrMakeFastHierarchy();
		SootMethod arg1 = sClassA.getMethodByName("run");
		SootMethod run = hierarchy.resolveConcreteDispatch(sClassA, arg1);
		System.out.println (run.retrieveActiveBody().toString());
		
		Collection<SootClass> subClasses = hierarchy.getSubclassesOf(sClassA);
		
		for (SootClass subClass : subClasses) {
			System.out.println(subClass.getName());
		}
	}

}
