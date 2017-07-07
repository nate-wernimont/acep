package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;

public class CallGraphExample {
	public static void main(String[] args) {

		List<String> argsList = new ArrayList();
		argsList.addAll(Arrays.asList(new String[] { "-w", "-app", "-p",
				"cg.spark", "enabled", "-include", "org.apache.", "-include",
				"org.w3c.", "-main-class", "BST", "-d",
				"/home/gupadhyaya/tamiflex/out", "Harness" }));
		PackManager.v().getPack("wjtp").add(
				new Transform("wjtp.myTrans", new SceneTransformer() {

					@Override
					protected void internalTransform(String phaseName,
							Map options) {
						CHATransformer.v().transform();
						SootClass a = Scene.v().getSootClass("BST");
						Scene.v().loadNecessaryClasses();
						SootMethod onCreate = Scene.v().getMainClass()
								.getMethodByName("main");
						CallGraph cg = Scene.v().getCallGraph();
						/*
						 * Iterator<MethodOrMethodContext> targets = new
						 * Targets( cg.edgesOutOf(onCreate));
						 */
						/*
						 * while (targets.hasNext()) { SootMethod tgt =
						 * (SootMethod) targets.next();
						 * System.out.println(onCreate + " may call " + tgt); }
						 */
					}
				}));
		String[] paras = new String[0];
		paras = argsList.toArray(new String[0]);
		System.out.print("before soot.Main.main\n");
		Scene
				.v()
				.setSootClassPath(
						".:/usr/lib/jvm/java-6-openjdk/jre/lib/rt.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/jce.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/jsse.jar:/usr/lib/jvm/java-6-openjdk/jre/lib::/home/gupadhyaya/tamiflex/out");
		soot.Main.main(paras);
	}
}