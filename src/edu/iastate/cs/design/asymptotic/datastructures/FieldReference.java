package edu.iastate.cs.design.asymptotic.datastructures;

public class FieldReference extends MemberReference {
	TypeReference _type;
	public FieldReference (TypeReference type) {
		_type = type;
	}
	public int getSize() {
		return 1;
	}
	public int getNumberOfStackSlots() {
		return 0;
	}
	
	public TypeReference getFieldContentsType () {
		return _type;
	}
	
}
