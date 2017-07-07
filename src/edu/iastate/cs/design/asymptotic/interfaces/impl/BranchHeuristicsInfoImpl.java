package edu.iastate.cs.design.asymptotic.interfaces.impl;

import java.util.Iterator;
import java.util.List;

import edu.iastate.cs.design.asymptotic.datastructures.Loop;
import edu.iastate.cs.design.asymptotic.datastructures.LoopInfo;
import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.datastructures.PostDominator;
import edu.iastate.cs.design.asymptotic.interfaces.BranchHeuristics;
import edu.iastate.cs.design.asymptotic.interfaces.BranchHeuristicsInfo;
import edu.iastate.cs.design.asymptotic.interfaces.BranchPredictionInfo;
import edu.iastate.cs.design.asymptotic.interfaces.BranchProbabilities;
import soot.IntType;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;

/**
 * This class is responsible to predict which successors of a basic block will
 * be taken and which will not according to an heuristic. If an heuristic
 * matches, a successor is considered taken with a given probability. The
 * heuristics implemented in this class, described in Ball (1993), with their
 * respective taken probabilities, found in Wu (1994), are described bellow: (1)
 * Loop Branch Heuristic (88%) (2) Pointer Heuristic (60%) (3) Call Heuristic
 * (78%) (4) Opcode Heuristic (84%) (5) Loop Exit Heuristic (80%) (6) Return
 * Heuristic (72%) (7) Store Heuristic (55%) (8) Loop Header Heuristic (75%) (9)
 * Guard Heuristic (62%)
 * 
 * References: Ball, T. and Larus, J. R. 1993. Branch prediction for free. In
 * Proceedings of the ACM SIGPLAN 1993 Conference on Programming Language Design
 * and Implementation (Albuquerque, New Mexico, United States, June 21 - 25,
 * 1993). R. Cartwright, Ed. PLDI '93. ACM, New York, NY, 300-313.
 * 
 * Youfeng Wu and James R. Larus. Static branch frequency and program profile
 * analysis. In MICRO 27: Proceedings of the 27th annual international symposium
 * on Microarchitecture. IEEE, 1994.
 * 
 * @author gupadhyaya
 * 
 */
public class BranchHeuristicsInfoImpl implements BranchHeuristicsInfo {

	PostDominator PD;
	BranchPredictionInfo BPI;
	LoopInfo LI;

	// A Prediction is a pair of basic blocks, in which the first indicates the
	// successor taken and the second the successor not taken.
	Pair<Unit, Unit> prediction;

	// There are 9 branch prediction heuristics.
	static final int numBranchHeuristics = 9;
	static BranchProbabilities[] probList = new BranchProbabilities[numBranchHeuristics];

	public BranchHeuristicsInfoImpl(LoopInfo li, BranchPredictionInfo bpi) {
		LI = li;
		BPI = bpi;
		PD = new PostDominator(LI.getDirectedGraph());
		init();
	}

	/**
	 * Initialize heuristic values The list of all heuristics with their
	 * respective probabilities. Notice that the list respect the order given in
	 * the ProfileHeuristics enumeration. This order will be used to index this
	 * list.
	 */
	private void init() {
		float[] probTaken = { 0.88f, 0.60f, 0.78f, 0.84f, 0.80f, 0.72f, 0.55f,
				0.75f, 0.62f };
		float[] probNotTaken = { 0.12f, 0.40f, 0.22f, 0.16f, 0.20f, 0.28f,
				0.45f, 0.25f, 0.38f };
		String[] desc = { "Loop Branch Heuristic", "Pointer Heuristic",
				"Call Heuristic", "Opcode Heuristic", "Loop Exit Heuristic",
				"Return Heuristic", "Store Heuristic", "Loop Header Heuristic",
				"Guard Heuristic" };
		for (int i = 0; i < 9; i++) {
			probList[i] = new BranchProbabilities(i, probTaken[i],
					probNotTaken[i], desc[i]);
		}
	}

	@Override
	public float getProbabilityTaken(int heuristic) {
		return probList[heuristic].probabilityTaken;
	}

	@Override
	public float getProbabilityNotTaken(int heuristic) {
		return probList[heuristic].probabilityNotTaken;
	}

