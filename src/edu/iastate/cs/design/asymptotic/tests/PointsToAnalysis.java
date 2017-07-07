package edu.iastate.cs.design.asymptotic.tests;

//package dk.brics.paddle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;

import soot.Body;
import soot.EntryPoints;
import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.FieldRef;
import soot.jimple.JimpleBody;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt; //import soot.jimple.paddle.PaddleTransformer;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.Chain;

public class PointsToAnalysis {

	// Make sure we get line numbers and whole program analysis
	static {
		soot.options.Options.v().set_keep_line_number(true);
		soot.options.Options.v().set_whole_program(true);
		soot.options.Options.v().setPhaseOption("cg", "verbose:true");
	}

	private static SootClass loadClass(String name, boolean main) {
		SootClass c = Scene.v().loadClassAndSupport(name);
		c.setApplicationClass();
		if (main)
			Scene.v().setMainClass(c);
		return c;
	}

	public static void main(String[] args) {
		// loadClass("Item",false);
		// loadClass("Container",false);
		SootClass c = loadClass("test1", true);

		soot.Scene.v().loadNecessaryClasses();
		soot.Scene.v().setEntryPoints(EntryPoints.v().all());

		/*
		 * if (args[0].equals("paddle")) setPaddlePointsToAnalysis(); else if
		 * (args[0].equals("spark"))
		 */
		setSparkPointsToAnalysis();

		/*
		 * SootField f = getField("Container","item"); Map<Local> ls =
		 * getLocals(c,"go","Container");
		 * 
		 * printLocalIntersects(ls); printFieldIntersects(ls,f);
		 */
		soot.PointsToAnalysis pointsTo = DumbPointerAnalysis.v();//null;
		/*if (Scene.v().hasPointsToAnalysis()) {
			pointsTo = Scene.v().getPointsToAnalysis();
		} else {
			System.out.println("Using DumbPointerAnalysis...");
			pointsTo = DumbPointerAnalysis.v();
		}*/
		Iterator methodIt = c.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			/*if (m.getName().equals("<clinit>")) {
				Body b = m.retrieveActiveBody();
				Iterator<Unit> unitsIter = b.getUnits().iterator();
				while (unitsIter.hasNext()) {
					Unit unit = unitsIter.next();
				}
				System.out.println("-------------------------- Body -----------------------------");
				System.out.println(b.toString());
				System.out.println("-------------------------- Body -----------------------------");
				continue;
			}
			if (m.getName().equals("<init>")) {
				Body b = m.retrieveActiveBody();
				System.out.println("-------------------------- Body -----------------------------");
				System.out.println(b.toString());
				System.out.println("-------------------------- Body -----------------------------");
				continue;
			}*/
			if (!m.getName().equals("main"))
				continue;
			System.out.println("****************" + m.getName()
					+ "*****************");
			Body b = m.retrieveActiveBody();
			System.out.println("-------------------------- Body -----------------------------");
			System.out.println(b.toString());
			System.out.println("-------------------------- Body -----------------------------");
			ArrayList<JimpleLocal> variables = new ArrayList<JimpleLocal>();
			for (Iterator stmtIt = b.getUnits().iterator(); stmtIt.hasNext();) {
				final Stmt stmt = (Stmt) stmtIt.next();
				for (ValueBox vb : stmt.getUseBoxes()) {
					Value v = vb.getValue();
					if (v instanceof JimpleLocal) {
						JimpleLocal value = (JimpleLocal) v;
						variables.add(value);
					}
				}
			}
			System.out.println("========================================= Tags ============================================");
			SootClass classVar = m.getDeclaringClass();
			Chain<SootField> fields = classVar.getFields();
			for (SootField sootField : fields) {
				List<Tag> tags = sootField.getTags();
				System.out.println(tags.toString());
			}

			HashMap<JimpleLocal, PointsToSet> pointsToSetMap = new HashMap<JimpleLocal, PointsToSet>();
			for (JimpleLocal jimpleLocal : variables) {
				if (pointsTo != null) {
					PointsToSet valueSet = pointsTo
							.reachingObjects(jimpleLocal);
					pointsToSetMap.put(jimpleLocal, valueSet);
					// System.out.println(valueSet.possibleTypes());
				}
			}

			LoopInfo LI = new LoopInfo(m);
			
			for (Iterator<Loop> loopsIter = LI.iterator(); loopsIter.hasNext();) {
				Loop loop = loopsIter.next();
				Value var = null;
				if (loop.requiresPointsTo()) {
					var = loop.pointsToVariable();
				}

				if (var != null) {
					System.out.println("Yes, time to do some work boss");
					PointsToSet set = null;
					if (var instanceof FieldRef) {
						SootField referred = ((FieldRef) var).getField();
						set = pointsTo.reachingObjects(referred);
					} else if (var instanceof JimpleLocal) {
						set = pointsTo.reachingObjects((JimpleLocal) var);
						/*Set<ClassConstant> constants = set
								.possibleClassConstants();
						for (ClassConstant classConstant : constants) {
							System.out.println(classConstant.getValue());
						}*/
					}
					
					// check if inside method there is some assigment to this varibale
					for (Iterator stmtIt = b.getUnits().iterator(); stmtIt.hasNext();) {
						final Unit stmt = (Unit) stmtIt.next();
						if (!(stmt instanceof JAssignStmt))	continue;
						JAssignStmt assignStmt = (JAssignStmt) stmt;
						Value left = assignStmt.getLeftOp();
						Value right = assignStmt.getRightOp();
						if (!left.equals(var) && !right.equals(var)) continue;
						Value assignedVal = null;
						if (left.equals(var)) {
							assignedVal = right;
						} else {
							assignedVal = left;
						}
						List<Value> values = assignedVal.getUseBoxes();
						for (Value value : values) {
							System.out.println(value.toString());
						}
						SootField sootField = null;
						if ((assignedVal instanceof JInstanceFieldRef)) {
							JInstanceFieldRef instanceField = (JInstanceFieldRef) assignedVal;
							sootField = instanceField.getField();
							//getFieldInitialValue(sootField, false);
						} else if (assignedVal instanceof StaticFieldRef) {
							StaticFieldRef staticField = (StaticFieldRef) assignedVal;
							sootField = staticField.getField();
							//getFieldInitialValue(sootField, true);
						}
					}

					if (set != null) {
						for (Entry<JimpleLocal, PointsToSet> entryOut : pointsToSetMap
								.entrySet()) {
							if (entryOut.equals(set))
								continue;
							if (entryOut.getValue()
									.hasNonEmptyIntersection(set)) {
								System.out.println(entryOut.getKey().getName()
										+ "------------"
										+ ((JimpleLocal) var).getName());
							}
						}
						/*Set<String> possibleVals = set.possibleStringConstants();
						System.out.println(possibleVals);
						Set<ClassConstant> classConstants = set.possibleClassConstants();
						System.out.println(classConstants.toString());*/
					}
				}
			}
			/*
			 * Iterator<Loop> loopsIter = LI.iterator(); while
			 * (loopsIter.hasNext()) { Loop loop = loopsIter.next();
			 * //System.out.println (loop.toString()); }
			 */
		}
	}

	static void setSparkPointsToAnalysis() {
		System.out.println("[spark] Starting analysis ...");

		HashMap opt = new HashMap();
		opt.put("enabled", "true");
		opt.put("verbose", "true");
		opt.put("ignore-types", "false");
		opt.put("force-gc", "false");
		opt.put("pre-jimplify", "false");
		opt.put("vta", "false");
		opt.put("rta", "false");
		opt.put("field-based", "false");
		opt.put("types-for-sites", "false");
		opt.put("merge-stringbuffer", "true");
		opt.put("string-constants", "false");
		opt.put("simulate-natives", "true");
		opt.put("simple-edges-bidirectional", "false");
		opt.put("on-fly-cg", "true");
		opt.put("simplify-offline", "false");
		opt.put("simplify-sccs", "false");
		opt.put("ignore-types-for-sccs", "false");
		opt.put("propagator", "worklist");
		opt.put("set-impl", "double");
		opt.put("double-set-old", "hybrid");
		opt.put("double-set-new", "hybrid");
		opt.put("dump-html", "false");
		opt.put("dump-pag", "false");
		opt.put("dump-solution", "false");
		opt.put("topo-sort", "false");
		opt.put("dump-types", "true");
		opt.put("class-method-var", "true");
		opt.put("dump-answer", "false");
		opt.put("add-tags", "false");
		opt.put("set-mass", "false");

		SparkTransformer.v().transform("", opt);

		System.out.println("[spark] Done!");
	}

	/*
	 * private static void setPaddlePointsToAnalysis() {
	 * System.out.println("[paddle] Starting analysis ...");
	 * 
	 * System.err.println("Soot version string: "+soot.Main.v().versionString);
	 * 
	 * HashMap opt = new HashMap(); opt.put("enabled","true");
	 * opt.put("verbose","true"); opt.put("bdd","true");
	 * opt.put("backend","buddy"); opt.put("context","kcfa"); opt.put("k","2");
	 * // opt.put("context-heap","true"); opt.put("propagator","auto");
	 * opt.put("conf","ofcg"); opt.put("order","32"); opt.put("q","auto");
	 * opt.put("set-impl","double"); opt.put("double-set-old","hybrid");
	 * opt.put("double-set-new","hybrid"); opt.put("pre-jimplify","false");
	 * 
	 * 
	 * PaddleTransformer pt = new PaddleTransformer(); PaddleOptions paddle_opt
	 * = new PaddleOptions(opt); pt.setup(paddle_opt); pt.solve(paddle_opt);
	 * soot.jimple.paddle.Results.v().makeStandardSootResults();
	 * 
	 * System.out.println("[paddle] Done!"); }
	 */

	private static int getLineNumber(Stmt s) {
		Iterator ti = s.getTags().iterator();
		while (ti.hasNext()) {
			Object o = ti.next();
			if (o instanceof LineNumberTag)
				return Integer.parseInt(o.toString());
		}
		return -1;
	}

	private static SootField getField(String classname, String fieldname) {
		Collection app = Scene.v().getApplicationClasses();
		Iterator ci = app.iterator();
		while (ci.hasNext()) {
			SootClass sc = (SootClass) ci.next();
			if (sc.getName().equals(classname))
				return sc.getFieldByName(fieldname);
		}
		throw new RuntimeException("Field " + fieldname
				+ " was not found in class " + classname);
	}

	private static Map/* <Integer,Local> */getLocals(SootClass sc,
			String methodname, String typename) {
		Map res = new HashMap();
		Iterator mi = sc.getMethods().iterator();
		while (mi.hasNext()) {
			SootMethod sm = (SootMethod) mi.next();
			System.err.println(sm.getName());
			if (true && sm.getName().equals(methodname) && sm.isConcrete()) {
				JimpleBody jb = (JimpleBody) sm.retrieveActiveBody();
				Iterator ui = jb.getUnits().iterator();
				while (ui.hasNext()) {
					Stmt s = (Stmt) ui.next();
					int line = getLineNumber(s);
					// find definitions
					Iterator bi = s.getDefBoxes().iterator();
					while (bi.hasNext()) {
						Object o = bi.next();
						if (o instanceof ValueBox) {
							Value v = ((ValueBox) o).getValue();
							if (v.getType().toString().equals(typename)
									&& v instanceof Local)
								res.put(new Integer(line), v);
						}
					}
				}
			}
		}

		return res;
	}

	private static void printLocalIntersects(Map/* <Integer,Local> */ls) {
		soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		Iterator i1 = ls.entrySet().iterator();
		while (i1.hasNext()) {
			Map.Entry e1 = (Map.Entry) i1.next();
			int p1 = ((Integer) e1.getKey()).intValue();
			Local l1 = (Local) e1.getValue();
			PointsToSet r1 = pta.reachingObjects(l1);
			Iterator i2 = ls.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry e2 = (Map.Entry) i2.next();
				int p2 = ((Integer) e2.getKey()).intValue();
				Local l2 = (Local) e2.getValue();
				PointsToSet r2 = pta.reachingObjects(l2);
				if (p1 <= p2)
					System.out.println("[" + p1 + "," + p2
							+ "]\t Container intersect? "
							+ r1.hasNonEmptyIntersection(r2));
			}
		}
	}
	
	private static void getFieldInitialValue (SootField sootField, boolean staticField) {
		SootClass sootClass = sootField.getDeclaringClass();
		String method = "";
		if (staticField) {
			method = "<clinit>";
		} else {
			method = "<init>";
		}
		SootMethod methodObj = sootClass.getMethodByName(method);
		Body b = methodObj.retrieveActiveBody();
		Iterator<Unit> unitsIter = b.getUnits().iterator();
		while (unitsIter.hasNext()) {
			Unit unit = unitsIter.next();
			if (unit instanceof JAssignStmt) {
				JAssignStmt assignStmt = (JAssignStmt) unit;
				Value left = assignStmt.getLeftOp();
				Value right = assignStmt.getRightOp();
				if (!(left instanceof StaticFieldRef) && !(right instanceof StaticFieldRef))	continue;
				SootField referredField = null;
				boolean isLeft = false;
				if (left instanceof StaticFieldRef) {
					referredField = ((StaticFieldRef) left).getField();
					isLeft = true;
				} else {
					referredField = ((StaticFieldRef) right).getField();
				}
				if (!referredField.equals(sootField))	continue;
				Value value = null;
				if (isLeft)	value = left;
				else value = right;
				System.out.println("Found value = "+ value.toString());
			}
		}
	}

	private static void printFieldIntersects(Map/* <Integer,Local> */ls,
			SootField f) {
		soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		Iterator i1 = ls.entrySet().iterator();
		while (i1.hasNext()) {
			Map.Entry e1 = (Map.Entry) i1.next();
			int p1 = ((Integer) e1.getKey()).intValue();
			Local l1 = (Local) e1.getValue();
			PointsToSet r1 = pta.reachingObjects(l1, f);
			Iterator i2 = ls.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry e2 = (Map.Entry) i2.next();
				int p2 = ((Integer) e2.getKey()).intValue();
				Local l2 = (Local) e2.getValue();
				PointsToSet r2 = pta.reachingObjects(l2, f);
				if (p1 <= p2)
					System.out.println("[" + p1 + "," + p2
							+ "]\t Container.item intersect? "
							+ r1.hasNonEmptyIntersection(r2));
			}
		}
	}

}
