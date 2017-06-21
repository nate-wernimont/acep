package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JThrowStmt;
import soot.util.Chain;

public class Instrumenter extends BodyTransformer {
	
	static SootClass printInfo;
	static SootMethod write;
	static SootMethod make;
	static SootMethod close;
	
	static {
		printInfo = Scene.v().forceResolve("edu.iastate.cs.design.asymptotic.machinelearning.calculation.PrintInfo", SootClass.BODIES);
		write = Scene.v().getMethod("<edu.iastate.cs.design.asymptotic.machinelearning.calculation.PrintInfo: void print(java.lang.String)>");
		make = Scene.v().getMethod("<edu.iastate.cs.design.asymptotic.machinelearning.calculation.PrintInfo: void makeBW(java.lang.String)>");
		close = Scene.v().getMethod("<edu.iastate.cs.design.asymptotic.machinelearning.calculation.PrintInfo: void close()>");
	}
	
	protected void internalTransform(Body body, String phase, Map options) {
		Chain<Unit> units = body.getUnits();
		Iterator<Unit> unitIter = units.snapshotIterator();
		Unit firstNonIdentity = findFirstNonIdentity(units);
		Stmt closeBW = null;
		if(body.getMethod().equals(Scene.v().getMainMethod())){
			Stmt makeBW = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(make.makeRef(), StringConstant.v(Scene.v().getMainClass().getShortName())));
			closeBW = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(close.makeRef()));
			units.insertBefore(makeBW, firstNonIdentity);
		}
		
		
		while(unitIter.hasNext()) {
			Unit u = unitIter.next();
			InvokeExpr invExpr = Jimple.v().newStaticInvokeExpr(write.makeRef(), StringConstant.v(body.getMethod()+
					PrintInfo.DIVIDER+u.toString()+
					"\n")); 
			Stmt invStmt = Jimple.v().newInvokeStmt(invExpr);
			System.out.println("Processed a new stmt: "+body.getMethod()+": "+u.toString());
			if(u instanceof JRetStmt || u instanceof JReturnStmt || u instanceof JReturnVoidStmt){
				units.insertBefore(invStmt, u);
			} else if (u instanceof JInvokeStmt || u instanceof JGotoStmt || u instanceof JIfStmt || u instanceof JThrowStmt){
				units.insertBefore(invStmt, u);
			} else if (u instanceof JIdentityStmt){
				units.insertBefore(invStmt, last(firstNonIdentity, u, units));
			} else {
				units.insertAfter(invStmt, last(firstNonIdentity, u, units));
			}
		}
		
		if(closeBW != null){
			units.insertBefore(closeBW, units.getLast());
		}
	}
	
	/**
	 * Finds the first unit of the original chain that isn't an identity statement
	 * @param units A chain of the units
	 */
	private Unit findFirstNonIdentity(Chain<Unit> units){
		Unit after = units.getFirst();
		while(after instanceof JIdentityStmt){
			after = units.getSuccOf(after);
		}
		return after;
	}
	
	private Unit last(Unit firstNonIdentity, Unit compare, Chain<Unit> units){
		for(Unit unit : units){
			if(unit.equals(firstNonIdentity)){
				return compare;
			} else if(unit.equals(compare)){
				return firstNonIdentity;
			}
		}
		return null;
	}

}
