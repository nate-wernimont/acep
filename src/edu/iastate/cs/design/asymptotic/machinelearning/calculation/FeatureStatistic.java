package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

public class FeatureStatistic {
	
	public enum Count {
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
	
	public enum Coverage {
		FIELDS				(0),
		FIELDS_WRITTEN		(1),
		INVOCATIONS			(2),
		LOCAL_VARIABLES		(3),
		PARAMETERS			(4);
		
		private int identifier;
		
		Coverage(int num){
			identifier = num;
		}
	}
	
	private int[] counts;
	
	private float[] coverages;
	
	public FeatureStatistic(){
		counts = new int[18];
		coverages = new float[5];
	}
	
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
	
	public void reset(){
		for(int i : counts){
			i = 0;
		}
		for(float f : coverages){
			f = 0;
		}
	}
	
	public void increment(Count feature){
		counts[feature.identifier]++;
	}
	
	public void increment(Count feature, int amount){
		counts[feature.identifier] += amount;
	}
	
	public int getValue(Count feature){
		return counts[feature.identifier];
	}
	
	public float getValue(Coverage coverage){
		return coverages[coverage.identifier];
	}
	
	public void setValue(Coverage coverage, float value){
		coverages[coverage.identifier] = value;
	}
	
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