	/**
	 * MatchHeuristic - Wrapper for the heuristics handlers meet above. This
	 * procedure assumes that root basic block has exactly two successors.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 */
	@Override
	public Pair<Block, Block> MatchHeuristic(int bh, Block root) {
		// Try to match the heuristic bh with their respective handler.
		switch (bh) {
		case BranchHeuristics.LOOP_BRANCH_HEURISTIC:
			return MatchLoopBranchHeuristic(root);
		case BranchHeuristics.POINTER_HEURISTIC:
			return MatchPointerHeuristic(root);
		case BranchHeuristics.CALL_HEURISTIC:
			return MatchCallHeuristic(root);
		case BranchHeuristics.OPCODE_HEURISTIC:
			return MatchOpcodeHeuristic(root);
		case BranchHeuristics.LOOP_EXIT_HEURISTIC:
			return MatchLoopExitHeuristic(root);
		case BranchHeuristics.RETURN_HEURISTIC:
			return MatchReturnHeuristic(root);
		case BranchHeuristics.STORE_HEURISTIC:
			return MatchStoreHeuristic(root);
		case BranchHeuristics.LOOP_HEADER_HEURISTIC:
			return MatchLoopHeaderHeuristic(root);
		case BranchHeuristics.GUARD_HEURISTIC:
			return MatchGuardHeuristic(root);
		default:
			// Unknown heuristic.
			// Should never happen.
			return null;
		}
	}

	/**
	 * MatchLoopBranchHeuristic - Predict as taken an edge back to a loop's
	 * head. Predict as not taken an edge exiting a loop.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 */
	@Override
	public Pair<Block, Block> MatchLoopBranchHeuristic(Block rootBasicBlock) {
		boolean matched = false;
		Pair<Block, Block> prediction = null;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors. True and False branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = null;
		if (successors.size() > 1) 
			falseSuccessor = successors.get(1);

		// True and false branch edges.
		Pair<Block, Block> trueEdge = new Pair<Block, Block>(rootBasicBlock,
				trueSuccessor);
		Pair<Block, Block> falseEdge = new Pair<Block, Block>(rootBasicBlock,
				falseSuccessor);

		if (BPI.isBackEdge(trueEdge))
			System.out.println ("yes");
		
		// If the true branch is a back edge to a loop's head or the false
		// branch is
		// an exit edge, match the heuristic.
		if ((BPI.isBackEdge(trueEdge) && li.isLoopHeader(trueSuccessor
				.getHead())) || BPI.isExitEdge(falseEdge)) {
			matched = true;
			prediction = new Pair<Block, Block>(trueSuccessor, falseSuccessor);
		}

		if (falseSuccessor != null) {
			// Check the opposite situation, the other branch.
			if ((BPI.isBackEdge(falseEdge) && li.isLoopHeader(falseSuccessor
					.getHead())) || BPI.isExitEdge(trueEdge)) {
				if (matched)
					return null;
				matched = true;
				prediction = new Pair<Block, Block>(falseSuccessor, trueSuccessor);
			}
		}
		return prediction;
	}

	/**
	 * MatchPointerHeuristic - Predict that a comparison of a pointer against
	 * null or of two pointers will fail.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 */
	@Override
	public Pair<Block, Block> MatchPointerHeuristic(Block rootBasicBlock) {

		Pair<Block, Block> prediction = null;

		Unit tail = rootBasicBlock.getTail();
		// Is the instruction a Branch Instruction?
		if (!(tail instanceof JIfStmt))
			return null;

		JIfStmt unit = (JIfStmt) tail;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);
		Block trueSuccessor = successors.get(1);
		Block falseSuccessor = successors.get(0);

		// ValueBox vb = u.getConditionBox();
		// Value v = vb.getValue();
		// for(ValueBox vb : rootBasicBlock.getUseBoxes()){
		// Value v = vb.getValue();
		// Value condition
		// }
		Value v = unit.getCondition();
		if (v instanceof JEqExpr) {
			/*
			 * JEqExpr expr = (JEqExpr) v; Value op1 = expr.getOp1(); Value op2
			 * = expr.getOp2(); if((op1 instanceof NullConstant) || (op2
			 * instanceof NullConstant))
			 */
			// this block is less likely to take
			// any two pointer comparison or pointer comparison with null is
			// less likely
			prediction = new Pair<Block, Block>(falseSuccessor, trueSuccessor);
		} else if (v instanceof JNeExpr) {
			prediction = new Pair<Block, Block>(trueSuccessor, falseSuccessor);
		}

