package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.List;

public class MethodReference extends MemberReference {

	List<TypeReference> _paramTypes;
	TypeReference _returnType;
	
	public MethodReference (List<TypeReference> paramTypes, TypeReference returnType) {
		_paramTypes = paramTypes;
		_returnType = returnType;
	}
	
	public TypeReference getReturnType () {
		return _returnType;
	}
	
	public int getParameterWords () {
		return 0;
	}
	
	public List<TypeReference> getParameterTypes () {
		return _paramTypes;
	}
	
	public boolean isMiranda() {
		return false;
	}
	
	public int getId () {
		return 0;
	}
	
	public RVMMethod peekInterfaceMethod () {
		return null;
	}
}
