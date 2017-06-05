package edu.iastate.cs.design.asymptotic.interfaces;
/**
 * Hold the information regarding the heuristic, the probability of taken and
 * not taken the branches and the heuristic name, for debugging purposes.
 * The probabilities were taken from Table 1 in Wu's (1994) paper.
 * Note that the enumeration order is respected, to allow a direct access to
 * the probabilities.
 * @author gupadhyaya
 *
 */
public class BranchProbabilities {
	
	int heuristic;
	// Both probabilities are represented in this structure for faster access.
	public float probabilityTaken;
	public float probabilityNotTaken; 
	String name;
	
	public BranchProbabilities (int h, float pt, float pnt, String n) {
		heuristic = h;
		probabilityTaken = pt;
		probabilityNotTaken = pnt;
		name = n;
	}

}
