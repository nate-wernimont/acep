package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.HashMap;
import java.util.Map.Entry;

public class ByteCodeToHashCode {
	public final String[] jbc = { "push", "aconst_null", "iconst", "lconst",
			"fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1", "ldc",
			"ldc2", "iload", "fload", "aload", "lload", "dload", "istore",
			"fstore", "astore", "lstore", "dstore", "iaload", "faload",
			"aaload", "caload", "saload", "baload", "laload", "daload",
			"iastore", "fastore", "aastore", "castore", "sastore", "bastore",
			"lastore", "dastore", "pop", "pop2", "dup", "dup_x1", "dup_x2",
			"dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "isub", "imul",
			"idiv", "irem", "ineg", "ishl", "ishr", "iushr", "iand", "ior",
			"ixor", "iinc", "ladd", "lsub", "lmul", "ldiv", "lrem", "lneg",
			"lshl", "lshr", "lushr", "land", "lor", "lxor", "fadd", "fsub",
			"fmul", "fdiv", "frem", "fneg", "dadd", "dsub", "dmul", "ddiv",
			"drem", "dneg", "i2l", "l2i", "i2f", "i2d", "l2f", "l2d", "f2d",
			"d2f", "f2i", "f2l", "d2i", "d2l", "i2b", "i2c", "i2s", "lcmp",
			"fcmpl", "fcmpg", "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge",
			"ifgt", "ifle", "icmpeq", "icmpne", "icmplt", "icmpge", "icmpgt",
			"icmple", "acmpeq", "acmpne", "ifnull", "ifnonnull", "goto", "jsr",
			"ret", "tableswitch", "lookupswitch", "ireturn", "lreturn",
			"freturn", "dreturn", "areturn", "return", "getstatic",
			"putstatic", "getfield", "putfield", "invokevirtual",
			"invokespecial", "invokestatic", "invokeinterface", "new",
			"newarray", "multianewarray", "arraylength", "athrow", "checkcast",
			"resolvedInterface", "resolvedClass", "checkcast_final",
			"instanceof", "instanceof_resolvedInterface",
			"instanceof_resolvedClass", "instanceof_final", "monitorenter",
			"monitorexit", "invoke_compiledmethod", "loadretaddrconst",
			"pending_goto" };
	
	private HashMap<String, Integer> jbchc = new HashMap<String, Integer>();
	
	public ByteCodeToHashCode () {
		populate();
	}

	public int getHashCode(String bytecode) {
		if (jbchc.containsKey(bytecode))
			return jbchc.get(bytecode).intValue();
		return -1;
	}

	private void populate() {
		for (String bytecode : jbc) {
			jbchc.put(bytecode, new Integer(bytecode.hashCode()));
		}
	}

//	public static void main(String[] args) {
//		populate();
//		display();
//	}
//
//	public static void display() {
////		while (jbchc.entrySet().iterator().hasNext()) {
////			Entry<String, Integer> entry = jbchc.entrySet().iterator().next();
////			System.out.println("key: " + entry.getKey() + "-------- value: "
////					+ entry.getValue().intValue());
////		}
//		System.out.println (getHashCode("fcmpg"));
//	}

}