		return prediction;
	}

	/**
	 * MatchCallHeuristic - Predict a successor that contains a call and does
	 * not post-dominate will not be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchCallHeuristic(Block rootBasicBlock) {

		boolean matched = false;
		Pair<Block, Block> prediction = null;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors. True and False branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = null;
		if (successors.size() > 1) {
			falseSuccessor = successors.get(1);
		}

		// Check if the successor contains a call and does not post-dominate.
		if (BPI.hasCall(trueSuccessor)
				&& !PD.dominates(trueSuccessor, rootBasicBlock)) {
			matched = true;
			prediction = new Pair<Block, Block>(falseSuccessor, trueSuccessor);
		}

		// Check the opposite situation, the other branch.
		if (falseSuccessor != null) {
			if (BPI.hasCall(falseSuccessor)
					&& !PD.dominates(falseSuccessor, rootBasicBlock)) {

				// If the heuristic matches both branches, predict none.
				if (matched)
					return null;

				matched = true;
				prediction = new Pair<Block, Block>(trueSuccessor,
						falseSuccessor);
			}
		}
		return prediction;
	}

	/**
	 * Checks the type of the operand for opcode heuristics
	 * 
	 * @param v1
	 * @param v2
	 * @param checkValue
	 * @return
	 */
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

	/**
	 * MatchOpcodeHeuristic - Predict that a comparison of an integer for less
	 * than zero, less than or equal to zero, or equal to a constant, will fail.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchOpcodeHeuristic(Block rootBasicBlock) {
		Pair<Block, Block> prediction = null;

		// Get the last instruction of the basicblock
		Unit tail = rootBasicBlock.getTail();
		// Is the instruction a Branch Instruction?
		if (!(tail instanceof JIfStmt))
			return null;

		JIfStmt unit = (JIfStmt) tail;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors, the true and false branches.
		Block trueSuccessor = successors.get(1);
		Block falseSuccessor = successors.get(0);

		// The return successors (the first taken and the second not taken).
		Pair<Block, Block> falseEdge = new Pair<Block, Block>(falseSuccessor,
				trueSuccessor);
		Pair<Block, Block> trueEdge = new Pair<Block, Block>(trueSuccessor,
				falseSuccessor);

		Value v = unit.getCondition();

		// Check several comparison operators.
		// If it's a equal comparison against a constant integer, match.
		// if ($var == constant) or if (constant == $var).
		if (v instanceof JEqExpr) {
			JEqExpr expr = (JEqExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (checkOperands(op1, op2, false)) {
					return falseEdge;
				}

			} else if (op2 instanceof JimpleLocal) {
				if (checkOperands(op2, op1, false)) {
					return falseEdge;
				}

			}

		} else if (v instanceof JNeExpr) {
			// If it's a not equal comparison against a constant integer, match.
			// if ($var != constant) or if (constant != $var).
			JNeExpr expr = (JNeExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (checkOperands(op1, op2, false)) {
					return trueEdge;
				}

			} else if (op2 instanceof JimpleLocal) {
				if (checkOperands(op2, op1, false)) {
					return trueEdge;
				}

			}

		} else if (v instanceof JGtExpr) {
			// if ($var > 0) or if (0 > $var).
			JGtExpr expr = (JGtExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (checkOperands(op1, op2, true)) {
					return trueEdge;
				}

			} else if (op2 instanceof JimpleLocal) {
				if (checkOperands(op2, op1, true)) {
					return falseEdge;
				}

			}

		} else if (v instanceof JGeExpr) {
			// if ($var >= 0) or if (0 >= $var).
			JGeExpr expr = (JGeExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (checkOperands(op1, op2, true)) {
					return trueEdge;
				}

			} else if (op2 instanceof JimpleLocal) {
				if (checkOperands(op2, op1, true)) {
					return falseEdge;
				}

			}

		} else if (v instanceof JLtExpr) {
			// if ($var < 0) or if (0 < $var).
			JLtExpr expr = (JLtExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (checkOperands(op1, op2, true)) {
					return falseEdge;
				}

			} else if (op2 instanceof JimpleLocal) {
				if (checkOperands(op2, op1, true)) {
					return trueEdge;
				}

			}

		} else if (v instanceof JLeExpr) {
			// if ($var <= 0) or if (0 <= $var).
			JLeExpr expr = (JLeExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (checkOperands(op1, op2, true)) {
					return falseEdge;
				}

			} else if (op2 instanceof JimpleLocal) {
				if (checkOperands(op2, op1, true)) {
					return trueEdge;
				}

			}
		} else {
			// Do not process any other comparison operators.
			return null;
		}
		// Heuristic not matched.
		return null;
	}

	/**
	 * MatchLoopExitHeuristic - Predict that a comparison in a loop in which no
	 * successor is a loop head will not exit the loop.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchLoopExitHeuristic(Block rootBasicBlock) {
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors. True and False branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);

		Loop loop = li.returnContainingLoop(rootBasicBlock);

		// If there's a loop, check if neither of the branches are loop headers.
		if ((loop == null) || loop.getHead().equals(trueSuccessor.getHead())
				|| loop.getHead().equals(falseSuccessor.getHead()))
			return null;

		// True and false branch edges.
		Pair<Block, Block> trueEdge = new Pair<Block, Block>(rootBasicBlock,
				trueSuccessor);
		Pair<Block, Block> falseEdge = new Pair<Block, Block>(rootBasicBlock,
				falseSuccessor);

		// If it is an exit edge, successor will fail so predict the other
		// branch.
		// Note that is not possible for both successors to be exit edges.
		if (BPI.isExitEdge(trueEdge))
			return new Pair<Block, Block>(falseSuccessor, trueSuccessor);
		else if (BPI.isExitEdge(falseEdge))
			return new Pair<Block, Block>(trueSuccessor, falseSuccessor);

		return null;
	}

	/**
	 * MatchReturnHeuristic - Predict a successor that contains a return will
	 * not be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchReturnHeuristic(Block rootBasicBlock) {
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors. True and False branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = successors.get(1);

		boolean matched = false;
		Pair<Block, Block> prediction = null;

		// Check if the true successor it's a return instruction.
		if (hasReturn(trueSuccessor)) {
			matched = true;
			prediction = new Pair<Block, Block>(falseSuccessor, trueSuccessor);
		}

		// Check the opposite situation, the other branch.
		if (hasReturn(falseSuccessor)) {
			// If the heuristic matches both branches, predict none.
			if (matched)
				return null;
			matched = true;
			prediction = new Pair<Block, Block>(trueSuccessor, falseSuccessor);
		}
		return prediction;
	}
	
	private boolean hasReturn (Block block) {
		Iterator<Unit> blkUnitIter = block.iterator();
		while (blkUnitIter.hasNext()) {
			Unit unit = blkUnitIter.next();
			if ((unit instanceof JReturnStmt) || (unit instanceof JReturnVoidStmt))
					return true;
		}
		return false;
	}

	/**
	 * MatchStoreHeuristic - Predict a successor that contains a store
	 * instruction and does not post-dominate will not be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchStoreHeuristic(Block rootBasicBlock) {
		boolean matched = false;
		Pair<Block, Block> prediction = null;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors. True and False branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = successors.get(1);

		// Check if the successor contains a store and does not post-dominate.
		if (BPI.hasStore(trueSuccessor)
				&& !PD.dominates(trueSuccessor, rootBasicBlock)) {
			matched = true;
			prediction = new Pair<Block, Block>(falseSuccessor, trueSuccessor);
		}

		// Check the opposite situation, the other branch.
		if (BPI.hasStore(falseSuccessor)
				&& !PD.dominates(falseSuccessor, rootBasicBlock)) {
			// If the heuristic matches both branches, predict none.
			if (matched)
				return null;

			matched = true;
			prediction = new Pair<Block, Block>(trueSuccessor, falseSuccessor);
		}

		return prediction;
	}

	/**
	 * MatchLoopHeaderHeuristic - Predict a successor that is a loop header or a
	 * loop pre-header and does not post-dominate will be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchLoopHeaderHeuristic(Block rootBasicBlock) {
		boolean matched = false;
		Pair<Block, Block> prediction = null;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() < 2)
			return null;

		// Basic block successors. True and False branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);

		// Get the most inner loop in which the true successor basic block is
		// in.
		Loop loop = li.returnContainingLoop(trueSuccessor);

		// // Update loop prehead before the check
		// if (LI.loopPreHeader(loop, rootBasicBlock))
		// loop.setPreHeader((Stmt)rootBasicBlock);

		// Check if exists a loop, the true branch successor is a loop header or
		// a
		// loop pre-header, and does not post dominate.
		
		if ((loop != null)
				&& ((loop.getHead().toString().equals(trueSuccessor.getHead().toString()) || LI.loopPreHeader(
						trueSuccessor))
				&& !PD.dominates(trueSuccessor, rootBasicBlock))) {
			matched = true;
			prediction = new Pair<Block, Block>(trueSuccessor, falseSuccessor);
		}

		if (falseSuccessor != null) {
			// Get the most inner loop in which the false successor basic block is
			// in.
			loop = li.returnContainingLoop(falseSuccessor);
	
			// Check if exists a loop,
			// the false branch successor is a loop header or a loop pre-header, and
			// does not post dominate.
			if ((loop != null)
					&& ((loop.getHead().toString().equals(falseSuccessor.getHead().toString()) || LI.loopPreHeader(
							falseSuccessor))
					&& !PD.dominates(falseSuccessor, rootBasicBlock))) {
				// If the heuristic matches both branches, predict none.
				if (matched)
					return null;
				matched = true;
				prediction = new Pair<Block, Block>(falseSuccessor, trueSuccessor);
			}
		}
		return prediction;
	}

	/**
	 * MatchGuardHeuristic - Predict that a comparison in which a register is an
	 * operand, the register is used before being defined in a successor block,
	 * and the successor block does not post-dominate will reach the successor
	 * block.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	Pair<Block, Block> MatchGuardHeuristic(Block rootBasicBlock) {
		boolean matched = false;
		Pair<Block, Block> prediction = null;
		// Get the last instruction of the basicblock
		Unit tail = rootBasicBlock.getTail();
		// Is the instruction a Branch Instruction?
		if (!(tail instanceof JIfStmt))
			return null;

		JIfStmt unit = (JIfStmt) tail;
		LoopInfo li = BPI.getLoopInformation();
		List<Block> successors = li.getSuccessors(rootBasicBlock);

		if (successors.size() == 0)
			return null;

		// Basic block successors, the true and false branches.
		Block trueSuccessor = successors.get(0);
		Block falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);

		// The return successors (the first taken and the second not taken).
		Pair<Block, Block> falseEdge = new Pair<Block, Block>(falseSuccessor,
				trueSuccessor);
		Pair<Block, Block> trueEdge = new Pair<Block, Block>(trueSuccessor,
				falseSuccessor);

		Value v = unit.getCondition();

		// Make sure condition is of type JCmpExpr
		if (v instanceof AbstractBinopExpr) {
			AbstractBinopExpr expr = (AbstractBinopExpr) v;
			Value op1 = expr.getOp1();
			Value op2 = expr.getOp2();
			if (op1 instanceof JimpleLocal) {
				if (li.isParam(op1)) {
					// Check if this variable was used in the true successor and
					// does not post dominate.
					if (li.isUsedInBasicBlock(trueSuccessor)
							&& !PD.dominates(trueSuccessor, rootBasicBlock)) {
						matched = true;
						prediction = new Pair<Block, Block>(trueSuccessor,
								falseSuccessor);
					}

					if (falseSuccessor != null) {
						if (li.isUsedInBasicBlock(falseSuccessor)
								&& !PD.dominates(falseSuccessor, rootBasicBlock)) {
							// If the heuristic matches both branches, predict none.
							if (matched)
								return null;
							matched = true;
							prediction = new Pair<Block, Block>(falseSuccessor,
									trueSuccessor);
						}
					}
				}
			}

			if (op2 instanceof JimpleLocal) {
				if (li.isParam(op2)) {
					// Check if this variable was used in the true successor and
					// does not post dominate.
					if (li.isUsedInBasicBlock(trueSuccessor)
							&& !PD.dominates(trueSuccessor, rootBasicBlock)) {
						matched = true;
						prediction = new Pair<Block, Block>(trueSuccessor,
								falseSuccessor);
					}
					if (falseSuccessor != null) {
						if (li.isUsedInBasicBlock(falseSuccessor)
								&& !PD.dominates(falseSuccessor, rootBasicBlock)) {
							// If the heuristic matches both branches, predict none.
							if (matched)
								return null;
							matched = true;
							prediction = new Pair<Block, Block>(falseSuccessor,
									trueSuccessor);
						}
					}
				}
			}

		}
		return prediction;
	}
}
