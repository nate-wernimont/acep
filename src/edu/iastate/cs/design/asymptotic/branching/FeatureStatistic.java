package edu.iastate.cs.design.asymptotic.branching;

public class FeatureStatistic {
	
	public enum Count {
		COMPARISONS				(0),
		NEW						(1),
		THIS					(2),
		ALL_VARIABLES			(3),
		ASSIGNMENTS				(4),
		DEREFERENCES			(5),
		FIELDS					(6),
		FIELDS_WRITTEN			(7),
		INVOCATIONS				(8),
		GOTO					(9),
		IF						(10),
		LOCAL_INVOCATIONS		(11),
		LOCAL_VARIABLES			(12),
		NON_LOCAL_INVOCATIONS	(13),
		PARAMETERS				(14),
		RETURN					(15),
		STATEMENTS				(16),
		THROW					(17);
		
		private int identifier;
		
		Count(int num){
			identifier = num;
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
			result += coverage.name()+": "+coverages[coverage.identifier]+"\n";
		}
		return result;
	}
	
}
