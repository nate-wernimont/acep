package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Module;

import soot.Body;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.RWSet;

public class PointsToAnalysisTest {

	public static class UniqueRWSets {
		public static ArrayList<RWSet> l = new ArrayList();

		RWSet getUnique(RWSet s) {
			if (s == null)
				return s;
			for (Iterator retIt = l.iterator(); retIt.hasNext();) {
				final RWSet ret = (RWSet) retIt.next();
				if (ret.isEquivTo(s))
					return ret;
			}
			l.add(s);
			return s;
		}

		Iterator iterator() {
			return l.iterator();
		}

		short indexOf(RWSet s) {
			short i = 0;
			for (Iterator retIt = l.iterator(); retIt.hasNext();) {
				final RWSet ret = (RWSet) retIt.next();
				if (ret.isEquivTo(s))
					return i;
				i++;
			}
			return -1;
		}
	}

	/*
	 * protected Object keyFor(Stmt s) { if (s.containsInvokeExpr()) { Iterator
	 * it = Scene.v().getCallGraph().edgesOutOf(s); if (!it.hasNext()) { return
	 * Collection<E>.EMPTY_LIST; } ArrayList ret = new ArrayList(); while
	 * (it.hasNext()) { ret.add(it.next()); } return ret; } else { return s; } }
	 */

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// SootClass sClass = Scene.v().loadClassAndSupport("loop");
			// sClass.setApplicationClass();
			Module module = new Module("Flow");
			module.init();
			// _module = m;
			CallGraphDFS CG = new CallGraphDFS(module.main());
			List<SootMethod> methods = CG.buildDFSFunctionTree();

			PointsToAnalysis pointsToAnalysis = null;
			if (Scene.v().hasPointsToAnalysis()) {
				pointsToAnalysis = DumbPointerAnalysis.v();//Scene.v().getPointsToAnalysis();
			} else {
				System.out.println("No pointsToset ...");
				return;
			}

			HashMap<JimpleLocal, PointsToSet> pointsTo = new HashMap<JimpleLocal, PointsToSet>();

			Iterator methodIt = methods.iterator();
			while (methodIt.hasNext()) {
				SootMethod m = (SootMethod) methodIt.next();
				if (!m.getName().equals("main"))
					continue;
				// JimpleBody b = (JimpleBody) m.retrieveActiveBody();

				Body b = m.retrieveActiveBody();
				ArrayList<JimpleLocal> variables = new ArrayList<JimpleLocal>();
				for (Iterator stmtIt = b.getUnits().iterator(); stmtIt
						.hasNext();) {
					final Stmt stmt = (Stmt) stmtIt.next();
					for (ValueBox vb : stmt
							.getUseBoxes()) {
						Value v = vb.getValue();
						if (v instanceof JimpleLocal) {
							JimpleLocal value = (JimpleLocal) v;
							variables.add(value);
							/*if (pointsToAnalysis != null) {
								PointsToSet valueSet = pointsToAnalysis
										.reachingObjects(value);
								pointsTo.put(value, valueSet);
								// System.out.println(valueSet.possibleTypes());
							}*/
						}
					}
				}

				for (JimpleLocal jimpleLocal : variables) {
					if (pointsToAnalysis != null) {
						PointsToSet valueSet = pointsToAnalysis
								.reachingObjects(jimpleLocal);
						pointsTo.put(jimpleLocal, valueSet);
						// System.out.println(valueSet.possibleTypes());
					}
				}
				
				for (Entry<JimpleLocal, PointsToSet> entryOut : pointsTo
						.entrySet()) {
					for (Entry<JimpleLocal, PointsToSet> entryIn : pointsTo
							.entrySet()) {
						if (entryOut.equals(entryIn)) continue;
						if (entryOut.getValue().hasNonEmptyIntersection(
								entryIn.getValue())) {
							System.out.println(entryOut.getKey().getName()
									+ "------------"
									+ entryIn.getKey().getName());
						}
					}
				}
			}

