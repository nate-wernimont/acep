package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Module;

public class DefUseTest {

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
				pointsToAnalysis = Scene.v().getPointsToAnalysis();
			} else {
				System.out.println("No pointsToset ...");
				return;
			}

			Iterator methodIt = methods.iterator();
			while (methodIt.hasNext()) {
				SootMethod m = (SootMethod) methodIt.next();
				if (!m.getName().equals("main"))
					continue;
				// JimpleBody b = (JimpleBody) m.retrieveActiveBody();

				Body body = m.retrieveActiveBody();

				ExceptionalUnitGraph eug = new ExceptionalUnitGraph(body);

				for (Iterator stmtIt = body.getUnits().iterator(); stmtIt
						.hasNext();) {
					final Stmt stmt = (Stmt) stmtIt.next();
					// Print all defs
					System.out.println ("=============== Defs ================");
					for (ValueBox vb : (List<ValueBox>) stmt
							.getDefBoxes()) {
						Value v = vb.getValue();
						System.out.println (v.toString());
					}
					
					System.out.println ("=============== Uses ================");
					for (ValueBox vb : (List<ValueBox>) stmt
							.getUseBoxes()) {
						Value v = vb.getValue();
						System.out.println (v.toString());
					}
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

	}
}
