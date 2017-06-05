package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import soot.Body;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.FieldRef;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.RWSet;
import soot.jimple.toolkits.pointer.SideEffectAnalysis;
import soot.util.Chain;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Module;

public class SideEffectAnalysisTest {
	
	public class UniqueRWSets {
		public ArrayList<RWSet> l = new ArrayList();

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
	
	
	public static void main(String[] args) {
		try {
			// SootClass sClass = Scene.v().loadClassAndSupport("loop");
			// sClass.setApplicationClass();
			Module module = new Module("Flow");
			module.init();
			
			Chain<SootField> fields = Scene.v().getMainClass().getFields();
			
			for (SootField sootField : fields) {
				System.out.println(sootField.toString());
			}
			// _module = m;
			CallGraphDFS CG = new CallGraphDFS(module.main());
			List<SootMethod> methods = CG.buildDFSFunctionTree();

			PointsToAnalysis pointsToAnalysis = null;
			if (Scene.v().hasPointsToAnalysis()) {
				pointsToAnalysis = Scene.v().getPointsToAnalysis();
			} else {
				System.out.println("No pointsToset ...");
				return;
			}

			Iterator methodIt = methods.iterator();
			while (methodIt.hasNext()) {
				SootMethod m = (SootMethod) methodIt.next();
				if (!m.getName().equals("foo"))
					continue;
				JimpleBody body = (JimpleBody) m.retrieveActiveBody();

				//Body body = m.retrieveActiveBody();
				/*System.out.println("=======================================");
				System.out.println(body.toString());
				System.out.println("=======================================");*/
				//DumbPointerAnalysis.v()
				SideEffectAnalysis sea = new SideEffectAnalysis(
						pointsToAnalysis, Scene.v().getCallGraph());
				sea.findNTRWSets(body.getMethod());
				
				HashMap stmtToReadSet = new HashMap();
				HashMap stmtToWriteSet = new HashMap();
				
				System.out.println("============================");
				HashMap<RWSet, Object> rwToStmt = new HashMap<RWSet, Object>();
				//ArrayList<RWSet> sets = new ArrayList<RWSet>();
				SideEffectAnalysisTest seaTest = new SideEffectAnalysisTest();
				UniqueRWSets sets = seaTest.new UniqueRWSets();
				for (Iterator stmtIt = body.getUnits().iterator(); stmtIt
						.hasNext();) {
					final Stmt stmt = (Stmt) stmtIt.next();
					
					if (stmt.containsFieldRef()) {
						System.out.println("=================="+stmt.toString()+"==============");
						FieldRef fieldRef = stmt.getFieldRef();
						System.out.println(fieldRef.getField());
					}
					
					Object key = stmt.toString();
					//RWSet rwSet = sea.readSet(m, stmt);
					if (!stmtToReadSet.containsKey(key)) {
						RWSet rwSet = sets.getUnique( sea.readSet( body.getMethod(), stmt ) );
						stmtToReadSet.put( key,rwSet);
						rwToStmt.put(rwSet, key);
					}
					//rwSet = sea.writeSet(m, stmt);
					if (!stmtToWriteSet.containsKey(key)) {
						RWSet rwSet = sets.getUnique( sea.writeSet( body.getMethod(), stmt ) );
						stmtToWriteSet.put( key, rwSet);
						rwToStmt.put(rwSet, key);
					}

				}

				for (Iterator outerIt = sets.iterator(); outerIt.hasNext();) {
					final RWSet outer = (RWSet) outerIt.next();

					for (Iterator innerIt = sets.iterator(); innerIt.hasNext();) {

						final RWSet inner = (RWSet) innerIt.next();
						if (inner == outer)
							break;
						if (outer.hasNonEmptyIntersection(inner)) {
							//System.out.println(outer.toString()+"---------"+inner.toString());
							//System.out.println(rwToStmt.get(outer)+"--->"+rwToStmt.get(inner));
						}
					}
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

	}

}
