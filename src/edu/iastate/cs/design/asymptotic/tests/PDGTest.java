package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootMethod;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.PDGRegion;
import edu.iastate.cs.design.asymptotic.datastructures.CallGraphDFS;
import edu.iastate.cs.design.asymptotic.datastructures.Module;

public class PDGTest {

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
				
				HashMutablePDG pdg = new HashMutablePDG(eug);
				
				List<PDGRegion> regions = pdg.getPDGRegions();
				
				for (PDGRegion pdgRegion : regions) {
					//System.out.println (pdgRegion.toString());
					for (PDGNode pdgNode : pdgRegion.getNodes()) {
						System.out.println (pdgNode.toShortString()+": dependents := ");
						for (PDGNode dependent : pdgNode.getDependets()) {
							System.out.println (dependent.toShortString());
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