			/*
			 * SideEffectAnalysis sea = sea =
			 * Scene.v().getSideEffectAnalysis();;
			 * 
			 * 
			 * Iterator methodIt = methods.iterator(); while
			 * (methodIt.hasNext()) { SootMethod m = (SootMethod)
			 * methodIt.next(); if (!m.getName().equals("main")) continue;
			 * //JimpleBody b = (JimpleBody) m.retrieveActiveBody();
			 * 
			 * Body b = m.retrieveActiveBody();
			 * 
			 * sea.findNTRWSets(m); HashMap<Stmt, RWSet> stmtToReadSet = new
			 * HashMap<Stmt, RWSet>(); HashMap<Stmt, RWSet> stmtToWriteSet = new
			 * HashMap<Stmt, RWSet>(); ArrayList<RWSet> sets = new
			 * ArrayList<RWSet>();
			 * 
			 * System.out.println("Starting side-effect analysis"); for
			 * (Iterator stmtIt = b.getUnits().iterator(); stmtIt .hasNext();) {
			 * final Stmt stmt = (Stmt) stmtIt.next(); if
			 * (!stmtToReadSet.containsKey(stmt)) { RWSet rwSet = null; rwSet =
			 * sea.readSet(m, stmt); if (rwSet != null) {
			 * stmtToReadSet.put(stmt, rwSet); if (!sets.contains(rwSet))
			 * sets.add(rwSet); }
			 * 
			 * rwSet = sea.writeSet(m, stmt); if (rwSet != null) {
			 * stmtToWriteSet.put(stmt, rwSet); if (!sets.contains(rwSet))
			 * sets.add(rwSet); } } }
			 * 
			 * // Let's compute some dependencies for (Iterator outerIt =
			 * sets.iterator(); outerIt.hasNext();) { final RWSet outer =
			 * (RWSet) outerIt.next();
			 * 
			 * for (Iterator innerIt = sets.iterator(); innerIt.hasNext();) {
			 * 
			 * final RWSet inner = (RWSet) innerIt.next(); if (inner == outer)
			 * break; if (outer.hasNonEmptyIntersection(inner)) { //
			 * graph.addEdge(sets.indexOf(outer), // sets.indexOf(inner));
			 * System.out.println(sets.indexOf(outer) + "----->" +
			 * sets.indexOf(inner)); } } } // ExceptionalUnitGraph eug = new
			 * ExceptionalUnitGraph(b);
			 * 
			 * 
			 * // BlockGraph bGraph = new BlockGraph(eug); CompleteBlockGraph
			 * cGraph = new CompleteBlockGraph(b);
			 * 
			 * // JimpleBody body = Jimple.v().newBody(m); // Run through all
			 * the basic blocks if (b != null) { // ExceptionalUnitGraph eug =
			 * new ExceptionalUnitGraph(b); // Iterator<Unit> units =
			 * eug.iterator(); Iterator<Block> blockIter = cGraph.iterator();
			 * while (blockIter.hasNext()) { Block blk = blockIter.next();
			 * Iterator<Unit> blkUnitsIter = blk.iterator(); while
			 * (blkUnitsIter.hasNext()) { Unit unit = blkUnitsIter.next(); // if
			 * (unit instanceof JIfStmt) { for (ValueBox vb : (List<ValueBox>)
			 * unit .getUseAndDefBoxes()) { Value v = vb.getValue(); if (v
			 * instanceof JimpleLocal) { JimpleLocal value = (JimpleLocal) v; if
			 * (pointsToAnalysis != null) { PointsToSet valueSet =
			 * pointsToAnalysis .reachingObjects(value); System.out
			 * .println(valueSet.possibleTypes()); } } else { // For now, do
			 * nothing }
			 * 
			 * } // } } } }
			 * 
			 * }
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
