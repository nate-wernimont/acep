package edu.iastate.cs.design.asymptotic.datastructures;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.ShortType;
import soot.Type;
import soot.VoidType;
import soot.baf.WordType;

public class TypeReference {

	Type _type;
	
	public TypeReference (Type type) {
		_type = type;
	}
	
	public boolean isReferenceType() {
		return (_type instanceof RefType)?true:false;
	}
	
	public boolean isBooleanType() {
		return (_type instanceof BooleanType)?true:false;
	}
	
	public boolean isByteType() {
		return (_type instanceof ByteType)?true:false;
	}
	
	public boolean isShortType() {
		return (_type instanceof ShortType)?true:false;
	}
	
	public boolean isCharType() {
		return (_type instanceof CharType)?true:false;
	}
	
	public boolean isIntType() {
		return (_type instanceof IntType)?true:false;
	}
	
	public boolean isFloatType() {
		return (_type instanceof FloatType)?true:false;
	}
	
	public boolean isWordLikeType() {
		return (_type instanceof WordType)?true:false;
	}
	
	public boolean isVoidType() {
		return (_type instanceof VoidType)?true:false;
	}
	
	public boolean isLongType() {
		return (_type instanceof LongType)?true:false;
	}
	
	public boolean isDoubleType() {
		return (_type instanceof DoubleType)?true:false;
	}
}
