package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JimpleLocal;
import soot.util.Chain;

public class Instrumenter extends BodyTransformer {
	
	private static final String FILE_LOCATION = "/Users/natemw/Documents/acep/profilingOutput/";
	static SootClass file;
	static SootMethod fileInit;
	static SootMethod fileCreator;
	static SootClass writer;
	static SootClass fileWriter;
	static SootMethod fileWriterInit;
	static SootClass bufferedWriter;
	static SootMethod bufferedWriterInit;
	static SootMethod printer;
	static SootMethod close;
	
	static {
		file = Scene.v().forceResolve("java.io.File", SootClass.BODIES);
		fileInit = Scene.v().getMethod("<java.io.File: void <init>(java.lang.String)>");
		fileCreator = Scene.v().getMethod("<java.io.File: boolean createNewFile()>");
		writer = Scene.v().forceResolve("java.io.Writer", SootClass.BODIES);
		fileWriter = Scene.v().forceResolve("java.io.FileWriter", SootClass.BODIES);
		fileWriterInit = Scene.v().getMethod("<java.io.FileWriter: void <init>(java.io.File)>");
		bufferedWriter = Scene.v().forceResolve("java.io.BufferedWriter", SootClass.BODIES);
		bufferedWriterInit = Scene.v().getMethod("<java.io.BufferedWriter: void <init>(java.io.Writer)>");
		printer = Scene.v().getMethod("<java.io.Writer: void write(java.lang.String)>");
		close = Scene.v().getMethod("<java.io.Writer: void close()>");
	}
	
	protected void internalTransform(Body body, String phase, Map options) {
		Chain<Unit> units = body.getUnits();
		Iterator<Unit> unitIter = units.snapshotIterator();
		Unit firstNonIdentity = findFirstNonIdentity(units);
		
		Local fileLocal = Jimple.v().newLocal("file", RefType.v(file));
		Local fileWriterLocal = Jimple.v().newLocal("fileWriter", RefType.v(fileWriter));
		Local bufferedWriterLocal = Jimple.v().newLocal("bufferedWriter", RefType.v(writer));
		body.getLocals().add(fileLocal);
		body.getLocals().add(fileWriterLocal);
		body.getLocals().add(bufferedWriterLocal);
		Stmt fileCreation = Jimple.v().newAssignStmt(fileLocal, new JNewExpr(RefType.v(file)));
		Stmt fileInitialization = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(fileLocal, fileInit.makeRef(), StringConstant.v(FILE_LOCATION+"blah.txt")));
		Stmt makeNewFile = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(fileLocal, fileCreator.makeRef()));
		Stmt fileWriterCreation = Jimple.v().newAssignStmt(fileWriterLocal, new JNewExpr(RefType.v(fileWriter)));
		Stmt fileWriterInitialization = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(fileWriterLocal, fileWriterInit.makeRef(), fileLocal));
		Stmt bufferedWriterCreation = Jimple.v().newAssignStmt(bufferedWriterLocal, new JNewExpr(RefType.v(bufferedWriter)));
		Stmt bufferedWriterInitialization = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(bufferedWriterLocal, bufferedWriterInit.makeRef(), fileWriterLocal));
		Stmt closeWriter = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(bufferedWriterLocal, close.makeRef()));
		
		units.insertBefore(fileCreation, firstNonIdentity);
		units.insertBefore(fileInitialization, firstNonIdentity);
		units.insertBefore(makeNewFile, firstNonIdentity);
		units.insertBefore(fileWriterCreation, firstNonIdentity);
		units.insertBefore(fileWriterInitialization, firstNonIdentity);
		units.insertBefore(bufferedWriterCreation, firstNonIdentity);
		units.insertBefore(bufferedWriterInitialization, firstNonIdentity);
		
		while(unitIter.hasNext()) {
			Unit u = unitIter.next();
			InvokeExpr invExpr = Jimple.v().newVirtualInvokeExpr(bufferedWriterLocal, printer.makeRef(), StringConstant.v(body.getMethod().getDeclaringClass().getShortName()+
					": "+body.getMethod().getName()+
					": "+u.toString()+
					"\n")); 
			Stmt invStmt = Jimple.v().newInvokeStmt(invExpr);
			System.out.println("Made a new stmt: "+body.getMethod()+": "+u.toString());
			if(u instanceof JRetStmt || u instanceof JReturnStmt || u instanceof JReturnVoidStmt){
				units.insertBefore(invStmt, u);
			} else {
				units.insertAfter(invStmt, last(firstNonIdentity, u, units));//TODO: Work on this bit
			}
		}
		
		units.insertBefore(closeWriter, units.getLast());
		
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
	
	private Unit last(Unit firstNonIdentity, Unit compare, Unit toAdd, Chain<Unit> units){
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
