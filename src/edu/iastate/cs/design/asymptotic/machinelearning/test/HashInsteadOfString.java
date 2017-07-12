package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.util.HashSet;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.util.Chain;

public class HashInsteadOfString {

	public static void main(String[] args){
		Benchmark bench = new Test("crypt/config.xml");
		Chain<SootClass> classes = Scene.v().getClasses();
		for(SootClass _class : classes){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			for(SootMethod sm : _class.getMethods()){
				Set<Integer> unitHashes = new HashSet<>();
				sm.retrieveActiveBody().getUnits().forEach((v)->unitHashes.add(new Integer(v.hashCode())));
				if(unitHashes.size() != sm.retrieveActiveBody().getUnits().size()){
					throw new Error("Hash size: "+unitHashes.size()+", "+sm.retrieveActiveBody().getUnits().size());
				}
			}
		}
	}
	
}
