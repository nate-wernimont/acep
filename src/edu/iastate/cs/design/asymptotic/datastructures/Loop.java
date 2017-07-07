package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import soot.Body;
import soot.IntType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDivExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JMulExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JSubExpr;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Loop {

	protected final Stmt header;
	protected Stmt preHeader;
	protected final Stmt backJump;
	protected final List<Stmt> loopStatements;
	protected final UnitGraph g;
	protected Collection<Stmt> loopExists;
	protected List<Block> loopblocks = null;
	
	/* Loop bound related begins */
	Unit lstart;
	Unit lend;
	Unit linc;
	
	boolean isLoopAnalysis = false;
	
	String lVar;
	int istart;
	int iinc;
	int iupto;
	int ibound = -1;
	Value vStart;
	Value vCond;
	// does loop body changes loop counter variable?
	boolean invariant = false;
	boolean pointsToRequired = false;
	Value pointsToVar;
	/* Loop bound related ends */
	
	private final int INFINITY = 1000000000;

	// protected List<Loop> inner;

	/**
	 * Creates a new loop. Expects that the last statement in the list is the
	 * loop head and the second-last statement is the back-jump to the head.
	 * {@link LoopFinder} will normally guarantee this.
	 * 
	 * @param head
	 *            the loop header @param loopStatements an ordered list of loop
	 *            statements, ending with the header
	 * @param g
	 *            the unit graph according to which the loop exists
	 */
	Loop(Stmt head, List<Stmt> loopStatements, UnitGraph g) {
		this.header = head;
		this.preHeader = null;
		this.g = g;

		// put header to the top
		loopStatements.remove(head);
		loopStatements.add(0, head);

		// last statement
		this.backJump = loopStatements.get(loopStatements.size() - 1);

		assert g.getSuccsOf(this.backJump).contains(head); // must branch back
		// to the head

		this.loopStatements = loopStatements;
		
		/*System.out.println("====================================================");
		System.out.println(header.toString());
		System.out.println(backJump.toString());
		System.out.println(loopStatements.toString());*/
		
		//if (this.header instanceof IfStmt)
		if (isLoopAnalysis)
			breakdownStdLoop(this.header, g);
		/*else {
			// Get hte IfStmt from the loop body and send it
			Unit loopIfStmt = null;
			for (Unit loopUnit : loopStatements) {
				if (loopUnit instanceof IfStmt) {
					loopIfStmt = loopUnit;
					break;
				}
			}
			if (loopIfStmt != null) {
				// Yay...found it.
				breakdownStdLoop((IfStmt) loopIfStmt, g);
			}
		}*/
		/*else if (this.backJump instanceof IfStmt) {
			breakdownStdLoop((IfStmt) this.backJump, g);
		} else {
			/ can't break loop and my head too....
		}*/
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String header = this.header.toString();
		String preHeader = "";
		if (this.preHeader != null)
			preHeader = this.preHeader.toString();
		String backjump = this.backJump.toString();
		String loopStmts = loopStatements.toString();

		String loop = "[\n header = " + header + "\n preHeader = " + preHeader
				+ "\n backjump = " + backjump + "\n loopStatements = "
				+ loopStmts + " \n]";
		// return super.toString();
		return loop;
	}

	public void setLoopBlcoks(List<Block> loopBlocks) {
		this.loopblocks = loopBlocks;
	}

	public List<Block> getLoopBlocks() {
		return this.loopblocks;
	}

	public Iterator<Block> loopIterator() {
		return this.loopblocks.iterator();
	}

	public Stmt getHead() {
		return header;
	}

	/**
	 * A loop has a preheader if there is only one edge to the header of the
	 * loop from outside of the loop. If this is the case, the block branching
	 * to the header of the loop is the preheader node.
	 * 
	 * @return
	 */
	public Stmt getPreHead() {
		return preHeader;
	}

	public void setPreHeader(Stmt preHeader) {
		this.preHeader = preHeader;
	}

	/**
	 * Returns the statement that jumps back to the head, thereby constituing
	 * the loop.
	 */
	public Stmt getBackJumpStmt() {
		return backJump;
	}

	/**
	 * @return all statements of the loop, including the header; the header will
	 *         be the first element returned and then the other statements
	 *         follow in the natural ordering of the loop
	 */
	public List<Stmt> getLoopStatements() {
		return loopStatements;
	}

	/**
	 * Returns all loop exists. A loop exit is a statement which has a successor
	 * that is not contained in the loop.
	 */
	public Collection<Stmt> getLoopExits() {
		if (loopExists == null) {
			loopExists = new HashSet<Stmt>();
			for (Stmt s : loopStatements) {
				for (Unit succ : g.getSuccsOf(s)) {
					if (!loopStatements.contains(succ)) {
						loopExists.add(s);
					}
				}
			}
		}
		return loopExists;
	}

	/**
	 * Computes all targets of the given loop exit, i.e. statements that the
	 * exit jumps to but which are not part of this loop.
	 */
	public Collection<Stmt> targetsOfLoopExit(Stmt loopExit) {
		assert getLoopExits().contains(loopExit);
		List<Unit> succs = g.getSuccsOf(loopExit);
		Collection<Stmt> res = new HashSet<Stmt>();
		for (Unit u : succs) {
			Stmt s = (Stmt) u;
			res.add(s);
		}
		res.removeAll(loopStatements);
		return res;
	}

	/**
	 * Returns <code>true</code> if this loop certainly loops forever, i.e. if
	 * it has not exit. @see #getLoopExits()
	 */
	public boolean loopsForever() {
		return getLoopExits().isEmpty();
	}

	/**
	 * Returns <code>true</code> if this loop has a single exit statement. @see
	 * #getLoopExits()
	 */
	public boolean hasSingleExit() {
		return getLoopExits().size() == 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		result = prime * result
				+ ((loopStatements == null) ? 0 : loopStatements.hashCode());
		return result;
	}

	public Block getLoopHeadBlock() {
		Iterator<Block> loopBlkIter = loopIterator();
		while (loopBlkIter.hasNext()) {
			Block blk = loopBlkIter.next();
			Iterator<Unit> BlkUnitsIter = blk.iterator();
			while (BlkUnitsIter.hasNext()) {
				if (BlkUnitsIter.next().equals(header)) {
					return blk;
				}
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Loop other = (Loop) obj;
		if (header == null) {
			if (other.header != null) {
				return false;
			}
		} else if (!header.equals(other.header)) {
			return false;
		}
		if (loopStatements == null) {
			if (other.loopStatements != null) {
				return false;
			}
		} else if (!loopStatements.equals(other.loopStatements))
			return false;
		return true;
	}

	public boolean contains(Unit stmt) {
		return loopStatements.contains(stmt);
	}

	public boolean contains(Block block) {
		return loopblocks.contains(block);
	}

	public boolean isInLoop(Block block) {
		for (Iterator<Block> blkIter = loopblocks.iterator(); blkIter.hasNext();) {
			Block blk = blkIter.next();
			if (blk.toString().equals(block.toString()))
				return true;
		}
		return false;
	}

	public boolean isInLoop(Unit stmt) {
		return loopStatements.contains(stmt);
	}

	private List<Unit> generateFullPredecessors(Unit u, UnitGraph eug,
			List<Unit> toIgnore) {
		List<Unit> predecessors = new ArrayList<Unit>();
		List<Unit> worklist = new ArrayList<Unit>();
		if (!toIgnore.contains(u))
			worklist.addAll(eug.getPredsOf(u));
		while (!worklist.isEmpty()) {
			Unit currUnit = worklist.get(0);
			predecessors.add(currUnit);
			worklist.remove(currUnit);
			if (toIgnore.contains(currUnit))
				continue;
			List<Unit> toAdd = new ArrayList<Unit>(eug.getPredsOf(currUnit));
			// toAdd.removeAll(toIgnore);
			worklist.addAll(toAdd);
			worklist.removeAll(predecessors);
		}
		return predecessors;
	}

	private List<Unit> generateFullSuccessors(Unit u, UnitGraph eug,
			List<Unit> toIgnore) {
		List<Unit> successors = new ArrayList<Unit>();
		List<Unit> worklist = new ArrayList<Unit>();
		worklist.addAll(eug.getSuccsOf(u));
		worklist.removeAll(toIgnore);
		while (!worklist.isEmpty()) {
			Unit currUnit = worklist.get(0);
			successors.add(currUnit);
			worklist.remove(currUnit);
			// if(currUnit.fallsThrough() || currUnit.branches()){
			List<Unit> toAdd = new ArrayList<Unit>(eug.getSuccsOf(currUnit));
			toAdd.removeAll(toIgnore);
			worklist.addAll(toAdd);
			worklist.removeAll(successors);
		}
		// Get correct order
		List<Unit> RPO = reversePostOrder(eug);
		List<Unit> succInOrder = new ArrayList<Unit>();
		final int rpoSize = RPO.size();
		for (int i = 0; i < rpoSize; i++) {
			Unit currElem = RPO.get(i);
			if (successors.contains(currElem)) {
				succInOrder.add(currElem);
			}
		}
		return succInOrder;
	}

	private List<Unit> iterativePostOrder(Unit rootNode, UnitGraph eug) {
		Stack<Unit> nodeStack = new Stack<Unit>();
		List<Unit> visited = new ArrayList<Unit>();
		List<Unit> po = new ArrayList<Unit>();
		nodeStack.add(rootNode);
		Unit currNode;
		while (!nodeStack.isEmpty()) {
			currNode = nodeStack.peek(); // .get(0);
			List<Unit> currSuccs = eug.getSuccsOf(currNode);
			final int succSize = currSuccs.size();

			if (succSize > 0 && !visited.contains(currSuccs.get(0))
					&& !nodeStack.contains(currSuccs.get(0))) {
				nodeStack.push(currSuccs.get(0));
			} else {
				if (succSize > 1 && !visited.contains(currSuccs.get(1))
						&& !nodeStack.contains(currSuccs.get(1))) {
					nodeStack.push(currSuccs.get(1));
				} else {
					nodeStack.pop(); // .remove(currNode);
					visited.add(currNode);
					po.add(currNode);
				}
			}
		}
		return po;
	}

	private void poVisit(Unit u, ExceptionalUnitGraph eug, List<Unit> PO,
			List<Unit> visitedList) {
		List<Unit> uList = eug.getSuccsOf(u);
		if (!uList.isEmpty()) {
			final int succSize = uList.size();
			for (int i = 0; i < succSize; i++) {
				Unit currElem = uList.get(i);
				if (!visitedList.contains(currElem)) {
					visitedList.add(currElem);
					poVisit(currElem, eug, PO, visitedList);
				}
			}
		}
		PO.add(u);
	}

	private List<Unit> reversePostOrder(UnitGraph eug) {
		List<Unit> RPO = new ArrayList<Unit>();
		List<Unit> PO = new ArrayList<Unit>();
		List<Unit> visitedList = new ArrayList<Unit>();
		List<Unit> entry = eug.getHeads();
		if (entry.size() > 1) {
			// throw new SymbolicExecutionException(new
			// InvalidArgument("Too many entry points"));
		}

		PO = iterativePostOrder(entry.get(0), eug);

		// poVisit(entry.get(0), eug, PO, visitedList);
		final int numNodes = PO.size();
		for (int i = numNodes - 1; i >= 0; i--) {
			RPO.add(PO.get(i));
		}
		return RPO;
	}

	private Unit getSuccessorInLoop(Unit u, List<Unit> loop, UnitGraph eug) {
		List<Unit> possibleEntryNodes = eug.getSuccsOf(u); // entry node is
		// x=succ(n) s.t. x
		// in loop
		Unit entryNode = possibleEntryNodes.get(0);
		// if entry not in loop, cannot be entry node
		if (!loop.contains(entryNode)) {
			if (possibleEntryNodes.size() > 1) { // Entry node is actually the
				// other
				entryNode = possibleEntryNodes.get(1);
				if (!loop.contains(entryNode)) {
					entryNode = null;
				}
			} else {
				entryNode = null;
			}
		}
		return entryNode;
	}

	private boolean jumpsOutOfLoop(Unit stmt, List<Unit> loop, UnitGraph eug) {
		List<Unit> stmtSuccs = eug.getSuccsOf(stmt);
		final int stmtSuccsSize = stmtSuccs.size();
		for (int i = 0; i < stmtSuccsSize; i++) {
			Unit currSucc = stmtSuccs.get(i);
			if (!loop.contains(currSucc))
				return true;
		}
		return false;
	}

	private boolean containsJumpsOutOfLoop(List<Unit> stmtList,
			List<Unit> loop, UnitGraph eug) {
		final int stmtListSize = stmtList.size();
		for (int i = 0; i < stmtListSize; i++) {
			if (jumpsOutOfLoop(stmtList.get(i), loop, eug))
				return true;
		}
		return false;
	}

	private int getIncrement(Value v) {
		//JimpleLocal local = (JimpleLocal) v;
		if (v instanceof IntConstant) {
			return ((IntConstant) v).value;
		} else {
			return 0;
		}
	}
	
	private void getFieldInitialValue (SootField sootField, boolean staticField) {
		SootClass sootClass = sootField.getDeclaringClass();
		String method = "";
		if (staticField) {
			method = "<clinit>";
		} else {
			method = "<init>";
		}
		List<SootMethod> methods = sootClass.getMethods();
		List<SootMethod> impMethods = new ArrayList<SootMethod>();
		for (SootMethod sootMethod : methods) {
			if (sootMethod.getSignature().contains(method))
				impMethods.add(sootMethod);
		}
		for (SootMethod methodObj : impMethods) {
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
					if (isLeft)	value = right;
					else value = left;
					System.out.println("Found value = "+ value.toString());
				}
			}
		}
		//SootMethod methodObj = //sootClass.getMethodByName(method);
		
	}
	
	private void pointsToVariableAnalysis() {
		if (!pointsToRequired)
			return;
		for (Iterator<Unit> unitIter = g.iterator(); unitIter.hasNext();) {
			Unit unit = unitIter.next();
			if (!(unit instanceof JAssignStmt))	continue;
			JAssignStmt assignStmt = (JAssignStmt) unit;
			Value left = assignStmt.getLeftOp();
			Value right = assignStmt.getRightOp();
			if (!left.equals(pointsToVar) && !right.equals(pointsToVar)) continue;
			Value assignedVal = null;
			if (left.equals(pointsToVar)) {
				assignedVal = right;
			} else {
				assignedVal = left;
			}
			SootField sootField = null;
			if ((assignedVal instanceof JInstanceFieldRef)) {
				JInstanceFieldRef instanceField = (JInstanceFieldRef) assignedVal;
				sootField = instanceField.getField();
				getFieldInitialValue(sootField, false);
			} else if (assignedVal instanceof StaticFieldRef) {
				StaticFieldRef staticField = (StaticFieldRef) assignedVal;
				sootField = staticField.getField();
				getFieldInitialValue(sootField, true);
			}
			System.out.println("PointsToVariable = "+pointsToVar.toString()+ " value = " + assignedVal.toString());
		}
	}

	private int evaluateLoopExpr(Value v1, Value v2, String variable) {
		boolean which = true;
		if (v1 instanceof JimpleLocal) {
			if (v2 instanceof JimpleLocal) {
				if (v1.toString().contains(variable)) {
					pointsToVar = v2;
				} else if (v2.toString().contains(variable)){
					pointsToVar = v1;
				} else {
					// assuming that loop start and condition variables are different
					vCond = v1;
				}
				// both variables.
				pointsToRequired = true;
				System.out.println("Loop bound is variable, do points-to analysis");
				pointsToVariableAnalysis();
				return 0;
			}
		} else if (v2 instanceof JimpleLocal) {
			which = false;
		} else {
			System.out
					.println("Ill formed loop, conditional expression is ill formed");
		}
		// if v1 is variable, then which = true, else false
		if (which) {
			if (v2 instanceof IntConstant) {
				return ((IntConstant) v2).value;
			} else { // v2 is expression evaluates to constant

			}
		} else {
			if (v1 instanceof IntConstant) {
				return ((IntConstant) v1).value;
			} else { // v1 is expression evaluates to constant

			}
		}

		return -INFINITY;

	}

	private boolean checkOperands(Value v1, Value v2, boolean checkValue) {

		JimpleLocal local = (JimpleLocal) v1;
		if (!(v1.getType() instanceof IntType)) {
			return false;
		}
		if (v2 instanceof IntConstant) {
			if (checkValue) {
				if (v2.equals(0)) {
					return true;
				}
				return false;
			} else
				return true;

		} else {
			return false;
		}

	}
	
	
	/*private SootLoopStmt breakdownIterLoop(IfStmt stmt, UnitGraph eug) {
        Unit start = null;
        List<Unit> end = new ArrayList<Unit>();
        Unit inc;
        // Calculate loop body
        List<Unit> emptyList = new ArrayList<Unit>();
        List<Unit> entireLoop = generateFullSuccessors(stmt, eug, emptyList);
        List<Unit> loopPreds = generateFullPredecessors(stmt, eug, emptyList);
        List<Unit> loopBody;
        entireLoop.retainAll(loopPreds);

        if (entireLoop.contains(stmt)) {
            // Set end
            // Iter loops have 2 stms as "end"/conditional
            // 1) "hasNext" call
            // 2) check result of (1)
            end.add(stmt); // add (2)
            List<Unit> stmtPreds = eug.getPredsOf(stmt);
            if (stmtPreds.size() != 1) {
                //Logging.trace("Misformed iter loop -- doesn't get has next before conditional");
                return null;
            }
            Unit getHasNext = stmtPreds.get(0);
            end.add(getHasNext); // add (1)

            // Set start which is predecessor of end's pred NOT in the loop
            // End must only have 2 preds -- unconditional jump in and increment
            List<Unit> endPreds = eug.getPredsOf(getHasNext);
            final int endPredsSize = endPreds.size();
            boolean found = false;
            for (int i = 0; i < endPredsSize && endPredsSize == 2; i++) {
                Unit jumpIn = endPreds.get(i);
                // Find pred that is not in the loop with successor in loop
                if (!entireLoop.contains(jumpIn)) {
                    Unit loopEntry = getSuccessorInLoop(jumpIn, entireLoop, eug);
                    if (loopEntry != null) {
                        if (!end.contains(loopEntry)) {
                            // If this is the case, it is not a for-loop
                            //Logging.trace("Misformed iter loop -- possible 2 entry points");
                            return null;
                        }
                        List<Unit> jumpInPreds = eug.getPredsOf(jumpIn);
                        // Must only have 1 pred b/c it must be "start"
                        // condition
                        if (jumpInPreds.size() == 1) {
                            start = jumpInPreds.get(0);
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                //Logging.trace("Misformed iter loop -- couldn't find entry point");
                return null;
            }

            // Set inc which is successor of conditional IN loop
            inc = getSuccessorInLoop(stmt, entireLoop, eug);
            if (inc == null) {
                //Logging.trace("Misformed iter loop -- couldn't find inc");
                return null;
            }

            // Calculate loop body we care about
            loopBody = new ArrayList<Unit>(entireLoop);
            loopBody.remove(inc);
            loopBody.removeAll(end);

            String v1 = null, v2 = null;
			int init = 0, incr = 0, last = 0;

			// Processing loop start
			for (ValueBox vb : (List<ValueBox>) start.getUseAndDefBoxes()) {
				Value v = vb.getValue();
				if (v instanceof JimpleLocal) {
					JimpleLocal value = (JimpleLocal) v;
					v1 = value.getName();
				} else if (v instanceof IntConstant) {
					init = ((IntConstant) v).value;
					// init = Integer.valueOf(v.toString()).intValue();
				}

			}

			// Processing loop condition
			// ValueBox vb = end.getConditionBox();
			Value cond = ((JIfStmt)end.get(0)).getCondition();
			// Value v = vb.getValue();

			if (cond instanceof JEqExpr) {
				JEqExpr expr = (JEqExpr) cond;
				Value op1 = expr.getOp1();
				Value op2 = expr.getOp2();
				last = evaluateLoopExpr(op1, op2, v1);

			} else if (cond instanceof JGtExpr) {
				JGtExpr expr = (JGtExpr) cond;
				Value op1 = expr.getOp1();
				Value op2 = expr.getOp2();
				last = evaluateLoopExpr(op1, op2, v1);

			} else if (cond instanceof JGeExpr) {
				JGeExpr expr = (JGeExpr) cond;
				Value op1 = expr.getOp1();
				Value op2 = expr.getOp2();
				last = evaluateLoopExpr(op1, op2, v1);
			} else if (cond instanceof JLtExpr) {
				JLtExpr expr = (JLtExpr) cond;
				Value op1 = expr.getOp1();
				Value op2 = expr.getOp2();
				last = evaluateLoopExpr(op1, op2, v1);
			} else if (cond instanceof JLeExpr) {
				JLeExpr expr = (JLeExpr) cond;
				Value op1 = expr.getOp1();
				Value op2 = expr.getOp2();
				last = evaluateLoopExpr(op1, op2, v1);
			} else if (cond instanceof JNeExpr) {
				JNeExpr expr = (JNeExpr) cond;
				Value op1 = expr.getOp1();
				Value op2 = expr.getOp2();
				last = evaluateLoopExpr(op1, op2, v1);
			} else {
				System.out.println("Invalid expression....");
			}

			if (inc instanceof JAssignStmt) {
				for (ValueBox vb : inc.getUseBoxes()) {
					Value v = vb.getValue();
					if (v instanceof Constant) {
						continue;
					} else if (v instanceof JimpleLocal) {
						JimpleLocal value = (JimpleLocal) v;
						v2 = value.getName();
						if (!v1.equals(v2))
							System.out
									.println("Ill formed loop, loop variable and increment are not same");
					} else if (v instanceof JAddExpr) {
						JAddExpr expr = (JAddExpr) v;
						Value op1 = expr.getOp1();
						Value op2 = expr.getOp2();
						if (op1 instanceof JimpleLocal) {
							if (!checkOperands(op1, op2, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op2);

						} else if (op2 instanceof JimpleLocal) {
							if (!checkOperands(op2, op1, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op1);
						}
					} else if (v instanceof JSubExpr) {
						JSubExpr expr = (JSubExpr) v;
						Value op1 = expr.getOp1();
						Value op2 = expr.getOp2();
						if (op1 instanceof JimpleLocal) {
							if (!checkOperands(op1, op2, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op2);
						} else if (op2 instanceof JimpleLocal) {
							if (!checkOperands(op2, op1, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op1);
						}
					} else if (v instanceof JMulExpr) {
						JMulExpr expr = (JMulExpr) v;
						Value op1 = expr.getOp1();
						Value op2 = expr.getOp2();
						if (op1 instanceof JimpleLocal) {
							if (!checkOperands(op1, op2, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op2);

						} else if (op2 instanceof JimpleLocal) {
							if (!checkOperands(op2, op1, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op1);
						}
					} else if (v instanceof JDivExpr) {
						JDivExpr expr = (JDivExpr) v;
						Value op1 = expr.getOp1();
						Value op2 = expr.getOp2();
						if (op1 instanceof JimpleLocal) {
							if (!checkOperands(op1, op2, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op2);

						} else if (op2 instanceof JimpleLocal) {
							if (!checkOperands(op2, op1, false)) {
								System.out
										.println("Ill formed loop, increment expression is invalid");
							}
							incr = getIncrement(op1);
						}
					} else {
						System.out
								.println("Ill formed loop, increment expression is invalid");
					}

				}
			} else {
				System.out.println("Ill formed loop, problem with increment");
			}

			System.out.println("Loop variable: " + v1);
			System.out.println("Loop start: " + init);
			System.out.println("Loop end: " + last);
			System.out.println("Loop increment: " + incr);


            // TODO: Make sure body doesn't step iterator i
            // TODO: check for common iter between start, inc, and end
            if (containsJumpsOutOfLoop(loopBody, entireLoop, eug)) {
                //Logging.trace("Misformed iter loop -- breaks");
                return null;
            }

            
            // TODO: Discuss shape of SootLoopStmt with Tyler.
            // return new SootLoopStmt(start, inc, end, loopBody);

        }
        return null;
    }*/

	private SootLoopStmt breakdownStdLoop(Stmt stmt, UnitGraph eug) {
		Unit start = null;
		Stmt end;
		Unit inc;
		// Calculate loop body
		List<Unit> emptyList = new ArrayList<Unit>();
		List<Unit> entireLoop = generateFullSuccessors(stmt, eug, emptyList);
		List<Unit> outerLoopStmts = new ArrayList<Unit>();
		
		// removeOuterLoopStmts
		for (Unit unit : entireLoop) {
			if (!loopStatements.contains(unit))
				outerLoopStmts.add(unit);
		}
		
		List<Unit> loopPreds = generateFullPredecessors(stmt, eug, emptyList);
		List<Unit> loopBody;
		entireLoop.retainAll(loopPreds);

		if (entireLoop.contains(stmt)) {
			// Set end
			end = stmt;

			// Set start which is predecessor of end NOT in the loop
			List<Unit> endPreds = eug.getPredsOf(end);
			// End must only have 2 preds -- unconditional jump in and increment
			final int endPredsSize = endPreds.size();
			/*if (endPredsSize != 2) {
				// Logging.trace("Misformed std loop -- too many preds on end");
				return null;
			}*/
			boolean found = false;
			int jumpInLocation = -1;
			for (int i = 0; i < endPredsSize /*&& endPredsSize == 2*/; i++) {
				Unit jumpIn = endPreds.get(i);
				if (!entireLoop.contains(jumpIn) || outerLoopStmts.contains(jumpIn)) {
					Unit loopEntry = getSuccessorInLoop(jumpIn, entireLoop, eug);
					if (loopEntry != null) {
						jumpInLocation = i;
						if (loopEntry != end) {
							// If this is the case, it is not a for-loop
							// Logging.trace("Misformed std loop -- possible 2 entry points");
							return null;
						}
						if (jumpIn instanceof GotoStmt) {
							List<Unit> jumpInPreds = eug.getPredsOf(jumpIn);
							// Must only have 1 pred b/c it must be "start"
							// condition
							if (jumpInPreds.size() == 1) {
								start = jumpInPreds.get(0);
								found = true;
								break;
							}
						} else { // When loop is implemented in other style,
							// condition is at top so no jump before it
							start = jumpIn;
							found = true;
							break;
						}
					}
				}
			}
			if (!found) {
				// Logging.trace("Misformed std loop -- couldn't find entry point");
				return null;
			}

			// Set inc which is predecessor of end IN loop
			int incLocation = jumpInLocation == 0 ? 1 : 0;
			inc = endPreds.get(incLocation);
			// When loop is in other style, "body" has a jmp at the end
			// that is, jump between "inc" and "end"
			Unit gotoTop = null;
			if ((inc instanceof GotoStmt) || (inc instanceof IfStmt)) {
				gotoTop = inc;
				List<Unit> gotoTopPreds = eug.getPredsOf(gotoTop);
				if (gotoTopPreds.size() != 1) {
					// Logging.trace("Misformed std/alt loop -- too many preds to goto top jmp");
					return null;
				}
				if (inc instanceof IfStmt) {
					end = (IfStmt) inc;
				} else {
					end = (GotoStmt) inc;
				}
				inc = gotoTopPreds.get(0);
			}
			
			// While-loop
			if (!(inc instanceof JAssignStmt))
				inc = end;

			// Calculate loop body we care about
			loopBody = new ArrayList<Unit>(entireLoop);
			loopBody.remove(inc);
			loopBody.remove(end);
			// TODO: Should we use this?
			loopBody.remove(gotoTop);

			if (!(end instanceof IfStmt)) {
				Unit loopIfStmt = null;
				for (Unit loopUnit : entireLoop) {
					if (loopUnit instanceof IfStmt) {
						loopIfStmt = loopUnit;
						break;
					}
				}
				if (loopIfStmt != null) {
					end = (Stmt) loopIfStmt;
				}
				loopBody.remove(end);
			}

			// Update loop bound related variable
			lstart = start;
			lend = end;
			linc = inc;
			
			System.out.println("------------------------------------------------------------------");
			System.out.println("std Start - " + start.toString());
			System.out.println("std End   - " + end.toString());
			System.out.println("std Inc   - " + inc.toString());
			//System.out.println("std Body  - " + loopBody.toString());
			System.out.println("------------------------------------------------------------------");
			
			formulateLoop(start, end, inc);
			// TODO: Make sure body doesn't define i
			// TODO: make sure loop is affine --
			// allow +1
			// allow +n if n is not defined in loop
			/*if (containsJumpsOutOfLoop(loopBody, entireLoop, eug)) {
				// Logging.trace("Misformed std loop -- breaks");
				return null;
			}

			return null;// new SootLoopStmt(start, inc, end, loopBody);
			 */			
		}
		return null;
	}
	
	private void formulateLoop (Unit start, Unit end, Unit inc) {
		String v1 = null, v2 = null;
		int init = 0, incr = 0, last = 0;

		//Value counterVar = null;
		// Processing loop start
		for (ValueBox vb : (List<ValueBox>) start.getUseAndDefBoxes()) {
			Value v = vb.getValue();
			if (v instanceof JimpleLocal) {
				JimpleLocal value = (JimpleLocal) v;
				v1 = value.getName();
				vStart = v;
			} else if (v instanceof IntConstant) {
				init = ((IntConstant) v).value;
				// init = Integer.valueOf(v.toString()).intValue();
			}

		}
		// Loop bound related updates
		lVar = v1;
		istart = init;
		
		// Processing loop condition
		// ValueBox vb = end.getConditionBox();
		//if (end instanceof IfStmt)
		Value cond = ((IfStmt) end).getCondition();
		// Value v = vb.getValue();
		Value op1 = null, op2 = null;
		if (cond instanceof JEqExpr) {
			JEqExpr expr = (JEqExpr) cond;
			op1 = expr.getOp1();
			op2 = expr.getOp2();
			last = evaluateLoopExpr(op1, op2, v1);

		} else if (cond instanceof JGtExpr) {
			JGtExpr expr = (JGtExpr) cond;
			op1 = expr.getOp1();
			op2 = expr.getOp2();
			last = evaluateLoopExpr(op1, op2, v1);

		} else if (cond instanceof JGeExpr) {
			JGeExpr expr = (JGeExpr) cond;
			op1 = expr.getOp1();
			op2 = expr.getOp2();
			last = evaluateLoopExpr(op1, op2, v1);
		} else if (cond instanceof JLtExpr) {
			JLtExpr expr = (JLtExpr) cond;
			op1 = expr.getOp1();
			op2 = expr.getOp2();
			last = evaluateLoopExpr(op1, op2, v1);
		} else if (cond instanceof JLeExpr) {
			JLeExpr expr = (JLeExpr) cond;
			op1 = expr.getOp1();
			op2 = expr.getOp2();
			last = evaluateLoopExpr(op1, op2, v1);
		} else if (cond instanceof JNeExpr) {
			JNeExpr expr = (JNeExpr) cond;
			op1 = expr.getOp1();
			op2 = expr.getOp2();
			last = evaluateLoopExpr(op1, op2, v1);
		} else {
			System.out.println("Invalid expression....");
		}
		
		// Loop bound related updates
		iupto = last;
		// Update pointsToVariable
		//updatePointsToVariable(op1, op2, counterVar);

		if (inc instanceof JAssignStmt) {
			for (ValueBox vb : inc.getUseBoxes()) {
				Value v = vb.getValue();
				if (v instanceof Constant) {
					continue;
				} else if (v instanceof JimpleLocal) {
					JimpleLocal value = (JimpleLocal) v;
					v2 = value.getName();
					if (!v1.equals(v2))
						System.out
								.println("Ill formed loop, loop variable and increment are not same");
				} else if (v instanceof JAddExpr) {
					JAddExpr expr = (JAddExpr) v;
					op1 = expr.getOp1();
					op2 = expr.getOp2();
					if (op1 instanceof JimpleLocal) {
						if (!checkOperands(op1, op2, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op2);

					} else if (op2 instanceof JimpleLocal) {
						if (!checkOperands(op2, op1, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op1);
					}
				} else if (v instanceof JSubExpr) {
					JSubExpr expr = (JSubExpr) v;
					op1 = expr.getOp1();
					op2 = expr.getOp2();
					if (op1 instanceof JimpleLocal) {
						if (!checkOperands(op1, op2, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op2);
					} else if (op2 instanceof JimpleLocal) {
						if (!checkOperands(op2, op1, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op1);
					}
				} else if (v instanceof JMulExpr) {
					JMulExpr expr = (JMulExpr) v;
					op1 = expr.getOp1();
					op2 = expr.getOp2();
					if (op1 instanceof JimpleLocal) {
						if (!checkOperands(op1, op2, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op2);

					} else if (op2 instanceof JimpleLocal) {
						if (!checkOperands(op2, op1, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op1);
					}
				} else if (v instanceof JDivExpr) {
					JDivExpr expr = (JDivExpr) v;
					op1 = expr.getOp1();
					op2 = expr.getOp2();
					if (op1 instanceof JimpleLocal) {
						if (!checkOperands(op1, op2, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op2);

					} else if (op2 instanceof JimpleLocal) {
						if (!checkOperands(op2, op1, false)) {
							System.out
									.println("Ill formed loop, increment expression is invalid");
						}
						incr = getIncrement(op1);
					}
				} else {
					System.out
							.println("Ill formed loop, increment expression is invalid");
				}

			}
		} else {
			System.out.println("Ill formed loop, problem with increment");
		}
		
		// Loop bound related update
		istart = init;
		iupto = last;
		iinc = incr;
		ibound = last - (init+1) - incr + 2;
		if ((ibound != -1) && (ibound < 1000))
			ibound = Math.abs(ibound);
		else 
			ibound = -1;
		System.out.println("------------------------------------------------------------------");
		System.out.println("Loop variable: " + v1);
		System.out.println("Loop start: " + init);
		System.out.println("Loop end: " + last);
		System.out.println("Loop increment: " + incr);
		System.out.println("Loop bound: "+ibound);
		System.out.println("------------------------------------------------------------------");
	}
	
	public boolean requiresPointsTo () {
		return pointsToRequired;
	}
	
	public Value pointsToVariable () {
		return pointsToVar;
	}
	
	public int getLoopBound () {
		return ibound;
	}

}