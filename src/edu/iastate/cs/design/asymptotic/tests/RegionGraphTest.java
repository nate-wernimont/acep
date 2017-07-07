package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.Region;
import soot.toolkits.graph.pdg.RegionAnalysis;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;

public class RegionGraphTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int count = 0;
		// Scene.v().loadNecessaryClasses();
		// Scene.v().addBasicClass(FPCpackage.a,SIGNATURES);
		SootClass sClass = Scene.v().loadClassAndSupport("Binary");
		sClass.setApplicationClass();
		
		//Scene.v().getPointsToAnalysis().

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("main"))
				continue;
			System.out
			.println("****************"+m.getName()+"*****************");
			LoopInfo LI = new LoopInfo(m);
			JimpleBody b = (JimpleBody) m.retrieveActiveBody();
			BriefUnitGraph graph = new BriefUnitGraph(b);
			//ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
			//HashMutablePDG pdg = new HashMutablePDG(graph);
			RegionAnalysis regionAnalysis = new RegionAnalysis(graph, m, m.getDeclaringClass());
			List<Region> regions = regionAnalysis.getRegions();
			Iterator<Region> regionIter = regions.iterator();
			while (regionIter.hasNext()) {
				Region region = regionIter.next();
				System.out.println("============= Region: "+region.getID()+"=============");
				System.out.println(region.getBlocks().toString());
				System.out.println("=====================================================");
			}
			//System.out.println(pdg.size());
		}
	}

}
