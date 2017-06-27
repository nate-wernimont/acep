package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

/**
 * 
 * @author Nate Wernimont
 * An object for storing the feature information of paths
 */
public class FeatureStatistic {
	
	public interface Feature {
		int identifier = 0;
	}
	
	/**
	 * 
	 * @author nate
	 * All of the features that are counted
	 *
	 */
	public enum Count implements Feature {
		COMPARISONS				,
		NEW						,
		THIS					,
		ALL_VARIABLES			,
		ASSIGNMENTS				,
		DEREFERENCES			,
		FIELDS					,
		FIELDS_WRITTEN			,
		INVOCATIONS				,
		GOTO					,
		IF						,
		LOCAL_INVOCATIONS		,
		LOCAL_VARIABLES			,
		NON_LOCAL_INVOCATIONS	,
		PARAMETERS				,
		RETURN					,
		STATEMENTS				,
		THROW					;
		
		private int identifier;
		
		Count(){
			identifier = this.ordinal();
		}
	}
	
	/**
	 * 
	 * All of the features that are coverages.
	 * @author nate
	 *
	 */
	public enum Coverage implements Feature {
		FIELDS				,
		FIELDS_WRITTEN		,
		INVOCATIONS			,
		LOCAL_VARIABLES		,
		PARAMETERS			;
		
		private int identifier;
		
		Coverage(){
			identifier = this.ordinal();
		}
	}
	
	/**
	 * The backing of all of the counts
	 */
	private int[] counts;
	
	/**
	 * The backing of all of the coverages
	 */
	private float[] coverages;
	
	/**
	 * Initialize a new FeatureStatistic
	 */
	public FeatureStatistic(){
		counts = new int[18];
		coverages = new float[5];
	}
	
	/**
	 * Copy constructor
	 * @param that The FeatureStatistic to copy
	 */
	public FeatureStatistic(FeatureStatistic that){
		this();
		if(that == null)
			throw new NullPointerException();
		for(int i = 0; i < 18; i++){
			this.counts[i] = that.counts[i];
		}
		for(int i = 0; i < 5; i++){
			this.coverages[i] = that.coverages[i]; 
		}
	}
	
	/**
	 * Reset the counts and coverages
	 */
	public void reset(){
		for(int i : counts){
			i = 0;
		}
		for(float f : coverages){
			f = 0;
		}
	}
	
	/**
	 * Increment the given feature by 1
	 * @param feature Feature to increment
	 */
	public void increment(Count feature){
		counts[feature.identifier]++;
	}
	
	/**
	 * Increment the given feature by amount
	 * @param feature Feature to increment
	 * @param amount The amount to increment it by
	 */
	public void increment(Count feature, int amount){
		counts[feature.identifier] += amount;
	}
	
	/**
	 * Fetch the value of the given feature
	 * @param feature Feature to fetch the value of
	 * @return The value of feature
	 */
	public Number getValue(Feature feature){
		if(feature instanceof Count)
			return counts[feature.identifier];
		else
			return coverages[feature.identifier];
	}
	
	/**
	 * Set the value of the coverage
	 * @param coverage The coverage to set
	 * @param value The value to set it to
	 */
	public void setValue(Coverage coverage, float value){
		coverages[coverage.identifier] = value;
	}
	
	/**
	 * Converts this object to a string format
	 */
	public String toString(){
		String result = "====Counts====\n";
		for(Count feature : Count.values()){
			result += feature.name()+": "+counts[feature.identifier]+"\n";
		}
		result += "\n====Coverages====\n";
		for(Coverage coverage : Coverage.values()){
			result += String.format("%s: %.3f\n",coverage.name(),coverages[coverage.identifier]);
		}
		return result;
	}
	
}
