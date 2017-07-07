package edu.iastate.cs.design.asymptotic.tests;

import java.util.Iterator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;

public class BasicBlock {

	public static void main(String[] args) {
		int count = 0;

		String _classpath = ".:/Users/gupadhyaya/workspace/test12/bin";
		Options.v().set_soot_classpath(_classpath);
		Options.v().set_keep_line_number(true);
		Scene.v().loadNecessaryClasses();
		SootClass sClass = Scene.v().loadClassAndSupport("MyClass");
		sClass.setApplicationClass();

		// Scene.v().getPointsToAnalysis().

		Iterator methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (!m.getName().equals("process"))
				continue;
			System.out.println("****************" + m.getName()
					+ "*****************");
			//LoopInfo LI = new LoopInfo(m);
			JimpleBody b = (JimpleBody) m.retrieveActiveBody();
			//ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
			BriefUnitGraph eug = new BriefUnitGraph(b);
			// Def-use analysis
			/*SimpleLocalDefs defs = new SimpleLocalDefs(eug);
			SimpleLocalUses uses = new SimpleLocalUses(b, defs);*/

			// Def-use map
			/*HashMap<Unit, Unit> defUses = new HashMap<Unit, Unit>();
			HashMap<Unit, Unit> usesOfDefs = new HashMap<Unit, Unit>();
			// BlockGraph bGraph = new BlockGraph(eug);
			CompleteBlockGraph cGraph = new CompleteBlockGraph(b);*/

/*			// Baf (bytecode) graph construction
			BafBody body = new BafBody(m.retrieveActiveBody(), (Map) null);
			BriefBlockGraph c = new BriefBlockGraph(body);*/

			//Iterator<Block> bafBlockIter = c.iterator();
			// JimpleBody body = Jimple.v().newBody(m);
			// Run through all the basic blocks
			//List<Value> defs = new ArrayList<Value>();
			if (b != null) {
				// ExceptionalUnitGraph eug = new ExceptionalUnitGraph(b);
				// Iterator<Unit> units = eug.iterator();
				// Iterator<Block> blockIter = cGraph.iterator();
				Iterator<Unit> unitIter = eug.iterator();
				while (unitIter.hasNext()) {
					/*Block bafBlock = bafBlockIter.next();
					for (Iterator<Unit> unitIter = bafBlock.iterator(); unitIter
					.hasNext();) {*/
						Unit unit = unitIter.next();
						System.out.println(unit.toString());
						//System.out.println(unit.toString());//+"-------"+uses.getUsesOf(unit).toString());
						/*List<UnitValueBoxPair> pairs = uses.getUsesOf(unit);
						for (UnitValueBoxPair unitValueBoxPair : pairs) {
							Unit use = unitValueBoxPair.getUnit();
							if (!defUses.containsKey(unit)) {
								defUses.put(unit, use);
							}
							if (!usesOfDefs.containsKey(use)) {
								usesOfDefs.put(use, unit);
							}*/
							/*System.out.println(use+"-----"+
									unitValueBoxPair.getValueBox().getValue().toString());*/
					//}
/*					for (Iterator<ValueBox> valueBoxIter = unit.getDefBoxes()
							.iterator(); valueBoxIter.hasNext();) {
						ValueBox box = valueBoxIter.next();
						Value value = box.getValue();
						// System.out.println("Def: "+value.toString());
						defs.add(value);
					}
						
					for (Iterator<ValueBox> valueBoxIter = unit.getUseBoxes().iterator(); 
							valueBoxIter.hasNext();) {
						ValueBox box = valueBoxIter.next();
						Value value = box.getValue();
						if (defs.contains(value))
							System.out.println("Use :" + value.toString());
					}*/
					
						/*// Method invocation has to be handled here...
						SootMethodRef mRef = null;
						double invokedMethodTime = 0.0;
						
						if (unit instanceof JSpecialInvokeExpr) {
							JSpecialInvokeExpr inst = (JSpecialInvokeExpr) unit;
							mRef = inst.getMethodRef();
						} else if (unit instanceof JInterfaceInvokeExpr) {
							JInterfaceInvokeExpr inst = (JInterfaceInvokeExpr) unit;
							mRef = inst.getMethodRef();
						} else if (unit instanceof JStaticInvokeExpr) {
							JStaticInvokeExpr inst = (JStaticInvokeExpr) unit;
							mRef = inst.getMethodRef();
						} else if (unit instanceof JVirtualInvokeExpr) {
							JVirtualInvokeExpr inst = (JVirtualInvokeExpr) unit;
							mRef = inst.getMethodRef();
						} else if (unit instanceof JInvokeStmt) {
							JInvokeStmt stmt = (JInvokeStmt) unit;
							//System.out.println(stmt.toString());
							
						}
	
						if (mRef != null) {
							SootMethod method = mRef.resolve();
							if (method.getDeclaringClass().isApplicationClass()
									|| !method.getDeclaringClass().getPackageName()
											.startsWith("java.")) {
								System.out.println("Called method: "+method.toString());
							}
						}*/
					//}
					/*for (Iterator j = unit.getTags().iterator(); j.hasNext();) {
						Tag tag = (Tag) j.next();
						if (tag instanceof LineNumberTag) {
							LineNumberTag lineNumberTag = (LineNumberTag) tag;
						    System.out.println(lineNumberTag.getLineNumber()+". "+unit.toString());
						}
					}*/
					/*
					 * Block blk = blockIter.next(); Iterator<Unit> blkUnitsIter
					 * = blk.iterator(); while (blkUnitsIter.hasNext()) { Unit
					 * unit = blkUnitsIter.next(); System.out.println
					 * (unit.toString()); //System.out.println
					 * ("Preds:= "+eug.getPredsOf(unit));
					 * //System.out.println("Succ:= "+eug.getSuccsOf(unit));
					 * //System.out.println
					 * ("===================================================");
					 * }
					 */
					// System.out.println (blk.toString());
				}
				// while (units.hasNext()) {
				// Unit unit = units.next();
				// System.out.println (unit.toString());
				// }
			}
			/*Set<Entry<Unit, Unit>> entries = defUses.entrySet();
			for (Entry<Unit, Unit> entry : entries) {
				System.out.println(entry.getKey()+"--->"+entry.getValue());
			}*/
		}

	}

}
