package edu.iastate.cs.design.asymptotic.interfaces;
/**
 * All possible branch heuristics.
 * @author gupadhyaya
 *
 */
public interface BranchHeuristics {
	
	public static final int LOOP_BRANCH_HEURISTIC = 0;
	public static final int POINTER_HEURISTIC = 1;
	public static final int CALL_HEURISTIC = 2;
	public static final int OPCODE_HEURISTIC = 3;
	public static final int LOOP_EXIT_HEURISTIC = 4;
	public static final int RETURN_HEURISTIC = 5;
	public static final int STORE_HEURISTIC = 6;
	public static final int LOOP_HEADER_HEURISTIC = 7;
	public static final int GUARD_HEURISTIC = 8;

}
