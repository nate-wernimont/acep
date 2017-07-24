package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nustaq.serialization.FSTConfiguration;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.jimple.internal.JThrowStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
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
	
	@Override
	protected void internalTransform(Body body, String phase, Map options) {
		Chain<Unit> units = body.getUnits();
		List<Block> blocks = new BriefBlockGraph(body).getBlocks();
		Iterator<Block> blockIter = blocks.iterator();
		Unit firstNonIdentity = findFirstNonIdentity(units);
		Stmt closeBW = null;
		if(body.getMethod().equals(Scene.v().getMainMethod())){
			Stmt makeBW = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(make.makeRef(), StringConstant.v(Scene.v().getMainClass().getShortName())));
			closeBW = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(close.makeRef()));
			units.insertBefore(makeBW, firstNonIdentity);
		}
		
		
		while(blockIter.hasNext()) {
			Block b = blockIter.next();
			if(containsReturnOrInvoke(b)){
				//print each unit
				System.out.println(b);
				Unit u = b.getHead();
				while(u != null) {
					InvokeExpr invExpr = Jimple.v().newStaticInvokeExpr(write.makeRef(), StringConstant.v(body.getMethod()+
							PrintInfo.DIVIDER+u.toString()+
							"\n")); 
					Stmt invStmt = Jimple.v().newInvokeStmt(invExpr);
					System.out.println("Processed a new stmt: "+body.getMethod()+": "+u.toString()+": "+b.getIndexInMethod());
					if(u instanceof JRetStmt || u instanceof JReturnStmt || u instanceof JReturnVoidStmt){
						units.insertBefore(invStmt, u);
					} else if (u instanceof JTableSwitchStmt || u instanceof JLookupSwitchStmt || u instanceof JInvokeStmt || u instanceof JGotoStmt || u instanceof JIfStmt || u instanceof JThrowStmt){
						units.insertBefore(invStmt, last(firstNonIdentity, u, units));
					} else if (u instanceof JIdentityStmt){
						units.insertBefore(invStmt, last(firstNonIdentity, u, units));
					} else {
						units.insertAfter(invStmt, last(firstNonIdentity, u, units));
					}
					u = b.getSuccOf(u);
					while(u instanceof JInvokeStmt && (((JInvokeStmt) u).getInvokeExpr().getMethod().equals(write) || ((JInvokeStmt) u).getInvokeExpr().getMethod().equals(make) || ((JInvokeStmt) u).getInvokeExpr().getMethod().equals(close))){
						u = b.getSuccOf(u);
					}
				}
			} else {
				//print block
				InvokeExpr invExpr = Jimple.v().newStaticInvokeExpr(write.makeRef(), StringConstant.v(body.getMethod()+
						PrintInfo.DIVIDER+b.getIndexInMethod()+
						"\n"));
				Stmt invStmt = Jimple.v().newInvokeStmt(invExpr);
				System.out.println("Processed a new stmt: "+body.getMethod()+": "+b.toShortString());
				if(b.getIndexInMethod() == 0){
					b.insertBefore(invStmt, firstNonIdentity);
				} else {
					b.insertBefore(invStmt, b.getHead());
				}
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
	
	private boolean containsReturnOrInvoke(Block block){
		for(Iterator<Unit> unitIter = block.iterator(); unitIter.hasNext();){
			Unit unit = unitIter.next();
			if(unit instanceof JReturnStmt || unit instanceof JReturnVoidStmt || unit instanceof JRetStmt || unit instanceof JInvokeStmt){
				return true;
			} else if(unit instanceof JAssignStmt){
				for(ValueBox vb : unit.getUseBoxes()){
					if(vb.getClass().getSimpleName().equals("LinkedRValueBox")){
						if(vb.getValue() instanceof InvokeExpr){
							return true;
						}
					}
				}
			}
		}
		return false;
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
