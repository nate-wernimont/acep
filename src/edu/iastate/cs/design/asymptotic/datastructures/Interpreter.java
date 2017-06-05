package edu.iastate.cs.design.asymptotic.datastructures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.ShortType;
import soot.SootFieldRef;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.baf.Inst;
import soot.baf.internal.BAddInst;
import soot.baf.internal.BAndInst;
import soot.baf.internal.BArrayLengthInst;
import soot.baf.internal.BArrayReadInst;
import soot.baf.internal.BArrayWriteInst;
import soot.baf.internal.BCmpInst;
import soot.baf.internal.BCmpgInst;
import soot.baf.internal.BCmplInst;
import soot.baf.internal.BDivInst;
import soot.baf.internal.BDup1Inst;
import soot.baf.internal.BDup1_x1Inst;
import soot.baf.internal.BDup1_x2Inst;
import soot.baf.internal.BDup2Inst;
import soot.baf.internal.BDup2_x1Inst;
import soot.baf.internal.BDup2_x2Inst;
import soot.baf.internal.BDupInst;
import soot.baf.internal.BEnterMonitorInst;
import soot.baf.internal.BExitMonitorInst;
import soot.baf.internal.BFieldGetInst;
import soot.baf.internal.BFieldPutInst;
import soot.baf.internal.BGotoInst;
import soot.baf.internal.BIdentityInst;
import soot.baf.internal.BIfCmpEqInst;
import soot.baf.internal.BIfCmpGeInst;
import soot.baf.internal.BIfCmpGtInst;
import soot.baf.internal.BIfCmpLeInst;
import soot.baf.internal.BIfCmpLtInst;
import soot.baf.internal.BIfCmpNeInst;
import soot.baf.internal.BIfEqInst;
import soot.baf.internal.BIfGeInst;
import soot.baf.internal.BIfGtInst;
import soot.baf.internal.BIfLeInst;
import soot.baf.internal.BIfLtInst;
import soot.baf.internal.BIfNeInst;
import soot.baf.internal.BIfNonNullInst;
import soot.baf.internal.BIfNullInst;
import soot.baf.internal.BIncInst;
import soot.baf.internal.BInstanceCastInst;
import soot.baf.internal.BInstanceOfInst;
import soot.baf.internal.BInterfaceInvokeInst;
import soot.baf.internal.BJSRInst;
import soot.baf.internal.BLoadInst;
import soot.baf.internal.BLookupSwitchInst;
import soot.baf.internal.BMulInst;
import soot.baf.internal.BNegInst;
import soot.baf.internal.BNewArrayInst;
import soot.baf.internal.BNewInst;
import soot.baf.internal.BNewMultiArrayInst;
import soot.baf.internal.BNopInst;
import soot.baf.internal.BOrInst;
import soot.baf.internal.BPopInst;
import soot.baf.internal.BPrimitiveCastInst;
import soot.baf.internal.BPushInst;
import soot.baf.internal.BRemInst;
import soot.baf.internal.BReturnInst;
import soot.baf.internal.BReturnVoidInst;
import soot.baf.internal.BShlInst;
import soot.baf.internal.BShrInst;
import soot.baf.internal.BSpecialInvokeInst;
import soot.baf.internal.BStaticGetInst;
import soot.baf.internal.BStaticInvokeInst;
import soot.baf.internal.BStaticPutInst;
import soot.baf.internal.BStoreInst;
import soot.baf.internal.BSubInst;
import soot.baf.internal.BSwapInst;
import soot.baf.internal.BTableSwitchInst;
import soot.baf.internal.BThrowInst;
import soot.baf.internal.BTrap;
import soot.baf.internal.BUshrInst;
import soot.baf.internal.BVirtualInvokeInst;
import soot.baf.internal.BXorInst;
import soot.dava.toolkits.base.AST.transformations.ExtraLabelNamesRemover;
//import sun.management.OperatingSystemImpl;

/**
 * This class has to perform three level of mapping 1. Soot.baf bytecode -> Java
 * bytecode 2. Java bytecode -> Machine code 3. Machine code -> CPU cycles
 * 
 * @author gupadhyaya
 * 
 */
public class Interpreter {

	private final String AddrJVM_32_or_64;
	private int WORDSIZE;
	private int LG_WORDSIZE;
	private final boolean SSE2_BASE = true;
	private final boolean SSE2_FULL = true;
	private final int NEEDS_DYNAMIC_LINK = Short.MIN_VALUE + 1;
	private final String ERROR = "Can't compute cycles, please check your instruction";
	private final int NUM_PARAMETER_FPRS = 8;
	private final int NUM_PARAMETER_GPRS = 3;
	int ARRAY_LENGTH_BYTES;
	int LOG_BYTES_IN_ADDRESS;
	int BYTES_IN_ADDRESS;
	private ByteCodeToHashCode _converter;
	int BYTES_IN_INT = 1 << 2;

	public Interpreter() {
		// Setting some constants
		AddrJVM_32_or_64 = System.getProperty("sun.arch.data.model");
		WORDSIZE = AddrJVM_32_or_64.equals("32") ? 4 : 8; // bytes
		LG_WORDSIZE = AddrJVM_32_or_64.equals("32") ? 2 : 3;
		LOG_BYTES_IN_ADDRESS = AddrJVM_32_or_64.equals("32") ? 2 : 3;
		BYTES_IN_ADDRESS = 1 << LOG_BYTES_IN_ADDRESS;
		ARRAY_LENGTH_BYTES = AddrJVM_32_or_64.equals("64") ? BYTES_IN_ADDRESS : BYTES_IN_INT;
		_converter = new ByteCodeToHashCode();
	}

	private int getNumOperandParts(char[] opcode) {
		String opCodeStr = new String(opcode);
		String[] opCodeParts = opCodeStr.split("\\s");
		return (opCodeParts.length - 1);
	}

	private String[] getOperands(char[] opcode) {
		String opCodeStr = new String(opcode);
		String[] opCodeParts = opCodeStr.split("\\s");
		String[] returnParts = new String[4];
		for (int i = 1; i < opCodeParts.length; i++) {
			returnParts[i - 1] = opCodeParts[i];
		}
		return returnParts;
	}

	/**
	 * Register value will start with '%'
	 * 
	 * @param operandStr
	 * @return
	 */
	private boolean isImmediateRegister(String operandStr) {
		char[] operand = operandStr.toCharArray();
		if (operand[0] == '%')
			return true;
		return false;
	}

	/**
	 * ImmediateValue will start with '$'
	 * 
	 * @param operandStr
	 * @return
	 */
	private boolean isImmediateValue(String operandStr) {
		char[] operand = operandStr.toCharArray();
		if (operand[0] == '$')
			return true;
		return false;
	}

	/**
	 * 
	 * @param msg
	 */
	private void END_CYCLE_COUNT(String msg) {
		throw new RuntimeException(msg);
	}

	/**
	 * Get opcode cycles required for machine code
	 * 
	 * @param argStr
	 * @return
	 */
	private double getOpCycles(char[] argStr) {
		// Need to implement code given by Tyler
		// return 0.0;
		double cycOverBase = 0.0;
		char[] opToCmp = null;
		int opParts;
		if (argStr != null)
			opToCmp = argStr;
		if (opToCmp == null)
			return -1;
		switch (opToCmp[0]) {
		case 'a':
			switch (opToCmp[1]) {
			case 'd':
				switch (opToCmp[2]) {
				case 'c':
				case 'd':
					/*
					 * FLIPPED! we analyze AT&T, table is for Intel reg,reg 1
					 * mem,reg 3 reg,mem 2 reg,immed 1 mem,immed 3 accum,immed 1
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 2 for add");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						// reg,reg 1
						if (isImmediateRegister(operands[0]))
							return 1.0;
						// reg,immed 1
						// accum,immed 1
						else if (isImmediateValue(operands[0]))
							return 1.0;
						// reg,mem 2
						else {
							cycOverBase = 1.0;
							return 2.0;
						}
					} else {
						// mem,reg 3
						// mem,immed 3
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				} // end switch ad-
			case 'n':
				switch (opToCmp[2]) {
				case 'd':
					/*
					 * FLIPPED! we analyze AT&T, table is for Intel reg,reg 1
					 * mem,reg 3 reg,mem 1 reg,immed 1 mem,immed 3 accum,immed 1
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 2 for and");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						// reg,reg 1
						// reg,immed 1
						// accum,immed 1
						// reg,mem 1
						return 1.0;
					} else {
						// mem,reg 3
						// mem,immed 3
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				} // end switch an-
			default:
				END_CYCLE_COUNT(ERROR);
			} // end switch a-
		case 'b':
			switch (opToCmp[1]) {
			case 's':
				switch (opToCmp[2]) {
				case 'f': // bsf
				case 'r': // bsr
					// using small side of eec time... not sure why it owuld
					// take 100 cycles to do bsr
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 2 for bsr");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0]))
						return 6.0;
					else {
						cycOverBase = 1.0;
						return 7.0;
					}

				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 't':
				switch (opToCmp[2]) {
				case 's':
					/*
					 * FLIPPED! we analyze AT&T, table is for Intel reg,immed 6
					 * reg,reg 6 mem,immed 8 mem,reg 13
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 2 for bst");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						return 6.0;
					} else {
						if (isImmediateValue(operands[0])) {
							cycOverBase = 2.0;
							return 8.0;
						} else {
							cycOverBase = 7.0;
							return 13.0;
						}
					}
				case '\0':
					// reg16,immed8 3
					// reg16,reg16 3
					// mem16,immed8 6
					// mem16,reg16 12
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 2 for bt");
					}
					String[] operands1 = getOperands(opToCmp);
					if (isImmediateRegister(operands1[1])) {
						return 3.0;
					} else {
						if (isImmediateValue(operands1[0])) {
							cycOverBase = 3.0;
							return 6.0;
						} else {
							cycOverBase = 9.0;
							return 12.0;
						}
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			default:
				// END_CYCLE_COUNT(ERROR);
				// reg16,immed8 3
				// reg16,reg16 3
				// mem16,immed8 6
				// mem16,reg16 12
				opParts = getNumOperandParts(opToCmp);
				if (opParts != 2) {
					END_CYCLE_COUNT(opParts
							+ "operand parts instead of 2 for bt");
				}
				String[] operands1 = getOperands(opToCmp);
				if (isImmediateRegister(operands1[1])) {
					return 3.0;
				} else {
					if (isImmediateValue(operands1[0])) {
						cycOverBase = 3.0;
						return 6.0;
					} else {
						cycOverBase = 9.0;
						return 12.0;
					}
				}
			}
		case 'c':
			switch (opToCmp[1]) {
			case 'a':
				switch (opToCmp[2]) {
				case 'l':
					/*
					 * rel16 (near, IP relative) 3 rel32 (near, IP relative) 3
					 * 
					 * reg16 (near, register indirect) 5 reg32 (near, register
					 * indirect) 5
					 * 
					 * mem16 (near, memory indirect) 5 mem32 (near, memory
					 * indirect) 5
					 * 
					 * ptr16:16 (far, full ptr supplied) 18 ptr16:32 (far, full
					 * ptr supplied) 18 ptr16:16 (far, ptr supplied, prot. mode)
					 * 20 ptr16:32 (far, ptr supplied, prot. mode) 20 m16:16
					 * (far, indirect) 17 m16:32 (far, indirect) 17 m16:16 (far,
					 * indirect, prot. mode) 20 m16:32 (far, indirect, prot.
					 * mode) 20
					 * 
					 * ptr16:16 (task, via TSS or task gate) 37+TS m16:16 (task,
					 * via TSS or task gate) 37+TS m16:32 (task) 37+TS m16:32
					 * (task) 37+TS
					 * 
					 * ptr16:16 (gate, same privilege) 35 ptr16:32 (gate, same
					 * privilege) 35 m16:16 (gate, same privilege) 35 m16:32
					 * (gate, same privilege) 35
					 * 
					 * ptr16:16 (gate, more priv, no parm) 69 ptr16:32 (gate,
					 * more priv, no parm) 69 m16:16 (gate, more priv, no parm)
					 * 69 m16:32 (gate, more priv, no parm) 69
					 * 
					 * ptr16:16 (gate, more priv, x parms) 77+4x ptr16:32 (gate,
					 * more priv, x parms) 77+4x m16:16 (gate, more priv, x
					 * parms) 77+4x m16:32 (gate, more priv, x parms) 77+4x
					 */
					return 20.0;
				default:
					END_CYCLE_COUNT(ERROR);
				} // end switch ca-
			case 'l':
				switch (opToCmp[2]) {
				case 'd':
					return 2.0;
				case 't':
					switch (opToCmp[3]) {
					case 'd': // cltd
						return 5.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'm':
				switch (opToCmp[2]) {
				case 'o': // cmo
					// from practical
					return 10.0;
				case 'p': // cmp
					/*
					 * reg,reg 1 reg,immed 1 reg,mem 2 accum,immed 1 mem,reg 2
					 * mem,immed 2
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 2 for cmp");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						// reg,reg 1
						// reg,immed 1
						// accum,immed 1
						// reg,mem 2
						if (isImmediateRegister(operands[0]))
							return 1.0;
						else if (isImmediateValue(operands[0]))
							return 1.0;
						else {
							cycOverBase = 1.0;
							return 2.0;
						}
					} else {
						// mem,reg 2
						// mem,immed 2
						return 2.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch cm-
			case 'w': // cw
				return 3.0;
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch c-
		case 'd':
			switch (opToCmp[1]) {
			case 'e':
				switch (opToCmp[2]) {
				case 'c': // dec
					/*
					 * reg8 1 mem 3 reg16/32 1
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 1 for dec");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						// reg8 1
						// reg16/32 1
						return 1.0;
					} else {
						// mem 3
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch de-
			case 'i':
				switch (opToCmp[2]) {
				case 'v': // div
					/*
					 * reg8 16 reg16 24 reg32 40 mem8 16 mem16 24 mem32 40
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 1 for div");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						char[] operand0 = operands[0].toCharArray();
						if (operand0[1] == 'e') {
							// reg32 40
							return 40.0;
						} else if (operand0[2] == 'l') {
							// reg8 16
							// END_CYCLE_COUNT("NOT 32 bit divide?\n");
							return 16.0;
						} else {
							// reg16 24
							// END_CYCLE_COUNT("NOT 32 bit divide?\n");
							return 40.0;
						}
					} else {
						// mem8 16
						// mem16 24
						// mem32 40
						return 40.0; // assume worst b/c most times will be i
						// think
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch di-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch d-
		case 'f':
			switch (opToCmp[1]) {
			case 'a':
				switch (opToCmp[2]) {
				case 'b':
					switch (opToCmp[3]) {
					case 's': // fabs
						return 3.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				case 'd':
					switch (opToCmp[3]) {
					case 'd': // fadd
						switch (opToCmp[4]) {
						case 'l':
							return 20.0;
						case 'p': // faddp
							return 20.0;
							// missing
						default:
							return 20.0;
						}
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'c':
				switch (opToCmp[2]) {
				case 'h':
					return 6.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'd':
				switch (opToCmp[2]) {
				case 'i':
					switch (opToCmp[3]) {
					case 'v':
						return 73.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'i':
				switch (opToCmp[2]) {
				case 'a':
					// fiadd
					return 32.0;
				case 'd':
					switch (opToCmp[3]) {
					case 'i':
						switch (opToCmp[4]) {
						case 'v': // fidiv
							return 87.0;
						default:
							END_CYCLE_COUNT(ERROR);
						}
					default:
						END_CYCLE_COUNT(ERROR);
					}
				case 'l':
					switch (opToCmp[3]) {
					case 'd': // fild
						switch (opToCmp[4]) {
						case 'l': // fildl
							return 18.0;
						case '\0':
							return 12.0;
							// missing r (18), l (12), w (16) and b versions
						default:
							END_CYCLE_COUNT(ERROR);
						}
					default:
						return 12.0;// END_CYCLE_COUNT(ERROR);
					}
				case 'm':
					// fimul
					return 27.0;
				case 's':
					// fisubrl
					return 32.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'l':
				switch (opToCmp[2]) {
				case 'd':
					switch (opToCmp[3]) {
					case 'c': // fldcd
						return 4.0;
					case 'z': // fldz
					case '1': // fldz
						return 4.0;
					case 'l': // fldl
					case 's':
					case '\0':
						// 4 if reg
						// 3 if small mem
						// 6 is 80 mem
						opParts = getNumOperandParts(opToCmp);
						if (opParts != 1) {
							END_CYCLE_COUNT(opParts
									+ "operand parts instead of 1 for fld\n");
						}
						String[] operands = getOperands(opToCmp);
						if (isImmediateRegister(operands[0])) {
							return 4.0;
						} else {
							cycOverBase = 2.0;
							return 6.0;
						}
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					// END_CYCLE_COUNT(ERROR);
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ "operand parts instead of 1 for fld\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						return 4.0;
					} else {
						cycOverBase = 2.0;
						return 6.0;
					}
				}
			case 'm':
				switch (opToCmp[2]) {
				case 'u':
					switch (opToCmp[3]) {
					case 'l': // fmul
						switch (opToCmp[4]) {
						case 'p': // fmulp
							return 16.0;
						default:
							return 16.0;
						}
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'n':
				switch (opToCmp[2]) {
				case 's':
					switch (opToCmp[3]) {
					case 't': // fnst
						return 3.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'r': // fr
				switch (opToCmp[2]) {
				case 'n': // frn
					return 25.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 's':
				switch (opToCmp[2]) {
				case 'q':
					// fsqrt
					return 87.0;
				case 't':
					switch (opToCmp[3]) {
					case 'l':
						opParts = getNumOperandParts(opToCmp);
						if (opParts != 1) {
							END_CYCLE_COUNT(opParts
									+ "operand parts instead of 1 for fstl\n");
						}
						String[] operands = getOperands(opToCmp);
						if (isImmediateRegister(operands[0])) {
							return 3.0;
						} else {
							return 7.0;
						}
					case 'p':
						/*
						 * reg 1 mem32 2 mem64 2 mem80 3
						 */
						opParts = getNumOperandParts(opToCmp);
						if (opParts != 1) {
							END_CYCLE_COUNT(opParts
									+ "operand parts instead of 1 for fstp\n");
						}
						String[] operands1 = getOperands(opToCmp);
						if (isImmediateRegister(operands1[0])) {
							// reg 1
							return 1.0;
						} else {
							// assuming 80 bit for float reg size
							cycOverBase = 2.0;
							return 3.0;
						}
					case 's': // fsts
						return 3.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}// end switch fst-
				case 'u':
					// fsub
					return 20.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch fs-
			case 'u':
				switch (opToCmp[2]) {
				case 'c':
					switch (opToCmp[3]) {
					case 'o': // fucom
						return 4.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'x':
				switch (opToCmp[2]) {
				case 'a': // fxam
					return 8.0;
				case 'c':
					switch (opToCmp[3]) {
					case 'h': // fxch
						return 4.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch f-
		case 'i':
			switch (opToCmp[1]) {
			case 'd':
				switch (opToCmp[2]) {
				case 'i': // idiv
					/*
					 * reg8 19 reg16 27 reg32 43 mem8 20 mem16 28 mem32 44
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 1 for idiv\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						char[] operand0 = operands[0].toCharArray();
						if (operand0[1] == 'e') {
							// reg32 43
							return 43.0;
						} else if (operand0[2] == 'l') {
							// reg8 19
							//END_CYCLE_COUNT("NOT 32 bit idivide?\n");
							return 19.0;
						} else {
							// reg16 27
							//END_CYCLE_COUNT("NOT 32 bit idivide?\n");
							return 43.0;
						}

					} else { // assume 32 bit
						cycOverBase = 1.0;
						return 44.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch id-
			case 'n':
				switch (opToCmp[2]) {
				case 'c': // inc
					/*
					 * reg8 1 reg16 1 reg32 1 mem 3
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 1 for inc\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						return 1.0;
					} else {
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch in-
			case 'm':
				switch (opToCmp[2]) {
				case 'u': // imul
					/*
					 * reg8 13-18 reg16 13-26 reg32 12-42 mem8 13-18 mem16 13-26
					 * mem32 13-42 reg16,reg16 13-26 reg32,reg32 13-42
					 * reg16,mem16 13-26 reg32,mem32 13-42 reg16,immed 13-26
					 * reg32,immed 13-42 reg16,reg16,immed 13-26
					 * reg32,reg32,immed 13-42 reg16,mem16,immed 13-26
					 * reg32,mem32,immed 13-42
					 */
					opParts = getNumOperandParts(opToCmp);
					String[] operands = getOperands(opToCmp);
					if (opParts == 1) {
						// reg8 13-18
						// reg16 13-26
						// reg32 12-42
						// mem8 13-18
						// mem16 13-26
						// mem32 13-42
						if (isImmediateRegister(operands[0])) {
							char[] operand0 = operands[0].toCharArray();
							if (operand0[1] == 'e') {
								// reg32 12-42
								return 42.0;
							} else if (operand0[2] == 'l') {
								// reg8 13-26
								//END_CYCLE_COUNT("NOT 32 bit imul?\n");
								return 26.0;
							} else {
								// reg16 13-18
								//END_CYCLE_COUNT("NOT 32 bit imul?\n");
								return 42.0;// return 18.0;
							}
						} else {
							cycOverBase = 1.0;
							return 42.0; // assume mem32
						}
					} // end opParts == 1
					else if (opParts == 2) {
						// reg16,reg16 13-26
						// reg32,reg32 13-42
						// reg16,mem16 13-26
						// reg32,mem32 13-42
						// reg16,immed 13-26
						// reg32,immed 13-42
						if (isImmediateRegister(operands[1])) {
							char[] operand1 = operands[1].toCharArray();
							if (operand1[1] == 'e') {
								// reg32 12-42
								return 42.0;
							} else if (operand1[2] == 'l') {
								// reg8 13-26
								END_CYCLE_COUNT("NOT 32 bit imul?\n");
								return 26.0;
							} else {
								// reg16 13-18
								// END_CYCLE_COUNT("NOT 32 bit imul?\n");
								return 42.0;//return 18.0;
							}
						} else {
							END_CYCLE_COUNT("imu, two op, dst not reg?\n");
						}
					} else { // opParts == 3
						// reg16,reg16,immed 13-26
						// reg32,reg32,immed 13-42
						// reg16,mem16,immed 13-26
						// reg32,mem32,immed 13-42
						if (isImmediateRegister(operands[2])) {
							char[] operand2 = operands[2].toCharArray();
							if (operand2[1] == 'e') {
								// reg32 12-42
								return 42.0;
							} else if (operand2[2] == 'l') {
								// reg8 13-26
								END_CYCLE_COUNT("NOT 32 bit imul?\n");
								return 26.0;
							} else {
								// reg16 13-18
								//END_CYCLE_COUNT("NOT 32 bit imul?\n");
								return 42.0;//return 18.0;
							}
						} else {
							END_CYCLE_COUNT("imu, three op, dst not reg?\n");
						}
					}
					return 42.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch im-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch i-
		case 'j':
			switch (opToCmp[1]) {
			case 'm': // jmp
				return 5.0;
			default:
				return 3.0;
			}// end switch j-
		case 'l':
			switch (opToCmp[1]) {
			case 'e':
				switch (opToCmp[2]) {
				case 'a': // lea
					return 1.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch le-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch l-
		case 'm':
			switch (opToCmp[1]) {
			case 'o':
				switch (opToCmp[2]) {
				case 'v': // mov
					return 1.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch mo-
			case 'u':
				switch (opToCmp[2]) {
				case 'l': // mul
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 1 for mul\n");
					}
					String[] operands = getOperands(opToCmp);
					// reg8 13-18
					// reg16 13-26
					// reg32 12-42
					// mem8 13-18
					// mem16 13-2
					// mem32 13-42
					if (isImmediateRegister(operands[0])) {
						char[] operand0 = operands[0].toCharArray();
						if (operand0[1] == 'e') {
							// reg32 12-42
							return 42.0;
						} else if (operand0[2] == 'l') {
							// reg8 13-26
							//END_CYCLE_COUNT("NOT 32 bit imul?\n");
							return 26.0;
						} else {
							// reg16 13-18
							//END_CYCLE_COUNT("NOT 32 bit imul?\n");
							return 42.0;//return 18.0;
						}
					} else {
						cycOverBase = 1.0;
						return 42.0; // assume mem32
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch mu-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch m-
		case 'n':
			switch (opToCmp[1]) {
			case 'e':
				switch (opToCmp[2]) {
				case 'g': // neg
					// reg 1
					// mem 3
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 1 for neg\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						return 1.0;
					} else {
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch ne-
			case 'o':
				switch (opToCmp[2]) {
				case 'p': // nop
					return 1.0;
				case 't': // not
					// reg 1
					// mem 3
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 1 for not\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0])) {
						return 1.0;
					} else {
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch no-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch n-
		case 'o':
			switch (opToCmp[1]) {
			case 'r': // or
				/*
				 * reg,reg 1 reg,mem 2 reg,immed 1 (reg)accum,immed 1 mem,reg 3
				 * mem8,immed8 3 mem16,immed16 3
				 */
				opParts = getNumOperandParts(opToCmp);
				if (opParts != 2) {
					END_CYCLE_COUNT(opParts
							+ " operand parts instead of 2 for or\n");
				}
				String[] operands = getOperands(opToCmp);
				if (isImmediateRegister(operands[1])) {
					// reg,reg 1
					// reg,mem 2
					// reg,immed 1
					// (reg)accum,immed 1
					if (isImmediateRegister(operands[0]))
						return 1.0;
					else if (isImmediateValue(operands[0]))
						return 1.0;
					else {
						cycOverBase = 1.0;
						return 2.0;
					}
				} else {
					cycOverBase = 2.0;
					return 3.0;
				}
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch o-
		case 'p':
			switch (opToCmp[1]) {
			case 'o':
				switch (opToCmp[2]) {
				case 'p':
					switch (opToCmp[3]) {
					case 'a': // popa
						return 9.0;
					case 'f': // popf
						return 9.0;
					default:
						opParts = getNumOperandParts(opToCmp);
						if (opParts != 1) {
							END_CYCLE_COUNT(opParts
									+ " operand parts instead of 1 for pop\n");
						}
						String[] operands = getOperands(opToCmp);
						if (isImmediateRegister(operands[0]))
							return 4.0;
						else
							return 6.0;

					}// end switch pop-
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch po-
			case 'u':
				switch (opToCmp[2]) {
				case 's':
					switch (opToCmp[3]) {
					case 'h':
						switch (opToCmp[4]) {
						case 'a': // pusha
							return 11.0;
						case 'f': // pushf
							return 4.0;
						default: // push
							opParts = getNumOperandParts(opToCmp);
							if (opParts != 1) {
								END_CYCLE_COUNT(opParts
										+ " operand parts instead of 1 for push\n");
							}
							String[] operands = getOperands(opToCmp);
							if (isImmediateRegister(operands[0]))
								return 1.0;
							else
								return 4.0;
						}// end switch push-
					default:
						END_CYCLE_COUNT(ERROR);
					}// end switch pus-
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch pu-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch p-
		case 'r':
			switch (opToCmp[1]) {
			case 'e':
				switch (opToCmp[2]) {
				case 'p': // rep
					// avg iters for rep instr?
					/*
					 * char operandOp[]; int i, j; for(i = 0; operand[i] == ' ';
					 * i++); for(j = 0; operand[i] != ' ' && operand[i] != '\0';
					 * i++, j++) operandOp[j] = operand[i]; operandOp[j] = '\0';
					 * return 4.0 * this->getOpCycles(operandOp);
					 */
					// TODO: Need to check, how to handle this?
				case 't': // ret
					return 5.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch re-
			case 'o': // ro-
				// 3 if reg
				// 4 if mem
				opParts = getNumOperandParts(opToCmp);
				if (opParts != 2) {
					END_CYCLE_COUNT(opParts
							+ " operand parts instead of 2 for ro[lr]\n");
				}
				String[] operands = getOperands(opToCmp);
				if (isImmediateRegister(operands[1])) {
					return 3.0;
				} else {
					cycOverBase = 1.0;
					return 4.0;
				}
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch r-
		case 's':
			switch (opToCmp[1]) {
			case 'a':
				switch (opToCmp[2]) {
				case 'l': // sal
				case 'r': // sar
					// look at dst
					// reg 3
					// mem 4
					opParts = getNumOperandParts(opToCmp);
					String[] operands = getOperands(opToCmp);
					if (opParts == 2) {

						if (isImmediateRegister(operands[1]))
							return 3.0;
						else {
							cycOverBase = 1.0;
							return 4.0;
						}
					} else if (opParts == 1) {

						if (isImmediateRegister(operands[0]))
							return 3.0;
						else {
							cycOverBase = 1.0;
							return 4.0;
						}
					} else {
						END_CYCLE_COUNT(ERROR);
					}
				case 'h': // sah
					return 2.0;
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch sa-
			case 'b':
				switch (opToCmp[2]) {
				case 'b': // sbb
					/*
					 * FLIPPED! we analyze AT&T, table is for Intel reg,reg 1
					 * mem,reg 3 reg,mem 2 reg,immed 1 mem,immed 3 accum,immed 1
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 2 for sbb\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						// reg,reg 1
						if (isImmediateRegister(operands[0]))
							return 1.0;
						// reg,immed 1
						// accum,immed 1
						else if (isImmediateValue(operands[0]))
							return 1.0;
						// reg,mem 2
						else {
							cycOverBase = 1.0;
							return 2.0;
						}
					} else {
						// mem,reg 3
						// mem,immed 3
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch sb-
			case 'c': // sc
				switch (opToCmp[2]) {
				case 'a': // sca
					switch (opToCmp[3]) {
					case 's':
						return 6.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'e':
				switch (opToCmp[2]) {
				case 't': // set
					// reg 3
					// mem 4
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 1) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 1 for set\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[0]))
						return 3.0;
					else {
						cycOverBase = 1.0;
						return 4.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch se-
			case 'h':
				switch (opToCmp[2]) {
				case 'l': // shlr
				case 'r': // shr
					switch (opToCmp[3]) {
					case 'd': // sh[lr]d
						// look at dst
						// reg 3
						// mem 4
						opParts = getNumOperandParts(opToCmp);
						if (opParts != 3) {
							END_CYCLE_COUNT(opParts
									+ " operand parts instead of 3 for sh[lr]d\n");
						}
						String[] operands = getOperands(opToCmp);
						if (isImmediateRegister(operands[2]))
							return 3.0;
						else {
							cycOverBase = 1.0;
							return 4.0;
						}
					default: // shl, shr, shrl
						// look at dst
						// reg 3
						// mem 4
						opParts = getNumOperandParts(opToCmp);
						String[] operands1 = getOperands(opToCmp);
						if (opParts != 2) {
							if (opParts == 1) {

								if (isImmediateRegister(operands1[0]))
									return 3.0;
								else {
									cycOverBase = 1.0;
									return 4.0;
								}
							}
							END_CYCLE_COUNT(opParts
									+ " operand parts instead of 2 for sh[lr]\n");
						}
						if (isImmediateRegister(operands1[1]))
							return 3.0;
						else {
							cycOverBase = 1.0;
							return 4.0;
						}
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch sh-
			case 't':
				switch (opToCmp[2]) {
				case 'o': // sto
					switch (opToCmp[3]) {
					case 's': // stos
						return 5.0;
					default:
						END_CYCLE_COUNT(ERROR);
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}
			case 'u':
				switch (opToCmp[2]) {
				case 'b': // sub
					/*
					 * FLIPPED! we analyze AT&T, table is for Intel reg,reg 1
					 * mem,reg 3 reg,mem 2 reg,immed 1 mem,immed 3 accum,immed 1
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 2 for sub\n");
					}
					String[] operands1 = getOperands(opToCmp);
					if (isImmediateRegister(operands1[1])) {
						// reg,reg 1
						if (isImmediateRegister(operands1[0]))
							return 1.0;
						// reg,immed 1
						// accum,immed 1
						else if (isImmediateValue(operands1[0]))
							return 1.0;
						// reg,mem 2
						else {
							cycOverBase = 1.0;
							return 2.0;
						}
					} else {
						// mem,reg 3
						// mem,immed 3
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch su-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch s-
		case 't':
			switch (opToCmp[1]) {
			case 'e':
				switch (opToCmp[2]) {
				case 's': // test
					/*
					 * reg,reg 1 reg,mem 1 reg,immed 1 accum,immed 1 mem,reg 2
					 * mem,immed 2
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 2 for add\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						// reg,reg 1
						// reg,mem 1
						// reg,immed 1
						// accum,immed 1
						return 1.0;
					} else {
						// mem,reg 2
						// mem,immed 2
						cycOverBase = 1.0;
						return 2.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch te-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch t-
		case 'x':
			switch (opToCmp[1]) {
			case 'c':
				switch (opToCmp[2]) {
				case 'h': // xch
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 2 for xch\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1]))
						return 3.0;
					else {
						cycOverBase = 2.0;
						return 5.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch xc-
			case 'o':
				switch (opToCmp[2]) {
				case 'r': // xor
					/*
					 * FLIPPED! we analyze AT&T, table is for Intel reg,reg 1
					 * mem,reg 3 reg,mem 2 reg,immed 1 mem,immed 3 accum,immed 1
					 */
					opParts = getNumOperandParts(opToCmp);
					if (opParts != 2) {
						END_CYCLE_COUNT(opParts
								+ " operand parts instead of 2 for xor\n");
					}
					String[] operands = getOperands(opToCmp);
					if (isImmediateRegister(operands[1])) {
						// reg,reg 1
						if (isImmediateRegister(operands[0]))
							return 1.0;
						// reg,immed 1
						// accum,immed 1
						else if (isImmediateValue(operands[0]))
							return 1.0;
						// reg,mem 2
						else {
							cycOverBase = 1.0;
							return 2.0;
						}
					} else {
						// mem,reg 3
						// mem,immed 3
						cycOverBase = 2.0;
						return 3.0;
					}
				default:
					END_CYCLE_COUNT(ERROR);
				}// end switch xo-
			default:
				END_CYCLE_COUNT(ERROR);
			}// end switch x-
		default:
			END_CYCLE_COUNT(ERROR);
		}// end switch -
		return -1;
	}

	/**
	 * Map Soot.baf bytecode to Java bytecode
	 * 
	 * @param unit
	 * @return
	 */
	private String bafToJBC(Unit unit) {
		if (unit instanceof BPushInst) {
			BPushInst inst = (BPushInst) unit;
			return "push";
		} else if (unit instanceof BAddInst) {
			BAddInst inst = (BAddInst) unit;
			Type inputType = inst.getOpType();
			// Check the type of the input and retrieve bycode accordingly
			if (inputType instanceof IntType) {
				return "iadd";
			} else if (inputType instanceof FloatType) {
				return "fadd";
			} else if (inputType instanceof DoubleType) {
				return "dadd";
			} else if (inputType instanceof LongType) {
				return "ladd";
			} else
				return null;

		} else if (unit instanceof BAndInst) {
			BAndInst inst = (BAndInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "iand";
			} else if (inputType instanceof LongType) {
				return "land";
			} else
				return null;

		} else if (unit instanceof BArrayLengthInst) {
			BArrayLengthInst inst = (BArrayLengthInst) unit;
			return "arraylength";
		} else if (unit instanceof BArrayReadInst) {
			BArrayReadInst inst = (BArrayReadInst) unit;
		} else if (unit instanceof BArrayWriteInst) {
			BArrayWriteInst inst = (BArrayWriteInst) unit;
		} else if (unit instanceof BCmpInst) {
			BCmpInst inst = (BCmpInst) unit;
		} else if (unit instanceof BCmpgInst) {
			BCmpgInst inst = (BCmpgInst) unit;
		} else if (unit instanceof BCmplInst) {
			BCmplInst inst = (BCmplInst) unit;
		} else if (unit instanceof BDivInst) {
			BDivInst inst = (BDivInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "idiv";
			} else if (inputType instanceof LongType) {
				return "ldiv";
			} else if (inputType instanceof DoubleType) {
				return "ddiv";
			} else if (inputType instanceof FloatType) {
				return "fdiv";
			} else
				return null;
		} else if (unit instanceof BDup1Inst) {
			BDup1Inst inst = (BDup1Inst) unit;
		} else if (unit instanceof BDup1_x1Inst) {
			BDup1_x1Inst inst = (BDup1_x1Inst) unit;
			return "dup_x1";
		} else if (unit instanceof BDup1_x2Inst) {
			BDup1_x2Inst inst = (BDup1_x2Inst) unit;
			return "dup_x2";
		} else if (unit instanceof BDup2Inst) {
			BDup2Inst inst = (BDup2Inst) unit;
			return "dup2";
		} else if (unit instanceof BDup2_x1Inst) {
			BDup2_x1Inst inst = (BDup2_x1Inst) unit;
			return "dup2_x1";
		} else if (unit instanceof BDup2_x2Inst) {
			BDup2_x2Inst inst = (BDup2_x2Inst) unit;
			return "dup2_x2";
		} else if (unit instanceof BDupInst) {
			BDupInst inst = (BDupInst) unit;
			return "dup";
		} else if (unit instanceof BEnterMonitorInst) {
			BEnterMonitorInst inst = (BEnterMonitorInst) unit;
			return "monitorenter";
		} else if (unit instanceof BExitMonitorInst) {
			BExitMonitorInst inst = (BExitMonitorInst) unit;
			return "monitorexit";
		} else if (unit instanceof BFieldGetInst) {
			BFieldGetInst inst = (BFieldGetInst) unit;
			SootFieldRef fieldRef = inst.getFieldRef();
			// Type fieldRefType = fieldRef.type();
			boolean isStatic = fieldRef.isStatic();
			if (isStatic)
				return "getstatic";
			return "getfield";
		} else if (unit instanceof BFieldPutInst) {
			BFieldPutInst inst = (BFieldPutInst) unit;
			return "putfield";
		} else if (unit instanceof BGotoInst) {
			BGotoInst inst = (BGotoInst) unit;
			return "goto";
		} else if (unit instanceof BIdentityInst) {
			BIdentityInst inst = (BIdentityInst) unit;
			return null;
		} else if (unit instanceof BIfCmpEqInst) {
			BIfCmpEqInst inst = (BIfCmpEqInst) unit;
		} else if (unit instanceof BIfCmpGeInst) {
			BIfCmpGeInst inst = (BIfCmpGeInst) unit;
		} else if (unit instanceof BIfCmpGtInst) {
			BIfCmpGtInst inst = (BIfCmpGtInst) unit;
		} else if (unit instanceof BIfCmpLeInst) {
			BIfCmpLeInst inst = (BIfCmpLeInst) unit;
		} else if (unit instanceof BIfCmpLtInst) {
			BIfCmpLtInst inst = (BIfCmpLtInst) unit;
		} else if (unit instanceof BIfCmpNeInst) {
			BIfCmpNeInst inst = (BIfCmpNeInst) unit;
		} else if (unit instanceof BIfEqInst) {
			BIfEqInst inst = (BIfEqInst) unit;
			return "ifeq";
		} else if (unit instanceof BIfGeInst) {
			BIfGeInst inst = (BIfGeInst) unit;
			return "ifge";
		} else if (unit instanceof BIfGtInst) {
			BIfGtInst inst = (BIfGtInst) unit;
			return "ifgt";
		} else if (unit instanceof BIfLeInst) {
			BIfLeInst inst = (BIfLeInst) unit;
			return "ifle";
		} else if (unit instanceof BIfLtInst) {
			BIfLtInst inst = (BIfLtInst) unit;
			return "iflt";
		} else if (unit instanceof BIfNeInst) {
			BIfNeInst inst = (BIfNeInst) unit;
			return "ifne";
		} else if (unit instanceof BIfNonNullInst) {
			BIfNonNullInst inst = (BIfNonNullInst) unit;
			return "ifnonnull";
		} else if (unit instanceof BIfNullInst) {
			BIfNullInst inst = (BIfNullInst) unit;
			return "ifnull";
		} else if (unit instanceof BIncInst) {
			BIncInst inst = (BIncInst) unit;
			return "iinc";
		} else if (unit instanceof BInstanceCastInst) {
			BInstanceCastInst inst = (BInstanceCastInst) unit;
			return "checkcast";
		} else if (unit instanceof BInstanceOfInst) {
			BInstanceOfInst inst = (BInstanceOfInst) unit;
			Type inst_type = inst.getCheckType();
			//if (inst_type instanceof FloatType)
			return "instanceof";
		} else if (unit instanceof BInterfaceInvokeInst) {
			BInterfaceInvokeInst inst = (BInterfaceInvokeInst) unit;
			return "invokeinterface";
		} else if (unit instanceof BJSRInst) {
			BJSRInst inst = (BJSRInst) unit;
			return "jsr";
		} else if (unit instanceof BLoadInst) {
			BLoadInst inst = (BLoadInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "iload";
			} else if (inputType instanceof FloatType) {
				return "fload";
			} else if (inputType instanceof DoubleType) {
				return "dload";
			} else if (inputType instanceof LongType) {
				return "lload";
			} else if (inputType instanceof RefType) {
				return "aload";
			} else if (inputType instanceof ArrayType) {
				ArrayType arrayInputType = (ArrayType) inputType;
				Type arrayElementType = arrayInputType.getElementType();
				if (arrayElementType instanceof IntType) {
					return "iaload";
				} else if (arrayElementType instanceof FloatType) {
					return "faload";
				} else if (arrayElementType instanceof DoubleType) {
					return "daload";
				} else if (arrayElementType instanceof LongType) {
					return "laload";
				} else if (arrayElementType instanceof RefType) {
					return "aaload";
				} else if (arrayElementType instanceof CharType) {
					return "caload";
				} else if (arrayElementType instanceof ShortType) {
					return "saload";
				} else if (arrayElementType instanceof ByteType) {
					return "baload";
				} else if (arrayElementType instanceof BooleanType) {
					return "baload";
				} else
					return null;
				// return "aload";
			} else
				return null;
		} else if (unit instanceof BLookupSwitchInst) {
			BLookupSwitchInst inst = (BLookupSwitchInst) unit;
			return "lookupswitch";
		} else if (unit instanceof BMulInst) {
			BMulInst inst = (BMulInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "imul";
			} else if (inputType instanceof FloatType) {
				return "fmul";
			} else if (inputType instanceof DoubleType) {
				return "dmul";
			} else if (inputType instanceof LongType) {
				return "lmul";
			} else
				return null;
		} else if (unit instanceof BNegInst) {
			BNegInst inst = (BNegInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "ineg";
			} else if (inputType instanceof FloatType) {
				return "fneg";
			} else if (inputType instanceof DoubleType) {
				return "dneg";
			} else if (inputType instanceof LongType) {
				return "lneg";
			} else
				return null;
		} else if (unit instanceof BNewArrayInst) {
			BNewArrayInst inst = (BNewArrayInst) unit;
			return "newarray";
		} else if (unit instanceof BNewInst) {
			BNewInst inst = (BNewInst) unit;
			return "new";
		} else if (unit instanceof BNewMultiArrayInst) {
			BNewMultiArrayInst inst = (BNewMultiArrayInst) unit;
			return "multianewarray";
		} else if (unit instanceof BNopInst) {
			BNopInst inst = (BNopInst) unit;
			return "nop";
		} else if (unit instanceof BOrInst) {
			BOrInst inst = (BOrInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "ior";
			} else if (inputType instanceof LongType) {
				return "lor";
			} else
				return null;
		} else if (unit instanceof BPopInst) {
			BPopInst inst = (BPopInst) unit;
			return "pop";
		} else if (unit instanceof BPrimitiveCastInst) {
			BPrimitiveCastInst inst = (BPrimitiveCastInst) unit;
		} else if (unit instanceof BRemInst) {
			BRemInst inst = (BRemInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "irem";
			} else if (inputType instanceof FloatType) {
				return "frem";
			} else if (inputType instanceof DoubleType) {
				return "drem";
			} else if (inputType instanceof LongType) {
				return "lrem";
			} else
				return null;
		} else if (unit instanceof BReturnInst) {
			BReturnInst inst = (BReturnInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "ireturn";
			} else if (inputType instanceof FloatType) {
				return "freturn";
			} else if (inputType instanceof DoubleType) {
				return "dreturn";
			} else if (inputType instanceof LongType) {
				return "lreturn";
			} else if (inputType instanceof ArrayType) {
				return "areturn";
			} else
				return "return";
		} else if (unit instanceof BReturnVoidInst) {
			BReturnVoidInst inst = (BReturnVoidInst) unit;
			return "return";
		} else if (unit instanceof BShlInst) {
			BShlInst inst = (BShlInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "ishl";
			} else if (inputType instanceof LongType) {
				return "lshl";
			} else
				return null;
		} else if (unit instanceof BShrInst) {
			BShrInst inst = (BShrInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "ishr";
			} else if (inputType instanceof LongType) {
				return "lshr";
			} else
				return null;
		} else if (unit instanceof BSpecialInvokeInst) {
			BSpecialInvokeInst inst = (BSpecialInvokeInst) unit;
			return "invokespecial";
		} else if (unit instanceof BStaticGetInst) {
			BStaticGetInst inst = (BStaticGetInst) unit;
			return "getstatic";
		} else if (unit instanceof BStaticInvokeInst) {
			BStaticInvokeInst inst = (BStaticInvokeInst) unit;
			return "invokestatic";
		} else if (unit instanceof BStaticPutInst) {
			BStaticPutInst inst = (BStaticPutInst) unit;
			return "putstatic";
		} else if (unit instanceof BStoreInst) {
			BStoreInst inst = (BStoreInst) unit;
			Type inputType = inst.getOpType();
			if (inputType instanceof IntType) {
				return "istore";
			} else if (inputType instanceof FloatType) {
				return "fstore";
			} else if (inputType instanceof DoubleType) {
				return "dstore";
			} else if (inputType instanceof LongType) {
				return "lstore";
			} else if (inputType instanceof RefType) {
				return "astore";
			} else if (inputType instanceof ArrayType) {
				ArrayType arrayInputType = (ArrayType) inputType;
				Type arrayElementType = arrayInputType.getElementType();
				if (arrayElementType instanceof IntType) {
					return "iastore";
				} else if (arrayElementType instanceof FloatType) {
					return "fastore";
				} else if (arrayElementType instanceof DoubleType) {
					return "dastore";
				} else if (arrayElementType instanceof LongType) {
					return "lastore";
				} else if (arrayElementType instanceof RefType) {
					return "aastore";
				} else if (arrayElementType instanceof CharType) {
					return "castore";
				} else if (arrayElementType instanceof ShortType) {
					return "sastore";
				} else if (arrayElementType instanceof ByteType) {
					return "bastore";
				} else if (arrayElementType instanceof BooleanType) {
					return "bastore";
				} else
					return null;
				// return "aload";
			} else
				return null;
		} else if (unit instanceof BSubInst) {
			BSubInst inst = (BSubInst) unit;
			Type inputType = inst.getOpType();
			// Check the type of the input and retrieve bycode accordingly
			if (inputType instanceof IntType) {
				return "isub";
			} else if (inputType instanceof FloatType) {
				return "fsub";
			} else if (inputType instanceof DoubleType) {
				return "dsub";
			} else if (inputType instanceof LongType) {
				return "lsub";
			} else
				return null;
		} else if (unit instanceof BSwapInst) {
			BSwapInst inst = (BSwapInst) unit;
			return "swap";
		} else if (unit instanceof BTableSwitchInst) {
			BTableSwitchInst inst = (BTableSwitchInst) unit;
			return "tableswitch";
		} else if (unit instanceof BThrowInst) {
			BThrowInst inst = (BThrowInst) unit;
			return "athrow";
		} else if (unit instanceof BTrap) {
			BTrap inst = (BTrap) unit;
		} else if (unit instanceof BUshrInst) {
			BUshrInst inst = (BUshrInst) unit;
			Type inputType = inst.getOpType();
			// Check the type of the input and retrieve bycode accordingly
			if (inputType instanceof IntType) {
				return "iushr";
			} else if (inputType instanceof LongType) {
				return "lushr";
			} else
				return null;
		} else if (unit instanceof BVirtualInvokeInst) {
			BVirtualInvokeInst inst = (BVirtualInvokeInst) unit;
			return "invokevirtual";
		} else if (unit instanceof BXorInst) {
			BXorInst inst = (BXorInst) unit;
			Type inputType = inst.getOpType();
			// Check the type of the input and retrieve bycode accordingly
			if (inputType instanceof IntType) {
				return "ixor";
			} else if (inputType instanceof LongType) {
				return "lxor";
			} else
				return null;
		} else {

		}
		return null;
	}

	/**
	 * 
	 * @param unit
	 * @return
	 */
	private double getCycles(Unit unit) {
		double cycleCount = 0.0;
		String bytecode = bafToJBC(unit);
		if (bytecode == null)
			return 0.0;
		List<String> machineCodes = JBCtoMachineCode(unit, bytecode);
		for (Iterator<String> mcIter = machineCodes.iterator(); mcIter
				.hasNext();) {
			String machineCode = mcIter.next();
			// TODO: I have to consider pipeline
			cycleCount += getOpCycles(machineCode.toCharArray());
		}
		return cycleCount;
	}
	
	
	public void byteCodeToCycles () {
		for (String jbc : _converter.jbc) {
			double cycleCount = 0.0;
			List<String> machineCodes = JBCtoMachineCode(null, jbc);
			for (Iterator<String> mcIter = machineCodes.iterator(); mcIter
					.hasNext();) {
				String machineCode = mcIter.next();
				// TODO: I have to consider pipeline
				cycleCount += getOpCycles(machineCode.toCharArray());
			}
			System.out.println(jbc+ " = "+cycleCount);
		}
	}

	public double process_unit(UnitInfo unitInfo) {
		Unit unit = unitInfo.unit();
		double cycles = 0.0;
		// Cycle count for the base instruction
		cycles += getCycles(unit);
		// Check if the unit has inner unit of instruction?
		Iterator<UnitBox> unitBoxIter = unit.getUnitBoxes().iterator();
		while (unitBoxIter.hasNext()) {
			UnitBox box = unitBoxIter.next();
			Unit innerUnit = box.getUnit();
			if (innerUnit instanceof Inst) {
				cycles += getCycles(innerUnit);
			}
		}
		return cycles;
	}

	private List<String> JBCtoMachineCode(Unit unit, String bytecode) {
		//Inst inst;
		int hashCode = _converter.getHashCode(bytecode);
		switch (hashCode) {
		case 3452698: // push
			return emit_push(0);
		case 3078573: // ddiv
			return emit_ddiv();
		case -1191314396: // ifnull
			return emit_ifnull(0);
		case 3241908:// ishr
			return emit_ishr();
		case 162797323:// pending_goto
			return emit_pending_goto(0);
		case -332153854:// bastore
			return emit_bastore();
		case -1425030906:// aaload
			return emit_aaload();
		case 111185:// pop
			return emit_pop();
		case 3229386:// ifgt
			return emit_ifgt(0);
		case 3151476:// frem
			return emit_frem();
		case 426720412:// lreturn
			return emit_lreturn();
		case -1408168672:// astore
			return emit_astore(0);
		case -1194119562:// icmple
			return emit_if_icmple(0);
		case 3241902:// ishl
			return emit_ishl();
		case -1045876866:// checkcast_final
			return emit_checkcast_final(null);
		case 99677:// f2i
			return emit_f2i();
		case -745885487:// areturn
			return emit_areturn();
		case 97526348:// fload
			return emit_fload(0);
		case 3095059:// dup2
			return emit_dup2();
		case -1281885151:// faload
			return emit_faload();
		case 99672:// f2d
			return emit_f2d();
		case -1194061670:// iconst
			return emit_iconst(0);
		case 3330222:// lrem
			return emit_lrem();
		case 3336303:// lxor
			return emit_lxor();
		case 1580087672:// monitorexit
			return emit_monitorexit();
		case 3236999:// ineg
			return emit_ineg();
		case 99680:// f2l
			return emit_f2l();
		case 3326372:// lneg
			return emit_lneg();
		case 107343:// lor
			return emit_lor();
		case -603334378:// freturn
			return emit_freturn();
		case 1585404617:// iastore
			return emit_iastore();
		case 3147626:// fneg
			return emit_fneg();
		case 3227528:// idiv
			return emit_idiv();
		case 100296911:// iload
			return emit_iload(0);
		case 3331281:// ls
			return emit_lshr();
		case 3316647:// ldc2
			return emit_ldc2();
		case 3331275:// lshl
			return emit_lshl();
		case 95409733:// dcmpg
			return emit_dcmpg();
		case 2010684517:// dup2_x1
			return emit_dup2_x1();
		case 2010684518:// dup2_x2
			return emit_dup2_x2();
		case 1442853508:// dastore
			return emit_dastore();
		case 103067474:// lload
			return emit_lload(0);
		case 95409738:// dcmpl
			return emit_dcmpl();
		case 1426572927:// arraylength
			return emit_arraylength();
		case 112801:// ret
			return emit_ret(0);
		case 3229588:// ifne
			return emit_ifne(0);
		case -1195997698:// iaload
			return emit_iaload();
		case 3325912:// lmul
			return emit_lmul();
		case 1719665988:// getstatic
			BStaticGetInst inst = (BStaticGetInst) unit;
			SootFieldRef sootfieldRef = inst.getFieldRef();
			Type fieldRefType = sootfieldRef.type();
			TypeReference type = new TypeReference(fieldRefType);
			FieldReference fieldRef = new FieldReference(type);
			return emit_unresolved_getstatic(fieldRef);
		case 555349827:// castore
			return emit_castore();
		case 3316901:// ldiv
			return emit_ldiv();
		case -224063538:// instanceof_resolvedInterface
			return emit_instanceof_resolvedInterface(null);
		case 3178851:// goto
			return emit_goto(0);
		case 3229321:// ifeq
			return emit_ifeq(0);
		case -1320568582:// dup_x2
			return emit_dup_x2();
		case 105438:// l2d
			return emit_l2d();
		case -1320568583:// dup_x1
			return emit_dup_x1();
		case 452639293:// putstatic
			BStaticPutInst putinst = (BStaticPutInst) unit;
			sootfieldRef = putinst.getFieldRef();
			fieldRefType = sootfieldRef.type();
			type = new TypeReference(fieldRefType);
			fieldRef = new FieldReference(type);
			return emit_unresolved_putstatic(fieldRef);
		case -1179135464:// istore
			return emit_istore(0);
		case 103339668:// lushr
			return emit_lushr();
		case 1803386699:// putfield
			BFieldPutInst fieldPutInst = (BFieldPutInst) unit;
			SootFieldRef sootFref = fieldPutInst.getFieldRef();
			Type unitType = sootFref.type();
			type = new TypeReference(unitType);
			FieldReference Fref = new FieldReference(type);
			return emit_unresolved_putfield(Fref);
		case -1265022917:// fstore
			return emit_fstore(0);
		case 3138155:// fdiv
			return emit_fdiv();
		case 577226785:// resolvedInterface
			return null;
		case -1302156777:// ifnonnull
			return emit_ifnonnull(0);
		case -1219657535:// aastore
			return emit_aastore();
		case 3135099:// fadd
			return emit_fadd();
		case -934396624:// return
			return emit_return();
		case -1423152975:// acmpeq
			return emit_if_acmpeq(0);
		case 399023175:// checkcast
			BInstanceCastInst castInst = (BInstanceCastInst) unit;
			TypeReference castType = new TypeReference(castInst.getCastType());
			return emit_checkcast(castType);
		case 450492249:// invoke_compiledmethod
			return emit_invoke_compiledmethod(null);
		case -1396401755:// baload
			return emit_baload();
		case 3240849:// irem
			return emit_irem();
		case 1870506835:// sastore
			return emit_sastore();
		case -1077106426:// fastore
			return emit_fastore();
		case -1110110245:// laload
			return emit_laload();
		case 105440:// l2f
			return emit_l2f();
		case 108960:// new
			BNewInst newInst = (BNewInst) unit;
			TypeReference newTypeRef = new TypeReference(newInst.getOpType());
			return emit_unresolved_new(newTypeRef);
		case 105443:// l2i
			return emit_l2i();
		case 3242295:// isub
			return emit_isub();
		case -1423152708:// acmpne
			return emit_if_acmpeq(0);
		case 3236539:// imul
			return emit_imul();
		case 1581852676:// aconst_null
			return emit_aconst_null();
		case 1298162851:// instanceof_final
			return emit_instanceof_final(null);
		case 3075517:// dadd
			return emit_dadd();
		case 3232469:// iinc
			return emit_iinc(0, 0);
		case 3229371:// ifge
			return emit_ifge(0);
		case 1982805860:// getfield
			BFieldGetInst fieldGetInst = (BFieldGetInst) unit;
			sootFref = fieldGetInst.getFieldRef();
			unitType = sootFref.type();
			type = new TypeReference(unitType);
			Fref = new FieldReference(type);
			return emit_unresolved_getfield(Fref);
		case 739214657:// invokespecial
			BSpecialInvokeInst specialInvokeInst = (BSpecialInvokeInst) unit;
			SootMethodRef sootMethodRef = specialInvokeInst.getMethodRef();
			List<Type> paramTypes = sootMethodRef.parameterTypes();
			List<TypeReference> typeRefs = new ArrayList<TypeReference>();
			for (Type paramtype : paramTypes) {
				TypeReference tref = new TypeReference(paramtype);
				typeRefs.add(tref);
			}
			TypeReference rType = new TypeReference(sootMethodRef.returnType());
			MethodReference mRef = new MethodReference(typeRefs, rType);
			return emit_unresolved_invokespecial(mRef);
		case -259755401:// loadretaddrconst
			return emit_loadretaddrconst(0);
		case 3313845:// ladd
			return emit_ladd();
		case -1194119717:// icmpge
			return emit_if_icmpge(0);
		case 3331668:// lsub
			return emit_lsub();
		case 1916625556:// dreturn
			return emit_dreturn();
		case 3229526:// ifle
			return emit_ifle(0);
		case 102563:// i2l
			return emit_i2l();
		case -1775970559:// multianewarray
			return emit_multianewarray(null, 0);
		case -1407599835:// athrow
			return emit_athrow();
		case 102570:// i2s
			return emit_i2s();
		case 2059176665:// ireturn
			return emit_ireturn();
		case 3088044:// dneg
			return emit_dneg();
		case 104460:// ior
			return emit_ior();
		case 102553:// i2b
			return emit_i2b();
		case -1670457552:// fconst_2
			return emit_fconst_2();
		case 102554:// i2c
			return emit_i2c();
		case 102555:// i2d
			return emit_i2d();
		case 1182228610:// tableswitch
			return emit_tableswitch(0, 0, 0);
		case 105545:// jsr
			return emit_jsr(0);
		case 92908743:// aload
			return emit_aload(0);
		case 106987:// ldc
			return emit_ldc();
		case 102557:// i2f
			return emit_i2f();
		case 99839:// dup
			return emit_dup();
		case -1339143453:// daload
			return emit_daload();
		case -1670457553:// fconst_1
			return emit_fconst_1();
		case 902025516:// instanceof
			return emit_instanceof(null);
		case -1081121901:// invokevirtual
			BVirtualInvokeInst virtualInvokeInst = (BVirtualInvokeInst) unit;
			sootMethodRef = virtualInvokeInst.getMethodRef();
			paramTypes = sootMethodRef.parameterTypes();
			typeRefs = new ArrayList<TypeReference>();
			for (Type paramtype : paramTypes) {
				TypeReference tref = new TypeReference(paramtype);
				typeRefs.add(tref);
			}
			rType = new TypeReference(sootMethodRef.returnType());
			mRef = new MethodReference(typeRefs, rType);
			return emit_unresolved_invokevirtual(mRef);
		case -1670457554:// fconst_0
			return emit_fconst_0();
		case -1194119702:// icmpgt
			return null;
		case 1737789886:// monitorenter
			return emit_monitorenter();
		case -861110928:// dconst_0
			return emit_dconst_0();
		case 3229541:// iflt
			return emit_iflt(0);
		case -861110927:// dconst_1
			return emit_dconst_1();
		case 100569105:// iushr
			return emit_iushr();
		case 3314155:// land
			return emit_land();
		case 3446785:// pop2
			return emit_pop2();
		case 3224782:// iand
			return emit_iand();
		case 443078886:// invokestatic
			BStaticInvokeInst staticInvokeInst = (BStaticInvokeInst) unit;
			sootMethodRef = staticInvokeInst.getMethodRef();
			paramTypes = sootMethodRef.parameterTypes();
			typeRefs = new ArrayList<TypeReference>();
			for (Type paramtype : paramTypes) {
				TypeReference tref = new TypeReference(paramtype);
				typeRefs.add(tref);
			}
			rType = new TypeReference(sootMethodRef.returnType());
			mRef = new MethodReference(typeRefs, rType);
			return emit_unresolved_invokestatic(mRef);
		case 3087584:// dmul
			return emit_dmul();
		case -1093248011:// lstore
			return emit_lstore(0);
		case -909706188:// saload
			return emit_saload();
		case -381898815:// invokeinterface
			BInterfaceInvokeInst interfaceInvokeInst = (BInterfaceInvokeInst) unit;
			sootMethodRef = interfaceInvokeInst.getMethodRef();
			paramTypes = sootMethodRef.parameterTypes();
			typeRefs = new ArrayList<TypeReference>();
			for (Type paramtype : paramTypes) {
				TypeReference tref = new TypeReference(paramtype);
				typeRefs.add(tref);
			}
			rType = new TypeReference(sootMethodRef.returnType());
			mRef = new MethodReference(typeRefs, rType);
			return emit_invokeinterface(mRef);
		case 3093340:// dsub
			return emit_dsub();
		case 3152922:// fsub
			return emit_fsub();
		case -47051636:// lastore
			return emit_lastore();
		case 1379126457:// newarray
			return emit_unresolved_newarray(null);
		case -1194119500:// icmpne
			return null;
		case 3246930:// ixor
			return emit_ixor();
		case 3224472:// iadd
			return emit_iadd();
		case 3316058:// lcmp
			return emit_lcmp();
		case 97752:// d2f
			return emit_d2f();
		case -1194119547:// icmplt
			return null;
		case 97755:// d2i
			return emit_d2i();
		case 3091894:// drem
			return emit_drem();
		case 168409184:// resolvedClass
			return null;
		case 97758:// d2l
			return emit_d2l();
		case -1194119767:// icmpeq
			return null;
		case -1108174217:// lconst
			return emit_lconst(0);
		case 97256775:// fcmpg
			return emit_fcmpg();
		case -1322281219:// dstore
			return emit_dstore(0);
		case -1367772604:// caload
			return emit_caload();
		case 3147166:// fmul
			return emit_fmul();
		case 3543443:// swap
			return emit_swap();
		case 95679306:// dload
			return emit_dload(0);
		case -749740403:// instanceof_resolvedClass
			return emit_instanceof_resolvedClass(null);
		case 97256780:// fcmpl
			return emit_fcmpl();
		case 472702254:// lookupswitch
			return emit_lookupswitch(0, 0);
		default:
			return null;
		}
	}

	/**
	 * Adjust the value of ESP/RSP
	 * 
	 * @param size
	 *            amount to change ESP/RSP by
	 * @param mayClobber
	 *            can the value in S0 or memory be destroyed? (ie can we use a
	 *            destructive short push/pop opcode)
	 */
	private List<String> adjustStack(int size, boolean mayClobber) {
		List<String> extraMachineCodes = new ArrayList<String>();
		final boolean debug = false;
		if (size != 0) {
			if (mayClobber) {
				// first try short opcodes
				/*
				 * asm.emitPUSH_Imm(imm); Generate an immediate PUSH. That is,
				 * push imm, SP += 4
				 */
				switch (size >> LG_WORDSIZE) {
				case -2:
					if (debug) {
						extraMachineCodes.add("push $0xFA1FACE");
						extraMachineCodes.add("push $0xFA2FACE");
					} else {
						extraMachineCodes.add("push %EAX");
						extraMachineCodes.add("push %EAX");
					}
					return extraMachineCodes;
				case -1:
					if (debug) {
						extraMachineCodes.add("push $0xFA3FACE");
					} else {
						extraMachineCodes.add("push %EAX");
					}
					return extraMachineCodes;
				case 1:
					/*
					 * Generate a register POP. That is, pop dstReg, SP -= 4
					 */
					extraMachineCodes.add("pop %S0");
					if (debug) {
						extraMachineCodes.add("mov %S0, $0xFA4FACE");
					}
					return extraMachineCodes;
				case 2:
					extraMachineCodes.add("pop %S0");
					extraMachineCodes.add("pop %S0");
					if (debug) {
						extraMachineCodes.add("mov %S0, $0xFA5FACE");
					}
					return extraMachineCodes;
				}
			}
			/*
			 * Generate a register--immediate ADD. That is, dstReg += imm
			 * --------------------------------------------- Generate a
			 * register--immediate ADD. That is, dstReg += (quad) imm
			 */
			// TODO: handle quad
			String addInst = AddrJVM_32_or_64.equals("32") ? "add %SP " + size
					: "add %SP " + size;
			extraMachineCodes.add(addInst);
			return extraMachineCodes;
		}
		return extraMachineCodes;
	}

	/**
	 * Emit a conditional branch on the given condition and bytecode target. The
	 * caller has just emitted the instruction sequence to set the condition
	 * codes.
	 */
	private void genCondBranch(byte cond, int bTarget) {

	}

	private List<String> emit_push(int val) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push " + val);
		return machineCodes;
	}

	private List<String> emit_aconst_null() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push $0");
		return machineCodes;
	}

	private List<String> emit_iconst(int val) {
		/*
		 * asm.emitPUSH_Imm(val); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push " + val);
		return machineCodes;
	}

	private List<String> emit_lconst(int val) {
		/*
		 * asm.emitPUSH_Imm(0); // high part asm.emitPUSH_Imm(val); // low part
		 * Generate an immediate PUSH. That is, push imm, SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push $0");
		machineCodes.add("push " + val);
		return machineCodes;
	}

	private List<String> emit_fconst_0() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push $0");
		return machineCodes;
	}

	private List<String> emit_fconst_1() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push $0x3f800000");
		return machineCodes;
	}

	private List<String> emit_fconst_2() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push $0x40000000");
		return machineCodes;
	}

	private List<String> emit_dconst_0() {
		/*
		 * Generate an immediate PUSH. That is, push imm, SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("push $0x00000000");
			machineCodes.add("push $0x00000000");
		} else {
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			String machineCode;
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("push $0x00000000");
		}
		return machineCodes;
	}

	private List<String> emit_dconst_1() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("push $0x3ff00000");
			machineCodes.add("push $0x00000000");
		} else {
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			String machineCode;
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			// Considering 0 for absolute value
			machineCodes.add("push $0");
		}
		return machineCodes;
	}

	private List<String> emit_ldc() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			// Considering 0 for absolute value
			machineCodes.add("push $0");
		} else {
			machineCodes.add("mov %T0, $0");
			machineCodes.add("push %T0");
		}
		return machineCodes;
	}

	private List<String> emit_ldc2() {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				/*
				 * adjustStack(-2 * WORDSIZE, true); // adjust stack
				 * asm.emitMOVQ_Reg_Abs(XMM0,
				 * Magic.getTocPointer().plus(offset)); // XMM0 // is //
				 * constant // value asm.emitMOVQ_RegInd_Reg(SP, XMM0); // place
				 * value on stack
				 */
				List<String> extraMachineCodes = adjustStack(-2 * WORDSIZE,
						true);
				Iterator<String> mcIter = extraMachineCodes.iterator();
				String machineCode;
				while (mcIter.hasNext())
					machineCodes.add(mcIter.next());
				machineCodes.add("mov %XMM0, $0");
				machineCodes.add("mov %SP, %XMM0");
			} else {
				/*
				 * asm.emitPUSH_Abs(Magic.getTocPointer().plus(offset).plus(
				 * WORDSIZE)); // high 32 bits
				 * asm.emitPUSH_Abs(Magic.getTocPointer().plus(offset)); // low
				 * 32 // bits
				 */
				machineCodes.add("push $0");
				machineCodes.add("push $0");
			}
		} else {
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			String machineCode;
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("push $0");
		}
		return machineCodes;
	}

	private List<String> emit_iload(int index) {
		/*
		 * Offset offset = localOffset(index); if (offset.EQ(Offset.zero())) {
		 * asm.emitPUSH_RegInd(ESP); } else { asm.emitPUSH_RegDisp(ESP, offset);
		 * }
		 */
		/*
		 * Generate a register-indirect PUSH. That is, push [base], SP += 4
		 * 
		 * Generate a register-displacement PUSH. That is, push [base + disp],
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push %ESP");
		return machineCodes;
	}

	private List<String> emit_fload(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		return emit_iload(index);
	}

	private List<String> emit_aload(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		return emit_iload(index);
	}

	private List<String> emit_lload(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				/*
				 * asm.emitMOVQ_Reg_RegDisp(XMM0, SP, offset.minus(WORDSIZE));
				 * // XMM0 is local value adjustStack(-2*WORDSIZE, true); //
				 * adjust stack asm.emitMOVQ_RegInd_Reg(SP, XMM0); // place
				 * value on stack
				 */
				machineCodes.add("mov %XMM0 %SP");
				List<String> extraMachineCodes = adjustStack(-2 * WORDSIZE,
						true);
				Iterator<String> mcIter = extraMachineCodes.iterator();
				String machineCode;
				while (mcIter.hasNext())
					machineCodes.add(mcIter.next());
				machineCodes.add("mov %SP %XMM0");
			} else {
				/* asm.emitPUSH_RegDisp(ESP, offset); // high part
		        asm.emitPUSH_RegDisp(ESP, offset); // low part (ESP has moved by 4!!) */
				machineCodes.add("push %ESP");
				machineCodes.add("push %ESP");
			}
		} else {
			adjustStack(-WORDSIZE, true);
			machineCodes.add("push %ESP");
		}

		return machineCodes;
	}

	private List<String> emit_dload(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		return emit_lload(index);
	}

	private List<String> emit_istore(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %ESP");
		return machineCodes;
	}

	private List<String> emit_fstore(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		return emit_istore(index);
	}

	private List<String> emit_astore(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		return emit_istore(index);
	}

	private List<String> emit_lstore(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				/*
				 * Offset offset = localOffset(index).minus(WORDSIZE);
				 * asm.emitMOVQ_Reg_RegInd(XMM0, SP); // XMM0 is stack value
				 * asm.emitMOVQ_RegDisp_Reg(SP, offset, XMM0); // place value in
				 * // local adjustStack(2 * WORDSIZE, true);
				 */
				machineCodes.add("mov %XMM0 %SP");
				machineCodes.add("mov %SP $0 %XMM0");
				List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);
				Iterator<String> mcIter = extraMachineCodes.iterator();
				String machineCode;
				while (mcIter.hasNext())
					machineCodes.add(mcIter.next());

			} else {
				// pop computes EA after ESP has moved by 4!
				/*
				 * Offset offset = localOffset(index + 1).minus(WORDSIZE);
				 * asm.emitPOP_RegDisp(ESP, offset); // high part
				 * asm.emitPOP_RegDisp(ESP, offset); // low part (ESP has moved
				 * by 4!!)
				 */
				machineCodes.add("pop %ESP");
				machineCodes.add("pop %ESP");
			}
		} else {
			/*
			 * Offset offset = localOffset(index + 1).minus(WORDSIZE);
			 * asm.emitPOP_RegDisp(ESP, offset); adjustStack(WORDSIZE, true); //
			 * throw away top word
			 */
			machineCodes.add("pop %ESP");
		}

		return machineCodes;
	}

	private List<String> emit_dstore(int index) {
		/*
		 * asm.emitPUSH_Imm(0); Generate an immediate PUSH. That is, push imm,
		 * SP += 4
		 */
		return emit_lstore(index);
	}

	/*
	 * Array loads
	 */

	/**
	 * Generate an array bounds check trapping if the array bound check fails,
	 * otherwise falling through.
	 */
	/* private List<String> genBoundCheck() {
		List<String> extraMachineCodes = new ArrayList<String>();
		return extraMachineCodes;
	}*/
	
	private List<String> genBoundsCheck() {
		List<String> machineCodes = new ArrayList<String>();
		if (ARRAY_LENGTH_BYTES == 4) {
			// asm.emitCMP_RegDisp_Reg(arrayRefReg,
			// ObjectModel.getArrayLengthOffset(), indexReg);
			machineCodes.add("cmp %T0 %T1");
		} else {
			// asm.emitMOV_Reg_Reg(indexReg, indexReg); // clear MSBs
			// asm.emitCMP_RegDisp_Reg_Quad(arrayRefReg,
			// ObjectModel.getArrayLengthOffset(), indexReg);
			machineCodes.add("cmp %T0 %T0");
			machineCodes.add("cmp %T0 %T1");
		}
		// Jmp around trap if index is OK
		/*
		 * asm.emitBranchLikelyNextInstruction(); ForwardReference fr =
		 * asm.forwardJcc(Assembler.LGT); // "pass" index param to C trap
		 * handler ThreadLocalState.emitMoveRegToField(asm,
		 * ArchEntrypoints.arrayIndexTrapParamField.getOffset(), indexReg); //
		 * trap asm.emitINT_Imm(RuntimeEntrypoints.TRAP_ARRAY_BOUNDS +
		 * RVM_TRAP_BASE); fr.resolve(asm);
		 */
		return machineCodes;

	}

	private List<String> emit_iaload() {
		/*
		 * asm.emitPOP_Reg(T0); // T0 is array index asm.emitPOP_Reg(S0); // S0
		 * is array ref genBoundsCheck(asm, T0, S0); // T0 is index, S0 is
		 * address of array // push [S0+T0<<2] asm.emitPUSH_RegIdx(S0, T0,
		 * Assembler.WORD, NO_SLOT);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");

		List<String> extraMachineCodes = genBoundsCheck();
		Iterator<String> mcIter = extraMachineCodes.iterator();
		String machineCode;
		while (mcIter.hasNext())
			machineCodes.add(mcIter.next());

		machineCodes.add("push %S0");
		return machineCodes;
	}

	private List<String> emit_faload() {
		return emit_iaload();
	}

	/**
	 * Emit code to load from a reference array
	 */

	private List<String> emit_aaload() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");// T0 is array index
		machineCodes.add("pop %T1");// T1 is array ref
		return machineCodes;
	}

	private List<String> emit_caload() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		extraCodes = genBoundsCheck();
		machineCodes.addAll(extraCodes);
		if (AddrJVM_32_or_64.equals("32")) {
			// asm.emitMOVZX_Reg_RegIdx_Word(T1, S0, T0, Assembler.SHORT,
			// NO_SLOT);
			machineCodes.add("mov %T1 %S0 %T0");
		} else {
			// This is supposed to be quad instruction
			machineCodes.add("mov %T1 %S0 %T0");
		}
		machineCodes.add("push %T1");
		return machineCodes;
	}

	private List<String> emit_saload() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		/*
		 * asm.emitPOP_Reg(T0); // T0 is array index asm.emitPOP_Reg(S0); // S0
		 * is array ref
		 */
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		extraCodes = genBoundsCheck(); // T0 is index, S0 is address of array
		machineCodes.addAll(extraCodes);
		if (AddrJVM_32_or_64.equals("32")) {
			// asm.emitMOVZX_Reg_RegIdx_Word(T1, S0, T0, Assembler.SHORT,
			// NO_SLOT);
			machineCodes.add("mov %T1 %S0 %T0");
		} else {
			// This is supposed to be quad instruction
			machineCodes.add("mov %T1 %S0 %T0");
		}
		machineCodes.add("push %T1");
		return machineCodes;
	}

	private List<String> emit_baload() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		/*
		 * asm.emitPOP_Reg(T0); // T0 is array index asm.emitPOP_Reg(S0); // S0
		 * is array ref
		 */
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		extraCodes = genBoundsCheck(); // T0 is index, S0 is address of array
		machineCodes.addAll(extraCodes);
		if (AddrJVM_32_or_64.equals("32")) {
			// asm.emitMOVZX_Reg_RegIdx_Word(T1, S0, T0, Assembler.SHORT,
			// NO_SLOT);
			machineCodes.add("mov %T1 %S0 %T0");
		} else {
			// This is supposed to be quad instruction
			machineCodes.add("mov %T1 %S0 %T0");
		}
		machineCodes.add("push %T1");
		return machineCodes;
	}

	private List<String> emit_laload() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %T1");
		if (AddrJVM_32_or_64.equals("32") && SSE2_BASE) {
			adjustStack(WORDSIZE * -2, true); // create space for result
		}
		extraCodes = genBoundsCheck(); // T0 is index, T1 is address of array
		machineCodes.addAll(extraCodes);
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				/*
				 * asm.emitMOVQ_Reg_RegIdx(XMM0, T1, T0, Assembler.LONG,
				 * NO_SLOT); asm.emitMOVQ_RegInd_Reg(SP, XMM0);
				 */
				machineCodes.add("mov %XMM0 %T1 T0");
				machineCodes.add("mov %SP %XMM0");
			} else {
				/*
				 * asm.emitPUSH_RegIdx(T1, T0, Assembler.LONG, ONE_SLOT); //
				 * load high part of desired long array element
				 * asm.emitPUSH_RegIdx(T1, T0, Assembler.LONG, NO_SLOT); // load
				 * low part of desired long array element
				 */
				machineCodes.add("push %T1");
				machineCodes.add("push %T1");
			}
		} else {
			adjustStack(-WORDSIZE, true);
			// asm.emitPUSH_RegIdx(T1, T0, Assembler.LONG, NO_SLOT); // load
			// desired long array element
			machineCodes.add("push %T1");
		}
		return machineCodes;
	}

	private List<String> emit_daload() {
		return emit_laload();
	}

	private List<String> boundsCheckHelper() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		/*
		 * stackMoveHelper(T0, index); // T0 is array index stackMoveHelper(S0,
		 * arrayRef); // S0 is array ref
		 */
		machineCodes.add("push %T1");
		machineCodes.add("push %T1");
		extraCodes = genBoundsCheck(); // T0 is index, S0 is address of array
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> arrayStore16bitHelper() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extaCodes = new ArrayList<String>();
		/*
		 * asm.emitPOP_Reg(T1); // T1 is the value asm.emitPOP_Reg(T0); // T0 is
		 * array index asm.emitPOP_Reg(S0); // S0 is array ref
		 * genBoundsCheck(asm, T0, S0); // T0 is index, S0 is address of array
		 * // store halfword element into array i.e. [S0 +T0] <- T1 (halfword)
		 * asm.emitMOV_RegIdx_Reg_Word(S0, T0, Assembler.SHORT, NO_SLOT, T1);
		 */
		machineCodes.add("pop %T1");
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		extaCodes = genBoundsCheck(); // T0 is index, S0 is address of array
		machineCodes.addAll(extaCodes);
		machineCodes.add("mov %S0 %T0");
		return machineCodes;
	}

	private List<String> arrayStore32bitHelper() {
		/*
		 * asm.emitPOP_Reg(T1); // T1 is the value asm.emitPOP_Reg(T0); // T0 is
		 * array index asm.emitPOP_Reg(S0); // S0 is array ref
		 * genBoundsCheck(asm, T0, S0); // T0 is index, S0 is address of array
		 * asm.emitMOV_RegIdx_Reg(S0, T0, Assembler.WORD, NO_SLOT, T1); // [S0 +
		 * T0<<2] <- T1
		 */
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		machineCodes.add("pop %T1");
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		extraCodes = genBoundsCheck(); // T0 is index, S0 is address of array
		machineCodes.addAll(extraCodes);
		machineCodes.add("mov %S0 %T0");
		return machineCodes;
	}

	private List<String> arrayStore64bitHelper() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				/*
				 * asm.emitMOVQ_Reg_RegInd(XMM0, SP); // XMM0 is the value
				 * adjustStack(WORDSIZE * 2, true); // remove value from the
				 * stack asm.emitPOP_Reg(T0); // T0 is array index
				 * asm.emitPOP_Reg(S0); // S0 is array ref
				 */
				machineCodes.add("mov %XMM0 %SP");
				adjustStack(WORDSIZE * 2, true);
				machineCodes.add("pop %T0");
				machineCodes.add("pop %S0");
			} else {
				/*
				 * asm.emitMOV_Reg_RegDisp(T0, SP, TWO_SLOTS); // T0 is the
				 * array index asm.emitMOV_Reg_RegDisp(S0, SP, THREE_SLOTS); //
				 * S0 is the array ref asm.emitMOV_Reg_RegInd(T1, SP); // low
				 * part of long value
				 */
				machineCodes.add("mov %T0 %SP");
				machineCodes.add("mov %S0 %SP");
				machineCodes.add("mov %T1 %SP");
			}
		} else {
			/*
			 * asm.emitPOP_Reg(T1); // T1 is the value adjustStack(WORDSIZE,
			 * true); // throw away slot asm.emitPOP_Reg(T0); // T0 is array
			 * index asm.emitPOP_Reg(S0); // S0 is array ref
			 */
			machineCodes.add("pop %T1");
			adjustStack(WORDSIZE, true);
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");

		}
		extraCodes = genBoundsCheck(); // T0 is index, S0 is address of array
		machineCodes.addAll(extraCodes);
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				// asm.emitMOVQ_RegIdx_Reg(S0, T0, Assembler.LONG, NO_SLOT,
				// XMM0); // [S0+T0<<<3]
				machineCodes.add("mov %S0 %T0");

			} else {
				// [S0 + T0<<3 + 0] <- T1 store low part into array
				/*
				 * asm.emitMOV_RegIdx_Reg(S0, T0, Assembler.LONG, NO_SLOT, T1);
				 * asm.emitMOV_Reg_RegDisp(T1, SP, ONE_SLOT); // high part of
				 * long // value // [S0 + T0<<3 + 4] <- T1 store high part into
				 * array adjustStack(WORDSIZE * 4, false); // remove index and
				 * ref from // the stack asm.emitMOV_RegIdx_Reg(S0, T0,
				 * Assembler.LONG, ONE_SLOT, T1);
				 */
				machineCodes.add("mov %S0 %T0");
				machineCodes.add("mov %T1 %SP");
				adjustStack(WORDSIZE * 4, false);
				machineCodes.add("mov %S0 %T0");
			}
		} else {
			// asm.emitMOV_RegIdx_Reg_Quad(S0, T0, Assembler.LONG, NO_SLOT, T1);
			// // [S0+T0<<<3]
			machineCodes.add("mov %S0 %T0");

		}
		return machineCodes;
	}

	/**
	 * Emit code to store to an int array
	 */
	private List<String> emit_iastore() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> boundCheckEmits = boundsCheckHelper();
		List<String> arrayStoreEmits = arrayStore32bitHelper();
		machineCodes.addAll(boundCheckEmits);
		machineCodes.addAll(arrayStoreEmits);
		return machineCodes;
	}

	private List<String> emit_fastore() {
		return emit_iastore();
	}

	private List<String> emit_aastore() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("mov %S0 %T0");
		machineCodes.add("cal $0");
		return machineCodes;
	}

	private List<String> emit_castore() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> boundCheckEmits = boundsCheckHelper();
		List<String> arrayStoreEmits = arrayStore16bitHelper();
		machineCodes.addAll(boundCheckEmits);
		machineCodes.addAll(arrayStoreEmits);
		return machineCodes;
	}

	private List<String> emit_sastore() {
		return emit_castore();
	}

	private List<String> emit_bastore() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> boundCheckEmits = boundsCheckHelper();
		machineCodes.addAll(boundCheckEmits);
		/*
		 * asm.emitPOP_Reg(T1); // T1 is the value asm.emitPOP_Reg(T0); // T0 is
		 * array index asm.emitPOP_Reg(S0); // S0 is array ref
		 * genBoundsCheck(asm, T0, S0); // T0 is index, S0 is address of array
		 * asm.emitMOV_RegIdx_Reg_Byte(S0, T0, Assembler.BYTE, NO_SLOT, T1); //
		 * [S0 // + // T0<<2] // <- // T1
		 */
		machineCodes.add("pop %T1");
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		boundCheckEmits = genBoundsCheck();
		machineCodes.addAll(boundCheckEmits);
		machineCodes.add("mov %S0 %T0");
		return machineCodes;
	}

	private List<String> emit_lastore() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> boundCheckEmits = boundsCheckHelper();
		List<String> arrayStoreEmits = arrayStore64bitHelper();
		machineCodes.addAll(boundCheckEmits);
		machineCodes.addAll(arrayStoreEmits);
		return machineCodes;
	}

	private List<String> emit_dastore() {
		return emit_lastore();
	}

	/**
	 * Emit code to implement the pop bytecode
	 */
	private List<String> emit_pop() {
		List<String> machineCodes = new ArrayList<String>();

		List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
		Iterator<String> mcIter = extraMachineCodes.iterator();
		String machineCode;
		while (mcIter.hasNext())
			machineCodes.add(mcIter.next());
		return machineCodes;
	}

	private List<String> emit_pop2() {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);
		Iterator<String> mcIter = extraMachineCodes.iterator();
		String machineCode;
		while (mcIter.hasNext())
			machineCodes.add(mcIter.next());
		return machineCodes;
	}

	private List<String> emit_dup() {
		List<String> machineCodes = new ArrayList<String>();
		// asm.emitPUSH_RegInd(SP);
		machineCodes.add("push %SP");
		return machineCodes;
	}

	private List<String> emit_dup_x1() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(S0); asm.emitPUSH_Reg(T0);
		 * asm.emitPUSH_Reg(S0); asm.emitPUSH_Reg(T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		machineCodes.add("push %T0");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_dup_x2() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(S0); asm.emitPOP_Reg(T1);
		 * asm.emitPUSH_Reg(T0); asm.emitPUSH_Reg(T1); asm.emitPUSH_Reg(S0);
		 * asm.emitPUSH_Reg(T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T1");
		machineCodes.add("push %T0");
		machineCodes.add("push %T1");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_dup2() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(S0); asm.emitPUSH_Reg(S0);
		 * asm.emitPUSH_Reg(T0); asm.emitPUSH_Reg(S0); asm.emitPUSH_Reg(T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_dup2_x1() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(S0); asm.emitPOP_Reg(T1);
		 * asm.emitPUSH_Reg(S0); asm.emitPUSH_Reg(T0); asm.emitPUSH_Reg(T1);
		 * asm.emitPUSH_Reg(S0); asm.emitPUSH_Reg(T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T1");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		machineCodes.add("push %T1");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_dup2_x2() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(S0); asm.emitPOP_Reg(T1);
		 * asm.emitPOP_Reg(S1); asm.emitPUSH_Reg(S0); asm.emitPUSH_Reg(T0);
		 * asm.emitPUSH_Reg(S1); asm.emitPUSH_Reg(T1); asm.emitPUSH_Reg(S0);
		 * asm.emitPUSH_Reg(T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T1");
		machineCodes.add("pop %S1");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		machineCodes.add("push %S1");
		machineCodes.add("push %T1");
		machineCodes.add("push %S0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_swap() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(S0); asm.emitPUSH_Reg(T0);
		 * asm.emitPUSH_Reg(S0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %S0");
		machineCodes.add("push %T0");
		machineCodes.add("push %S0");
		return machineCodes;
	}

	/*
	 * int ALU
	 */
	private List<String> emit_iadd() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitADD_RegInd_Reg(SP, T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		/*
		 * Generate a register(indirect)--register ADD. That is, [dstBase] +=
		 * srcReg
		 */
		machineCodes.add("add %SP %T0");
		return machineCodes;
	}

	private List<String> emit_isub() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitSUB_RegInd_Reg(SP, T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		/*
		 * Generate a register(indirect)--register SUB. That is, [dstBase] -=
		 * srcReg
		 */
		machineCodes.add("sub %SP %T0");
		return machineCodes;
	}

	private List<String> emit_imul() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitPOP_Reg(T1); asm.emitIMUL2_Reg_Reg(T0,
		 * T1); asm.emitPUSH_Reg(T0);
		 */
		// TODO: Find out what is the difference b/w imul1 and imul2
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("pop %T1");
		machineCodes.add("imul %T0 %T1");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_idiv() {
		/*
		 * asm.emitPOP_Reg(ECX); // ECX is divisor; NOTE: can't use symbolic
		 * registers because of intel hardware requirements
		 * asm.emitPOP_Reg(EAX); // EAX is dividend asm.emitCDQ(); // sign
		 * extend EAX into EDX asm.emitIDIV_Reg_Reg(EAX, ECX);
		 * asm.emitPUSH_Reg(EAX); // push result
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %ECX");
		machineCodes.add("pop %EAX");
		// TODO: Find out if CDQ needs any cycles
		/*
		 * Generate a IDIV by register. That is, EAX:EDX = EAX u/ srcReg
		 */
		machineCodes.add("idiv %EAX");
		machineCodes.add("push %EAX");
		return machineCodes;
	}

	private List<String> emit_irem() {
		/*
		 * asm.emitPOP_Reg(ECX); // ECX is divisor; NOTE: can't use symbolic
		 * registers because of intel hardware requirements
		 * asm.emitPOP_Reg(EAX); // EAX is dividend asm.emitCDQ(); // sign
		 * extend EAX into EDX asm.emitIDIV_Reg_Reg(EAX, ECX);
		 * asm.emitPUSH_Reg(EDX); // push remainder
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %ECX");
		machineCodes.add("pop %EAX");
		/*
		 * Generate a IDIV by register. That is, EAX:EDX = EAX u/ srcReg
		 */
		machineCodes.add("idiv %EAX");
		machineCodes.add("push %EDX");
		return machineCodes;
	}

	private List<String> emit_ineg() {
		// asm.emitNEG_RegInd(SP); // [SP] <- -[SP]
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * Generate a NEG to register indirect. That is, - [reg]
		 */
		machineCodes.add("neg %SP");
		return machineCodes;
	}

	private List<String> emit_ishl() {
		/*
		 * asm.emitPOP_Reg(ECX); asm.emitSHL_RegInd_Reg(SP, ECX);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %ECX");
		/*
		 * Generate a register-indirect--register SHL. That is, logical shift
		 * left of [dstBase] by srcReg
		 */
		machineCodes.add("shl %SP %ECX");
		return machineCodes;
	}

	private List<String> emit_ishr() {
		/*
		 * asm.emitPOP_Reg(ECX); asm.emitSAR_RegInd_Reg(SP, ECX);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %ECX");
		/*
		 * Generate a register-indirect--register SAR. That is, arithemetic
		 * shift right of [dstBase] by srcReg
		 */
		machineCodes.add("sar %SP %ECX");
		return machineCodes;
	}

	private List<String> emit_iushr() {
		/*
		 * asm.emitPOP_Reg(ECX); asm.emitSHR_RegInd_Reg(SP, ECX);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %ECX");
		/*
		 * Generate a register-indirect--register SAR. That is, arithemetic
		 * shift right of [dstBase] by srcReg
		 */
		machineCodes.add("shr %SP %ECX");
		return machineCodes;
	}

	private List<String> emit_iand() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitAND_RegInd_Reg(SP, T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		/*
		 * Generate a register(indirect)--register AND. That is, [dstBase] &=
		 * srcReg
		 */
		machineCodes.add("and %SP %T0");
		return machineCodes;
	}

	private List<String> emit_ior() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitOR_RegInd_Reg(SP, T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		/*
		 * Generate a register(indirect)--register AND. That is, [dstBase] |=
		 * srcReg
		 */
		machineCodes.add("or %SP %T0");
		return machineCodes;
	}

	private List<String> emit_ixor() {
		/*
		 * asm.emitPOP_Reg(T0); asm.emitXOR_RegInd_Reg(SP, T0);
		 */
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		/*
		 * Generate a register(indirect)--register AND. That is, [dstBase] ~=
		 * srcReg
		 */
		machineCodes.add("xor %SP %T0");
		return machineCodes;
	}

	private List<String> emit_iinc(int index, int val) {
		/*
		 * Offset offset = localOffset(index); asm.emitADD_RegDisp_Imm(ESP,
		 * offset, val);
		 */
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * Generate a register-displacement--immediate ADD. That is, [dstBase +
		 * dstDisp] += imm
		 */
		machineCodes.add("add %ESP " + val);
		return machineCodes;
	}

	/*
	 * long ALU
	 */
	private List<String> emit_ladd() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitPOP_Reg(T0); // the low half of one long
			 * asm.emitPOP_Reg(S0); // the high half asm.emitADD_RegInd_Reg(SP,
			 * T0); // add low halves asm.emitADC_RegDisp_Reg(SP, ONE_SLOT, S0);
			 * // add high halves with // carry
			 */
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("add %SP %T0");
			/*
			 * Generate a register-displacement--register ADC. That is, [dstBase
			 * + dstDisp] +CF= srcReg
			 */
			machineCodes.add("adc %SP %S0");
		} else {
			/*
			 * asm.emitPOP_Reg(T0); // the long value asm.emitPOP_Reg(S0); //
			 * throw away slot asm.emitADD_RegInd_Reg_Quad(SP, T0); // add
			 * values
			 */
			// TODO: Find out if quad is something spl?
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			/*
			 * Generate a register(indirect)--register ADD. That is, [dstBase]
			 * += (quad) srcReg
			 */
			machineCodes.add("add %SP %T0");
		}
		return machineCodes;
	}

	private List<String> emit_lsub() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitPOP_Reg(T0); // the low half of one long
			 * asm.emitPOP_Reg(S0); // the high half asm.emitSUB_RegInd_Reg(SP,
			 * T0); // subtract low halves asm.emitSBB_RegDisp_Reg(SP, ONE_SLOT,
			 * S0); // subtract high halves // with borrow
			 */
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			/*
			 * Generate a register(indirect)--register SUB. That is, [dstBase]
			 * -= srcReg
			 */
			machineCodes.add("sub %SP %T0");
			/*
			 * Generate a register-displacement--register SBB. That is, [dstBase
			 * + dstDisp] -CF= srcReg
			 */
			machineCodes.add("sbb %SP %S0");
		} else {
			/*
			 * asm.emitPOP_Reg(T0); // the long value adjustStack(WORDSIZE,
			 * true); // throw away slot asm.emitSUB_RegInd_Reg_Quad(SP, T0); //
			 * sub values
			 */
			machineCodes.add("pop %T0");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			String machineCode;
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("sub %SP %T0");

		}
		return machineCodes;
	}

	private List<String> emit_lmul() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("64")) {
			/*
			 * asm.emitPOP_Reg(T0); // the long value asm.emitPOP_Reg(S0); //
			 * throw away slot asm.emitIMUL2_Reg_RegInd_Quad(T0, SP);
			 * asm.emitMOV_RegInd_Reg_Quad(SP, T0);
			 */
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("imul %T0 %SP");
			machineCodes.add("pop %T0");
		} else {
			machineCodes.add("pop %EAX");
			machineCodes.add("mov %S0 %SP");
			machineCodes.add("or %EDX %S0");
			machineCodes.add("mov %EDX %SP");
			machineCodes.add("mul %EAX");
			machineCodes.add("imul %EDX %SP");
			machineCodes.add("imul %S0 %EAX");
			machineCodes.add("add %S0 %EDX");
			machineCodes.add("mul %EAX");
			machineCodes.add("add %EDX %S0");
			machineCodes.add("mov %SP %EDX");
			machineCodes.add("mov %SP %EAX");
		}
		return machineCodes;
	}

	private List<String> emit_ldiv() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("64")) {
			machineCodes.add("pop %ECX");// ECX is divisor; NOTE: can't use
			// symbolic registers because of
			// intel hardware requirements
			machineCodes.add("pop %EAX");// throw away slot
			machineCodes.add("pop %EAX");// EAX is dividend, // sign extend EAX
			// into EDX
			machineCodes.add("idiv %EAX");
			machineCodes.add("push %EAX");// push result
		} else {
			// (1) zero check
			machineCodes.add("mov %T0 %SP");
			machineCodes.add("or %T0 %SP");
			// don't know how to handle emitINT_Imm
			// GPR[] NONVOLATILE_GPRS = VM.buildFor32Addr() ? new GPR[] {
			// R5 /* EBP */, R7 /* EDI */, R3 /* EBX */} : new GPR[] { R5,
			// R7, R3 };
			// /** Number of non-volatile GPRs */
			// int NUM_NONVOLATILE_GPRS = NONVOLATILE_GPRS.length;
			int numNonVols = 3;// AddrJVM_32_or_64.equals("32")?
			machineCodes.add("push %R5");
			machineCodes.add("push %R7");
			machineCodes.add("push %R3");

			// (3) Push args to C function (reversed)
			machineCodes.add("push %SP");
			machineCodes.add("push %SP");
			machineCodes.add("push %SP");
			machineCodes.add("push %SP");

			// (4) invoke C function through bootrecord
			machineCodes.add("mov %S0 Abs");
			machineCodes.add("cal %S0");

			// (5) pop space for arguments
			List<String> extraMachineCodes = adjustStack(4 * WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());

			// (6) restore RVM nonvolatiles
			machineCodes.add("pop %R5");
			machineCodes.add("pop %R7");
			machineCodes.add("pop %R3");

			// (7) pop expression stack
			extraMachineCodes = adjustStack(4 * WORDSIZE, true);
			mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());

			// (8) push results
			machineCodes.add("push %T1");
			machineCodes.add("push %T0");

		}
		return machineCodes;
	}

	private List<String> emit_lrem() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("64")) {
			machineCodes.add("pop %ECX");// ECX is divisor; NOTE: can't use
			// symbolic registers because of
			// intel hardware requirements
			machineCodes.add("pop %EAX");// throw away slot
			machineCodes.add("pop %EAX");// EAX is dividend
			machineCodes.add("idiv %EAX");
			machineCodes.add("push %EDX");// push result
		} else {
			// (1) zero check
			machineCodes.add("mov %T0 %SP");
			machineCodes.add("or %T0 %SP");
			// don't know how to handle emitINT_Imm
			// GPR[] NONVOLATILE_GPRS = VM.buildFor32Addr() ? new GPR[] {
			// R5 /* EBP */, R7 /* EDI */, R3 /* EBX */} : new GPR[] { R5,
			// R7, R3 };
			// /** Number of non-volatile GPRs */
			// int NUM_NONVOLATILE_GPRS = NONVOLATILE_GPRS.length;
			int numNonVols = 3;// AddrJVM_32_or_64.equals("32")?
			machineCodes.add("push %R5");
			machineCodes.add("push %R7");
			machineCodes.add("push %R3");

			// (3) Push args to C function (reversed)
			machineCodes.add("push %SP");
			machineCodes.add("push %SP");
			machineCodes.add("push %SP");
			machineCodes.add("push %SP");

			// (4) invoke C function through bootrecord
			machineCodes.add("mov %S0 Abs");
			machineCodes.add("cal %S0");

			// (5) pop space for arguments
			List<String> extraMachineCodes = adjustStack(4 * WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());

			// (6) restore RVM nonvolatiles
			machineCodes.add("pop %R5");
			machineCodes.add("pop %R7");
			machineCodes.add("pop %R3");

			// (7) pop expression stack
			extraMachineCodes = adjustStack(4 * WORDSIZE, true);
			mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());

			// (8) push results
			machineCodes.add("push %T1");
			machineCodes.add("push %T0");
		}
		return machineCodes;
	}

	private List<String> emit_lneg() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitPOP_Reg(T0); // T0 = low asm.emitNEG_Reg(T0); // T0 =
			 * -low asm.emitPOP_Reg(T1); // T1 = high asm.emitADC_Reg_Imm(T1,
			 * 0); // T1 = high + 0 + CF asm.emitNEG_Reg(T1); // T1 = -T1
			 * asm.emitPUSH_Reg(T1); asm.emitPUSH_Reg(T0);
			 */
			machineCodes.add("pop %T0");
			machineCodes.add("neg %T0");
			machineCodes.add("pop %T1");
			machineCodes.add("adc %T1 $0");
			machineCodes.add("neg %T1");
			machineCodes.add("push %T1");
			machineCodes.add("push %T0");
		} else {
			machineCodes.add("neg %SP");
		}
		return machineCodes;
	}

	private List<String> emit_lshl() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				machineCodes.add("pop %T0");
				machineCodes.add("mov %XMM1 %SP");
				machineCodes.add("and %T0 $0x3F");
				machineCodes.add("mov %XMM0 %T0");
				/*
				 * Generate a register--register PSLLQ. That is, dstReg <<=
				 * (quad) srcReg
				 */
				machineCodes.add("psll %XMM1 %XMM0");
				machineCodes.add("mov %SP %XMM1");
			} else {
				machineCodes.add("pop %ECX");
				machineCodes.add("pop %T0");
				machineCodes.add("pop %T1");
				machineCodes.add("test %ECX $32");
				machineCodes.add("shld %T1 %T0 %ECX");
				machineCodes.add("shl %T0 %ECX");
				machineCodes.add("mov %T1 %T0");
				machineCodes.add("shl %T1 %ECX");
				machineCodes.add("xor %T0 %T0");
				machineCodes.add("push %T1");
				machineCodes.add("push %T0");
			}

		} else {
			machineCodes.add("pop %ECX");
			machineCodes.add("shl %SP %ECX");
		}
		return machineCodes;
	}

	private List<String> emit_lshr() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("pop %ECX");
			machineCodes.add("pop %T0");
			machineCodes.add("pop %T1");
			machineCodes.add("test %ECX $32");
			machineCodes.add("shrd %T0 %T1 %ECX");
			machineCodes.add("sar %T1 %ECX");
			machineCodes.add("mov %T0 %T1");
			machineCodes.add("sar %T1 $31");
			machineCodes.add("sar %T0 %ECX");
			machineCodes.add("push %T1");
			machineCodes.add("push %T0");
		} else {
			machineCodes.add("pop %ECX");
			machineCodes.add("sar %SP %ECX");
		}
		return machineCodes;
	}

	private List<String> emit_lushr() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			if (SSE2_BASE) {
				machineCodes.add("pop %T0");
				machineCodes.add("mov %XMM1 %SP");
				machineCodes.add("and %T0 $0x3F");
				machineCodes.add("mov %XMM0 %T0");
				machineCodes.add("psrl %XMM1 %XMM0");
				machineCodes.add("mov %SP %XMM1");
			} else {
				machineCodes.add("pop %ECX");
				machineCodes.add("pop %T0");
				machineCodes.add("pop %T1");
				machineCodes.add("test %ECX $32");
				machineCodes.add("shrd %T0 %T1 %ECX");
				machineCodes.add("shr %T1 %ECX");
				machineCodes.add("mov %T0 %T1");
				machineCodes.add("xor %T1 %T1");
				machineCodes.add("shr %T0 %ECX");
				machineCodes.add("push %T1");
				machineCodes.add("push %T0");
			}
		} else {
			machineCodes.add("pop %ECX");
			machineCodes.add("shr %SP %ECX");
		}
		return machineCodes;
	}

	private List<String> emit_land() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("and %SP %T0");
			machineCodes.add("and %SP %S0");
		} else {
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("and %SP %T0");
		}
		return machineCodes;
	}

	private List<String> emit_lor() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("or %SP %T0");
			machineCodes.add("or %SP %S0");
		} else {
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("or %SP %T0");
		}
		return machineCodes;
	}

	private List<String> emit_lxor() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("xor %SP %T0");
			machineCodes.add("xor %SP %S0");
		} else {
			machineCodes.add("pop %T0");
			machineCodes.add("pop %S0");
			machineCodes.add("xor %SP %T0");
		}
		return machineCodes;
	}

	private List<String> emit_fadd() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");
			machineCodes.add("add %XMM0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");

		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fadd %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}

		return machineCodes;
	}

	private List<String> emit_fsub() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			/*
			 * asm.emitMOVSS_Reg_RegDisp(XMM0, SP, ONE_SLOT); // XMM0 = value1
			 * asm.emitSUBSS_Reg_RegInd(XMM0, SP); // XMM0 -= value2
			 * adjustStack(WORDSIZE, true); // throw away slot
			 * asm.emitMOVSS_RegInd_Reg(SP, XMM0); // set result on stack
			 */
			machineCodes.add("mov %XMM0 %SP");
			machineCodes.add("sub %XMM0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");

		} else {
			/*
			 * asm.emitFLD_Reg_RegDisp(FP0, SP, ONE_SLOT); // FPU reg. stack <-
			 * // value1 asm.emitFSUB_Reg_RegDisp(FP0, SP, NO_SLOT); // FPU reg.
			 * stack -= // value2 adjustStack(WORDSIZE, true); // throw away
			 * slot asm.emitFSTP_RegInd_Reg(SP, FP0); // POP FPU reg. stack onto
			 * stack
			 */
			machineCodes.add("fld %FP0");
			machineCodes.add("fsub %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_fmul() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");
			machineCodes.add("mul %XMM0");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");

		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fmul %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_fdiv() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");
			machineCodes.add("div %XMM0");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");

		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fdiv %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_frem() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("fld %FP0"); // FPU reg. stack <- value2, or a
		machineCodes.add("fld %FP0"); // FPU reg. stack <- value1, or b
		// emitFPREM();
		machineCodes.add("fstp %SP"); // POP FPU reg. stack (results) onto
		// java stack
		machineCodes.add("fstp %SP");
		List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
		Iterator<String> mcIter = extraMachineCodes.iterator();
		while (mcIter.hasNext())
			machineCodes.add(mcIter.next());
		return machineCodes;
	}

	private List<String> emit_fneg() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("xor %SP $0x80000000");
		return machineCodes;
	}

	private List<String> emit_dadd() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("add %XMM0 %SP");// XMM0 += value1
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");
		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fadd %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_dsub() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("sub %XMM0 %SP");// XMM0 += value1
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");
		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fsub %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_dmul() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("mul %XMM0");// XMM0 += value1
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");
		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fmul %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_ddiv() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("div %XMM0");// XMM0 += value1
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("mov %SP %XMM0");
		} else {
			machineCodes.add("fld %FP0");
			machineCodes.add("fdiv %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(WORDSIZE * 2, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_drem() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("fld %FP0"); // FPU reg. stack <- value2, or a
		machineCodes.add("fld %FP0"); // FPU reg. stack <- value1, or b
		// emitFPREM();
		machineCodes.add("fstp %SP"); // POP FPU reg. stack (results) onto
		// java stack
		machineCodes.add("fstp %SP");
		List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);
		Iterator<String> mcIter = extraMachineCodes.iterator();
		while (mcIter.hasNext())
			machineCodes.add(mcIter.next());
		return machineCodes;
	}

	private List<String> emit_dneg() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("xor %SP $0x80000000");
		return machineCodes;
	}

	/*
	 * Conversion ops
	 */

	private List<String> emit_i2l() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("push %SP");// duplicate int on stack
			machineCodes.add("sar %SP $31");// sign extend as high word of long
		} else {
			machineCodes.add("pop %EAX");
			// emitCDQE ()
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("push %EAX");
		}

		return machineCodes;
	}

	private List<String> emit_l2i() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");// long value
		List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
		Iterator<String> mcIter = extraMachineCodes.iterator();
		while (mcIter.hasNext())
			machineCodes.add(mcIter.next());
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_i2f() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			/*
			 * asm.emitCVTSI2SS_Reg_RegInd(XMM0, SP);
			 * asm.emitMOVSS_RegInd_Reg(SP, XMM0);
			 */
			machineCodes.add("mov %SP %XMM0");
		} else {
			/*
			 * asm.emitFILD_Reg_RegInd(FP0, SP); asm.emitFSTP_RegInd_Reg(SP,
			 * FP0);
			 */
			machineCodes.add("fild %FP0 %SP");
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_i2d() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			/*
			 * asm.emitCVTSI2SD_Reg_RegInd(XMM0, SP); adjustStack(-WORDSIZE,
			 * true); // grow the stack asm.emitMOVLPD_RegInd_Reg(SP, XMM0);
			 */
			// machineCodes.add("fstp %SP %FP0");
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("fstp %SP");

		} else {
			/*
			 * asm.emitFILD_Reg_RegInd(FP0, SP); adjustStack(-WORDSIZE, true);
			 * // grow the stack asm.emitFSTP_RegInd_Reg_Quad(SP, FP0);
			 */
			machineCodes.add("fild %FP0 %SP");
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("fstp %SP");

		}
		return machineCodes;
	}

	private List<String> emit_l2f() {
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * asm.emitFILD_Reg_RegInd_Quad(FP0, SP); adjustStack(WORDSIZE, true);
		 * // shrink the stack asm.emitFSTP_RegInd_Reg(SP, FP0);
		 */
		machineCodes.add("fild %FP0 %SP");
		List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
		machineCodes.addAll(extraMachineCodes);
		machineCodes.add("fstp %SP");
		return machineCodes;
	}

	private List<String> emit_l2d() {
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * asm.emitFILD_Reg_RegInd_Quad(FP0, SP);
		 * asm.emitFSTP_RegInd_Reg_Quad(SP, FP0);
		 */
		machineCodes.add("fild %FP0 %SP");
		machineCodes.add("fstp %SP");
		return machineCodes;
	}

	private List<String> emit_f2d() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			/*
			 * asm.emitCVTSS2SD_Reg_RegInd(XMM0, SP); adjustStack(-WORDSIZE,
			 * true); // throw away slot asm.emitMOVLPD_RegInd_Reg(SP, XMM0);
			 */
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("mov %SP %XMM0");
		} else {
			/*
			 * asm.emitFLD_Reg_RegInd(FP0, SP); adjustStack(-WORDSIZE, true); //
			 * throw away slot asm.emitFSTP_RegInd_Reg_Quad(SP, FP0);
			 */
			machineCodes.add("fld %FP0");
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_d2f() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			/*
			 * asm.emitCVTSD2SS_Reg_RegInd(XMM0, SP); adjustStack(WORDSIZE,
			 * true); // throw away slot asm.emitMOVSS_RegInd_Reg(SP, XMM0);
			 */
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("mov %SP %XMM0");
		} else {
			/*
			 * asm.emitFLD_Reg_RegInd_Quad(FP0, SP); adjustStack(WORDSIZE,
			 * true); // throw away slot asm.emitFSTP_RegInd_Reg(SP, FP0);
			 */
			machineCodes.add("fld %FP0");
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("fstp %SP");
		}
		return machineCodes;
	}

	private List<String> emit_f2i() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			// Set up max int in XMM0
			/*
			 * asm.emitMOVSS_Reg_Abs(XMM0, Magic.getTocPointer().plus(
			 * Entrypoints.maxintFloatField.getOffset())); // Set up value in
			 * XMM1 asm.emitMOVSS_Reg_RegInd(XMM1, SP); // if value > maxint or
			 * NaN goto fr1; FP0 = value asm.emitUCOMISS_Reg_Reg(XMM0, XMM1);
			 * ForwardReference fr1 = asm.forwardJcc(Assembler.LLE);
			 * asm.emitCVTTSS2SI_Reg_Reg(T0, XMM1); asm.emitMOV_RegInd_Reg(SP,
			 * T0); ForwardReference fr2 = asm.forwardJMP(); fr1.resolve(asm);
			 * ForwardReference fr3 = asm.forwardJcc(Assembler.PE); // if value
			 * == // NaN goto // fr3 asm.emitMOV_RegInd_Imm(SP, 0x7FFFFFFF);
			 * ForwardReference fr4 = asm.forwardJMP(); fr3.resolve(asm);
			 * asm.emitMOV_RegInd_Imm(SP, 0); fr2.resolve(asm);
			 * fr4.resolve(asm);
			 */
			machineCodes.add("mov %XMM0 $0");
			machineCodes.add("mov %XMM1 %SP");
			machineCodes.add("FUCOM %XMM0 $0");
			machineCodes.add("mov %SP %T0");
			machineCodes.add("mov %SP $0x7FFFFFFF");

		} else {
			// TODO: use x87 operations to do this conversion inline taking care
			// of
			// the boundary cases that differ between x87 and Java

			// (1) save RVM nonvolatiles
			/*
			 * int numNonVols = NONVOLATILE_GPRS.length; Offset off =
			 * Offset.fromIntSignExtend(numNonVols * WORDSIZE); for (int i = 0;
			 * i < numNonVols; i++) { asm.emitPUSH_Reg(NONVOLATILE_GPRS[i]); }
			 * // (2) Push arg to C function asm.emitPUSH_RegDisp(SP, off); //
			 * (3) invoke C function through bootrecord asm.emitMOV_Reg_Abs(S0,
			 * Magic.getTocPointer().plus(
			 * Entrypoints.the_boot_recordField.getOffset()));
			 * asm.emitCALL_RegDisp(S0, Entrypoints.sysFloatToIntIPField
			 * .getOffset()); // (4) pop argument; asm.emitPOP_Reg(S0); // (5)
			 * restore RVM nonvolatiles for (int i = numNonVols - 1; i >= 0;
			 * i--) { asm.emitPOP_Reg(NONVOLATILE_GPRS[i]); } // (6) put result
			 * on expression stack asm.emitMOV_RegInd_Reg(SP, T0);
			 */
			machineCodes.add("push %R5");
			machineCodes.add("push %R7");
			machineCodes.add("push %R3");
			machineCodes.add("push $0");
			machineCodes.add("mov %SP $0");
			machineCodes.add("cal %S0");
			machineCodes.add("pop %S0");
			machineCodes.add("pop %R5");
			machineCodes.add("pop %R7");
			machineCodes.add("pop %R3");
		}
		return machineCodes;
	}

	private List<String> emit_f2l() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			// TODO: SSE3 has a FISTTP instruction that stores the value with
			// truncation
			// meaning the FPSCW can be left alone

			// Setup value into FP1
			/*
			 * asm.emitFLD_Reg_RegInd(FP0, SP); // Setup maxlong into FP0
			 * asm.emitFLD_Reg_Abs(FP0, Magic.getTocPointer().plus(
			 * Entrypoints.maxlongFloatField.getOffset())); // if value >
			 * maxlong or NaN goto fr1; FP0 = value asm.emitFUCOMIP_Reg_Reg(FP0,
			 * FP1); ForwardReference fr1 = asm.forwardJcc(Assembler.LLE); //
			 * Normally the status and control word rounds numbers, but for //
			 * conversion // to an integer/long value we want truncation. We
			 * therefore save // the FPSCW, // set it to truncation perform
			 * operation then restore adjustStack(-WORDSIZE, true); // Grow the
			 * stack asm.emitFNSTCW_RegDisp(SP, MINUS_ONE_SLOT); // [SP-4] =
			 * fpscw asm.emitMOVZX_Reg_RegDisp_Word(T0, SP, MINUS_ONE_SLOT); //
			 * EAX = // fpscw asm.emitOR_Reg_Imm(T0, 0xC00); // EAX = FPSCW in
			 * truncate mode asm.emitMOV_RegInd_Reg(SP, T0); // [SP] = new fpscw
			 * value asm.emitFLDCW_RegInd(SP); // Set FPSCW
			 * asm.emitFISTP_RegInd_Reg_Quad(SP, FP0); // Store 64bit long
			 * asm.emitFLDCW_RegDisp(SP, MINUS_ONE_SLOT); // Restore FPSCW
			 * ForwardReference fr2 = asm.forwardJMP(); fr1.resolve(asm);
			 * asm.emitFSTP_Reg_Reg(FP0, FP0); // pop FPU*1 ForwardReference fr3
			 * = asm.forwardJcc(Assembler.PE); // if value == // NaN goto // fr3
			 * asm.emitMOV_RegInd_Imm(SP, 0x7FFFFFFF); asm.emitPUSH_Imm(-1);
			 * ForwardReference fr4 = asm.forwardJMP(); fr3.resolve(asm);
			 * asm.emitMOV_RegInd_Imm(SP, 0); asm.emitPUSH_Imm(0);
			 * fr2.resolve(asm); fr4.resolve(asm);
			 */
			machineCodes.add("fld %FP0");
			machineCodes.add("fld %FP0");
			machineCodes.add("fucom %FP0 %FP1");
			List<String> extraMachineCodes = adjustStack(-WORDSIZE, true);
			machineCodes.addAll(extraMachineCodes);
			machineCodes.add("fnst %SP");
			machineCodes.add("mov %T0 %SP");
			machineCodes.add("or %T0 $0xC00");
			machineCodes.add("fld %SP");
			machineCodes.add("fist %SP %FP0");
			machineCodes.add("fld %SP");
			machineCodes.add("mov %SP $0x7FFFFFFF");
			machineCodes.add("push $-1");
			machineCodes.add("mov %SP $0");
			machineCodes.add("push $0");
		} else {
			// Set up max int in XMM0
			/*
			 * asm.emitMOVSS_Reg_Abs(XMM0, Magic.getTocPointer().plus(
			 * Entrypoints.maxlongFloatField.getOffset())); // Set up value in
			 * XMM1 asm.emitMOVSS_Reg_RegInd(XMM1, SP); // if value > maxint or
			 * NaN goto fr1; FP0 = value asm.emitUCOMISS_Reg_Reg(XMM0, XMM1);
			 * ForwardReference fr1 = asm.forwardJcc(Assembler.LLE);
			 * asm.emitCVTTSS2SI_Reg_Reg_Quad(T0, XMM1); ForwardReference fr2 =
			 * asm.forwardJMP(); fr1.resolve(asm); ForwardReference fr3 =
			 * asm.forwardJcc(Assembler.PE); // if value == // NaN goto // fr3
			 * asm.emitMOV_Reg_Imm_Quad(T0, 0x7FFFFFFFFFFFFFFFL);
			 * ForwardReference fr4 = asm.forwardJMP(); fr3.resolve(asm);
			 * asm.emitXOR_Reg_Reg(T0, T0); fr2.resolve(asm); fr4.resolve(asm);
			 * asm.emitPUSH_Reg(T0);
			 */
			machineCodes.add("mov %XMM0 $0");
			machineCodes.add("mov %XMM1 %SP");
			machineCodes.add("fucom %XMM0 %XMM1");
			machineCodes.add("mov %T0 $0x7FFFFFFFFFFFFFFFL");
			machineCodes.add("xor %T0 %T0");
			machineCodes.add("push %T0");
		}
		return machineCodes;
	}

	private List<String> emit_d2i() {
		List<String> machineCodes = new ArrayList<String>();
		if (SSE2_BASE) {
			// Set up max int in XMM0
			/*
			 * asm.emitMOVLPD_Reg_Abs(XMM0, Magic.getTocPointer().plus(
			 * Entrypoints.maxintField.getOffset())); // Set up value in XMM1
			 * asm.emitMOVLPD_Reg_RegInd(XMM1, SP); adjustStack(WORDSIZE, true);
			 * // throw away slot // if value > maxint or NaN goto fr1; FP0 =
			 * value asm.emitUCOMISD_Reg_Reg(XMM0, XMM1); ForwardReference fr1 =
			 * asm.forwardJcc(Assembler.LLE); asm.emitCVTTSD2SI_Reg_Reg(T0,
			 * XMM1); asm.emitMOV_RegInd_Reg(SP, T0); ForwardReference fr2 =
			 * asm.forwardJMP(); fr1.resolve(asm); ForwardReference fr3 =
			 * asm.forwardJcc(Assembler.PE); // if value == // NaN goto // fr3
			 * asm.emitMOV_RegInd_Imm(SP, 0x7FFFFFFF); ForwardReference fr4 =
			 * asm.forwardJMP(); fr3.resolve(asm); asm.emitMOV_RegInd_Imm(SP,
			 * 0); fr2.resolve(asm); fr4.resolve(asm);
			 */
			machineCodes.add("mov %XMM0 $0");
			machineCodes.add("mov %XMM1 %SP");
			List<String> extraCodes = adjustStack(WORDSIZE, true);
			machineCodes.addAll(extraCodes);
			machineCodes.add("fucom %XMM0 %XMM1");
			machineCodes.add("mov %SP %T0");
			machineCodes.add("mov %SP $0x7FFFFFFF");
			machineCodes.add("mov %SP $0");
		} else {
			// TODO: use x87 operations to do this conversion inline taking care
			// of
			// the boundary cases that differ between x87 and Java
			// (1) save RVM nonvolatiles
			/*
			 * int numNonVols = NONVOLATILE_GPRS.length; Offset off =
			 * Offset.fromIntSignExtend(numNonVols * WORDSIZE); for (int i = 0;
			 * i < numNonVols; i++) { asm.emitPUSH_Reg(NONVOLATILE_GPRS[i]); }
			 * // (2) Push args to C function (reversed)
			 * asm.emitPUSH_RegDisp(SP, off.plus(4)); asm.emitPUSH_RegDisp(SP,
			 * off.plus(4)); // (3) invoke C function through bootrecord
			 * asm.emitMOV_Reg_Abs(S0, Magic.getTocPointer().plus(
			 * Entrypoints.the_boot_recordField.getOffset()));
			 * asm.emitCALL_RegDisp(S0, Entrypoints.sysDoubleToIntIPField
			 * .getOffset()); // (4) pop arguments asm.emitPOP_Reg(S0);
			 * asm.emitPOP_Reg(S0); // (5) restore RVM nonvolatiles for (int i =
			 * numNonVols - 1; i >= 0; i--) {
			 * asm.emitPOP_Reg(NONVOLATILE_GPRS[i]); } // (6) put result on
			 * expression stack adjustStack(WORDSIZE, true); // throw away slot
			 * asm.emitMOV_RegInd_Reg(SP, T0);
			 */
			machineCodes.add("push %R5");
			machineCodes.add("push %R7");
			machineCodes.add("push %R3");
			machineCodes.add("push $0");
			machineCodes.add("push $0");
			machineCodes.add("mov %SP $0");
			machineCodes.add("cal %S0");
			machineCodes.add("pop %S0");
			machineCodes.add("pop %S0");
			machineCodes.add("pop %R5");
			machineCodes.add("pop %R7");
			machineCodes.add("pop %R3");
			List<String> extraCodes = adjustStack(WORDSIZE, true);
			machineCodes.addAll(extraCodes);
			machineCodes.add("mov %SP %T0");

		}
		return machineCodes;
	}

	private List<String> emit_d2l() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			// TODO: SSE3 has a FISTTP instruction that stores the value with
			// truncation
			// meaning the FPSCW can be left alone
			// Setup value into FP1
			machineCodes.add("fld %FP0");
			// Setup maxlong into FP0
			machineCodes.add("fld %FP0");
			// if value > maxlong or NaN goto fr1; FP0 = value
			machineCodes.add("fucom %FP0 %FP1");
			// Normally the status and control word rounds numbers, but for
			// conversion
			// to an integer/long value we want truncation. We therefore save
			// the FPSCW,
			// set it to truncation perform operation then restore
			machineCodes.add("fnst %SP");// [SP-4] = fpscw
			machineCodes.add("mov %T0 %SP");// EAX = fpscw
			machineCodes.add("or %T0 $0xC00");// EAX = FPSCW in truncate mode
			machineCodes.add("mov %SP %T0");// [SP] = new fpscw value
			machineCodes.add("fld %SP");// Set FPSCW
			machineCodes.add("fst %SP %FP0");// Store 64bit long
			machineCodes.add("fld %SP");// Restore FPSCW
			machineCodes.add("fstp %FP0");// pop FPU*1
			machineCodes.add("mov %SP $0x7FFFFFFF");
			machineCodes.add("mov %SP $-1");
			machineCodes.add("mov %SP $0");
			machineCodes.add("mov %SP $0");

		} else {

		}

		return machineCodes;
	}

	private List<String> emit_i2b() {
		// This could be coded as 2 instructions as follows:
		// asm.emitMOVSX_Reg_RegInd_Byte(T0, SP);
		// asm.emitMOV_RegInd_Reg(SP, T0);
		// Indirection via ESP requires an extra byte for the indirection, so
		// the
		// total code size is 6 bytes. The 3 instruction version below is only 4
		// bytes long and faster on Pentium 4 benchmarks.
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("mov %T0 %T0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_i2c() {
		// This could be coded as zeroing the high 16bits on stack:
		// asm.emitMOV_RegDisp_Imm_Word(SP, Offset.fromIntSignExtend(2), 0);
		// or as 2 instructions:
		// asm.emitMOVZX_Reg_RegInd_Word(T0, SP);
		// asm.emitMOV_RegInd_Reg(SP, T0);
		// Benchmarks show the following sequence to be more optimal on a
		// Pentium 4
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("mov %T0 %T0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_i2s() {
		// This could be coded as 2 instructions as follows:
		// asm.emitMOVSX_Reg_RegInd_Word(T0, SP);
		// asm.emitMOV_RegInd_Reg(SP, T0);
		// Indirection via ESP requires an extra byte for the indirection, so
		// the
		// total code size is 6 bytes. The 3 instruction version below is only 4
		// bytes long and faster on Pentium 4 benchmarks.
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("mov %T0 %T0");
		machineCodes.add("push %T0");
		return machineCodes;
	}

	/*
	 * Comparision operations
	 */

	private List<String> emit_lcmp() {
		List<String> machineCodes = new ArrayList<String>();
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("pop %T0");// (S0:T0) = (high half value2: low half
			// value2)
			machineCodes.add("pop %S0");
			machineCodes.add("pop %T1");// (..:T1) = (.. : low half of value1)
			machineCodes.add("sub %T1 %T0");// T1 = T1 - T0
			machineCodes.add("pop %T0");// (T0:..) = (high half of value1 : ..)
			// NB pop does not alter the carry register
			machineCodes.add("sbb %T0 %S0");// T0 = T0 - S0 - CF
			machineCodes.add("or %T0 %T1");// T0 = T0 | T1
			machineCodes.add("push $0");// push result on stack
			machineCodes.add("push $1");// push result on stack
			machineCodes.add("push $-1");// push result on stack
		} else {
			// TODO: consider optimizing to z = ((x - y) >> 63) - ((y - x) >>
			// 63)
			machineCodes.add("pop %T0");// T0 is long value
			List<String> extraMachineCodes = adjustStack(WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("pop %T1");// T1 is long value
			extraMachineCodes = adjustStack(WORDSIZE, true);// throw away slot
			mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("cmp %T1 %T0");// T1 = T1 - T0
			machineCodes.add("push $0");// push result on stack
			machineCodes.add("push $1");// push result on stack
			machineCodes.add("push $-1");// push result on stack
		}
		return machineCodes;
	}

	private List<String> emit_fcmpl() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("xor %T0 %T0");// T0 = 0
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("mov %XMM1 %SP");// XMM1 = value1
			List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare value1 and value2
		} else {
			machineCodes.add("fld %FP0");// Setup value2 into FP1,
			machineCodes.add("fld %FP0");// value1 into FP0
			List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare and pop FPU *1
		}
		machineCodes.add("set %T0");// T0 = XMM0 > XMM1 ? 1 : 0
		machineCodes.add("sbb %T0 $0");// T0 -= XMM0 < or unordered XMM1 ? 1 : 0
		machineCodes.add("push %T0");// push result on stack

		if (!SSE2_BASE) {
			machineCodes.add("fstp %FP0");// pop FPU*1
		}
		return machineCodes;
	}

	private List<String> emit_fcmpg() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("xor %T0 %T0");// T0 = 0
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("mov %XMM1 %SP");// XMM1 = value1
			List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare value1 and value2
		} else {
			machineCodes.add("fld %FP0");// Setup value2 into FP1,
			machineCodes.add("fld %FP0");// value1 into FP0
			List<String> extraMachineCodes = adjustStack(2 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare and pop FPU *1
		}
		machineCodes.add("set %T0");// T0 = XMM0 > XMM1 ? 1 : 0
		machineCodes.add("sbb %T0 $0");// T0 -= XMM0 < or unordered XMM1 ? 1 : 0
		machineCodes.add("push %T0");// push result on stack
		machineCodes.add("push $1");// push 1 on stack
		if (!SSE2_BASE) {
			machineCodes.add("fstp %FP0");// pop FPU*1
		}

		return machineCodes;
	}

	private List<String> emit_dcmpl() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("xor %T0 %T0");// T0 = 0
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("mov %XMM1 %SP");// XMM1 = value1
			List<String> extraMachineCodes = adjustStack(4 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare value1 and value2
		} else {
			machineCodes.add("fld %FP0");// Setup value2 into FP1,
			machineCodes.add("fld %FP0");// value1 into FP0
			List<String> extraMachineCodes = adjustStack(4 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare and pop FPU *1
		}
		machineCodes.add("set %T0");// T0 = XMM0 > XMM1 ? 1 : 0
		machineCodes.add("sbb %T0 $0");// T0 -= XMM0 < or unordered XMM1 ? 1 : 0
		machineCodes.add("push %T0");// push result on stack

		if (!SSE2_BASE) {
			machineCodes.add("fstp %FP0");// pop FPU*1
		}

		return machineCodes;
	}

	private List<String> emit_dcmpg() {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("xor %T0 %T0");// T0 = 0
		if (SSE2_BASE) {
			machineCodes.add("mov %XMM0 %SP");// XMM0 = value2
			machineCodes.add("mov %XMM1 %SP");// XMM1 = value1
			List<String> extraMachineCodes = adjustStack(4 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare value1 and value2
		} else {
			machineCodes.add("fld %FP0");// Setup value2 into FP1,
			machineCodes.add("fld %FP0");// value1 into FP0
			List<String> extraMachineCodes = adjustStack(4 * WORDSIZE, true);// throw
			// away
			// slots
			Iterator<String> mcIter = extraMachineCodes.iterator();
			while (mcIter.hasNext())
				machineCodes.add(mcIter.next());
			machineCodes.add("fucom %XMM1 %XMM0");// compare and pop FPU *1
		}
		machineCodes.add("set %T0");// T0 = XMM0 > XMM1 ? 1 : 0
		machineCodes.add("sbb %T0 $0");// T0 -= XMM0 < or unordered XMM1 ? 1 : 0
		machineCodes.add("push %T0");// push result on stack
		machineCodes.add("push $1");// push 1 on stack
		if (!SSE2_BASE) {
			machineCodes.add("fstp %FP0");// pop FPU*1
		}
		return machineCodes;
	}

	/*
	 * Branching
	 */

	// For all the cases goCondBranch emits jmp instruction
	private List<String> emit_ifeq(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		machineCodes.add("test %T0 %T0");
		// genCondBranch(, 0);
		machineCodes.add("jmp EQ $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ifne(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("test %T0 %T0");
		machineCodes.add("jmp NE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_iflt(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("test %T0 %T0");
		machineCodes.add("jmp LT $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ifge(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("test %T0 %T0");
		machineCodes.add("jmp GE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ifgt(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("test %T0 %T0");
		machineCodes.add("jmp GT $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ifle(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("test %T0 %T0");
		machineCodes.add("jmp LE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_icmpeq(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		machineCodes.add("cmp %T0 %S0");
		machineCodes.add("jmp EQ $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_icmpne(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		machineCodes.add("cmp %T0 %S0");
		machineCodes.add("jmp NE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_icmplt(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		machineCodes.add("cmp %T0 %S0");
		machineCodes.add("jmp LT $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_icmpge(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		machineCodes.add("cmp %T0 %S0");
		machineCodes.add("jmp GE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_icmpgt(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		machineCodes.add("cmp %T0 %S0");
		machineCodes.add("jmp GT $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_icmple(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		machineCodes.add("cmp %T0 %S0");
		machineCodes.add("jmp LE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_acmpeq(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * asm.emitPOP_Reg(S0); asm.emitPOP_Reg(T0);
		 */
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		if (AddrJVM_32_or_64.equals("32")) {
			// asm.emitCMP_Reg_Reg(T0, S0);
			machineCodes.add("cmp %T0 %S0");
		} else {
			// asm.emitCMP_Reg_Reg_Quad(T0, S0);
			machineCodes.add("cmp %T0 %S0");
		}
		machineCodes.add("jmp EQ $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_if_acmpne(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %S0");
		machineCodes.add("pop %T0");
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("cmp %T0 %S0");
		} else {
			machineCodes.add("cmp %T0 %S0");
		}
		machineCodes.add("jmp NE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ifnull(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("test %T0 %T0");
		} else {
			machineCodes.add("test %T0 %T0");
		}
		machineCodes.add("jmp EQ $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ifnonnull(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("pop %T0");
		if (AddrJVM_32_or_64.equals("32")) {
			machineCodes.add("test %T0 %T0");
		} else {
			machineCodes.add("test %T0 %T0");
		}
		machineCodes.add("jmp NE $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_goto(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("jmp $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_jsr(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("call $" + bTarget);
		return machineCodes;
	}

	private List<String> emit_ret(int index) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("push %ESP");
		machineCodes.add("ret");
		return machineCodes;
	}

	private List<String> emit_tableswitch(int defaultval, int low, int high) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_lookupswitch(int defaultval, int npairs) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> genEpilogue(int returnSize, int bytesPopped) {
		List<String> machineCodes = new ArrayList<String>();
		// adjustStack(LG_WORDSIZE, true);
		// I don't care handling annotations, so i'm considering
		// all methods are normal here. TODO: Visit and correct this.
		int spaceToRelease = returnSize - bytesPopped;
		List<String> extraCodes = adjustStack(spaceToRelease, true);
		machineCodes.addAll(extraCodes);
		machineCodes.add("pop %EBX");
		machineCodes.add("pop %EDI");
		extraCodes = adjustStack(WORDSIZE, true); // throw away CMID
		machineCodes.addAll(extraCodes);
		machineCodes.add("pop %TR");
		machineCodes.add("ret");
		return machineCodes;
	}

	private List<String> emit_ireturn() {
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * if (method.isSynchronized()) genMonitorExit(); asm.emitPOP_Reg(T0);
		 * genEpilogue(WORDSIZE, WORDSIZE);
		 */
		machineCodes.add("pop %T0");
		List<String> extraCodes = genEpilogue(WORDSIZE, WORDSIZE);
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_lreturn() {
		List<String> machineCodes = new ArrayList<String>();
		// if (method.isSynchronized()) genMonitorExit();
		if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitPOP_Reg(T1); // low half asm.emitPOP_Reg(T0); // high
			 * half genEpilogue(2*WORDSIZE, 2*WORDSIZE);
			 */
			machineCodes.add("pop %T1");
			machineCodes.add("pop %T0");
			List<String> extraCodes = genEpilogue(2 * WORDSIZE, 2 * WORDSIZE);
			machineCodes.addAll(extraCodes);
		} else {
			/*
			 * asm.emitPOP_Reg(T0); genEpilogue(2*WORDSIZE, WORDSIZE);
			 */
			machineCodes.add("pop %T0");
			List<String> extraCodes = genEpilogue(2 * WORDSIZE, WORDSIZE);
			machineCodes.addAll(extraCodes);
		}
		return machineCodes;
	}

	private List<String> emit_freturn() {
		List<String> machineCodes = new ArrayList<String>();
		// if (method.isSynchronized()) genMonitorExit();
		if (SSE2_FULL) {
			// asm.emitMOVSS_Reg_RegInd(XMM0, SP);
			machineCodes.add("mov %XMM0 %SP");
		} else {
			// asm.emitFLD_Reg_RegInd(FP0, SP);
			machineCodes.add("fld %FP0");
		}
		// genEpilogue(WORDSIZE, 0);
		List<String> extraCodes = genEpilogue(WORDSIZE, 0);
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_dreturn() {
		List<String> machineCodes = new ArrayList<String>();
		// if (method.isSynchronized()) genMonitorExit();
		if (SSE2_FULL) {
			// asm.emitMOVLPD_Reg_RegInd(XMM0, SP);
			machineCodes.add("mov %XMM0 %SP");
		} else {
			// asm.emitFLD_Reg_RegInd_Quad(FP0, SP);
			machineCodes.add("fld %FP0");
		}
		// genEpilogue(2*WORDSIZE, 0);
		List<String> extraCodes = genEpilogue(2 * WORDSIZE, 0);
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_areturn() {
		List<String> machineCodes = new ArrayList<String>();
		// if (method.isSynchronized()) genMonitorExit();
		// asm.emitPOP_Reg(T0);
		// genEpilogue(WORDSIZE, WORDSIZE);
		machineCodes.add("pop %T0");
		List<String> extraCodes = genEpilogue(WORDSIZE, WORDSIZE);
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_return() {
		List<String> machineCodes = new ArrayList<String>();
		// if (method.isSynchronized()) genMonitorExit();
		// genEpilogue(0, 0);
		List<String> extraCodes = genEpilogue(0, 0);
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_unresolved_getstatic(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = emitDynamicLinkingSequence(fieldRef, true);
		machineCodes.addAll(extraCodes);
		if (fieldRef.getSize() <= BYTES_IN_INT) {
			// get static field - [SP--] = [T0<<0+JTOC]
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
				machineCodes.add("push %T0");
			} else {
				/*
				 * asm.emitMOV_Reg_RegOff(T0, T0, Assembler.BYTE, Magic
				 * .getTocPointer().toWord().toOffset()); asm.emitPUSH_Reg(T0);
				 */
				machineCodes.add("mov %T0 %T0");
				machineCodes.add("push %T0");
			}
		} else { // field is two words (double or long)
			/*
			 * if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() ==
			 * BYTES_IN_LONG);
			 */
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset().plus(WORDSIZE)); // get high part
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset()); // get low part
				 */
				machineCodes.add("push %T0");
				machineCodes.add("push %T0");
			} else {
				if (fieldRef.getNumberOfStackSlots() != 1) {
					extraCodes = adjustStack(-WORDSIZE, true);
					machineCodes.addAll(extraCodes);
				}
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
				machineCodes.add("push %T0");
			}
		}
		return machineCodes;
	}

	private List<String> emit_resolved_getstatic(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();
		if (fieldRef.getSize() <= BYTES_IN_INT) {
			// get static field - [SP--] = [T0<<0+JTOC]
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
				machineCodes.add("push $0");
			} else {
				/*
				 * asm.emitMOV_Reg_RegOff(T0, T0, Assembler.BYTE, Magic
				 * .getTocPointer().toWord().toOffset()); asm.emitPUSH_Reg(T0);
				 */
				machineCodes.add("mov %T0 $0");
				machineCodes.add("push %T0");
			}
		} else { // field is two words (double or long)
			/*
			 * if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() ==
			 * BYTES_IN_LONG);
			 */
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset().plus(WORDSIZE)); // get high part
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset()); // get low part
				 */
				machineCodes.add("push $0");
				machineCodes.add("push $0");
			} else {
				if (fieldRef.getNumberOfStackSlots() != 1) {
					List<String> extraCodes = adjustStack(-WORDSIZE, true);
					machineCodes.addAll(extraCodes);
				}
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
				machineCodes.add("push $0");
			}
		}
		return machineCodes;
	}

	private List<String> emit_unresolved_putstatic(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = emitDynamicLinkingSequence(fieldRef, true);
		machineCodes.addAll(extraCodes);
		if (fieldRef.getSize() <= BYTES_IN_INT) {
			// get static field - [SP--] = [T0<<0+JTOC]
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
				machineCodes.add("pop %T0");
			} else {
				/*
				 * asm.emitMOV_Reg_RegOff(T0, T0, Assembler.BYTE, Magic
				 * .getTocPointer().toWord().toOffset()); asm.emitPUSH_Reg(T0);
				 */
				machineCodes.add("pop %T1");
				machineCodes.add("mov %T0 %T1");
			}
		} else { // field is two words (double or long)
			/*
			 * if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() ==
			 * BYTES_IN_LONG);
			 */
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset().plus(WORDSIZE)); // get high part
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset()); // get low part
				 */
				machineCodes.add("pop %T0");
				machineCodes.add("pop %T0");
			} else {
				machineCodes.add("pop %T0");
				if (fieldRef.getNumberOfStackSlots() != 1) {
					extraCodes = adjustStack(WORDSIZE, true);
					machineCodes.addAll(extraCodes);
				}
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
			}
		}
		// The field may be volatile
		// asm.emitMFENCE();
		return machineCodes;
	}

	private List<String> emit_resolved_putstatic(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();
		if (fieldRef.getSize() <= BYTES_IN_INT) {
			// get static field - [SP--] = [T0<<0+JTOC]
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
				machineCodes.add("pop $0");
			} else {
				/*
				 * asm.emitMOV_Reg_RegOff(T0, T0, Assembler.BYTE, Magic
				 * .getTocPointer().toWord().toOffset()); asm.emitPUSH_Reg(T0);
				 */
				machineCodes.add("pop %T1");
				machineCodes.add("mov $0 %T1");
			}
		} else { // field is two words (double or long)
			/*
			 * if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() ==
			 * BYTES_IN_LONG);
			 */
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset().plus(WORDSIZE)); // get high part
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset()); // get low part
				 */
				machineCodes.add("pop %T0");
				machineCodes.add("pop %T0");
			} else {
				machineCodes.add("pop $0");
				if (fieldRef.getNumberOfStackSlots() != 1) {
					List<String> extraCodes = adjustStack(WORDSIZE, true);
					machineCodes.addAll(extraCodes);
				}
				/*
				 * asm.emitPUSH_RegOff(T0, Assembler.BYTE, Magic.getTocPointer()
				 * .toWord().toOffset());
				 */
			}
		}
		/*
		 * if (field.isVolatile()) { asm.emitMFENCE(); }
		 */
		return machineCodes;
	}

	private List<String> emitDynamicLinkingSequence(MemberReference ref,
			boolean couldBeZero) {
		List<String> machineCodes = new ArrayList<String>();
		if (couldBeZero) {
			if (AddrJVM_32_or_64.equals("32")) {
				machineCodes.add("mov %T0 $0");
			} else {
				// TODO:This mov inst supposed to be 64 bit, please check
				machineCodes.add("mov %T0 $0");
			}
			machineCodes.add("mov %T0 %T1");
			if (NEEDS_DYNAMIC_LINK == 0) {
				/*
				 * asm.emitTEST_Reg_Reg(reg, reg); // reg ?= NEEDS_DYNAMIC_LINK,
				 * is // field's class loaded?
				 */
				machineCodes.add("test %T0 %T1");
			} else {
				/*
				 * asm.emitCMP_Reg_Imm(reg, NEEDS_DYNAMIC_LINK); // reg ?= //
				 * NEEDS_DYNAMIC_LINK, // is field's // class loaded?
				 */
				machineCodes.add("cmp %T0 $0");
			}
			machineCodes.add("push $0");
			machineCodes.add("mov %T0 %T1");
			machineCodes.add("cal $0");
			machineCodes.add("jmp $0");
		} else {
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitMOV_Reg_Abs(reg, Magic.getTocPointer()
				 * .plus(tableOffset)); // reg is offsets table
				 */
				machineCodes.add("mov %T0 $0");
			} else {
				/*
				 * asm.emitMOV_Reg_Abs_Quad(reg, Magic.getTocPointer().plus(
				 * tableOffset)); // reg is offsets table
				 */
				machineCodes.add("mov %T0 $0");
			}
			/*
			 * asm.emitMOV_Reg_RegDisp(reg, reg, memberOffset); // reg is offset
			 * of // member
			 */
			machineCodes.add("mov %T0 %T1");
		}
		return machineCodes;
	}

	private List<String> emit_unresolved_getfield(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();
		TypeReference fieldType = fieldRef.getFieldContentsType();
		List<String> extraCodes = emitDynamicLinkingSequence(fieldRef, true);
		machineCodes.addAll(extraCodes);
		if (fieldType.isReferenceType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("push %S0");
		} else if (fieldType.isBooleanType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isByteType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isShortType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isCharType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isIntType()
				|| fieldType.isFloatType()
				|| (AddrJVM_32_or_64.equals("32") && fieldType.isWordLikeType())) {
			machineCodes.add("pop %S0");
			machineCodes.add("push %T1");
		} else {
			// 64bit load
			// asm.emitPOP_Reg(T1); // T1 is object reference
			machineCodes.add("pop %T1");
			if (AddrJVM_32_or_64.equals("32")) {
				// NB this is a 64bit copy from memory to the stack so implement
				// as a slightly optimized Intel memory copy using the FPU
				extraCodes = adjustStack(-2 * WORDSIZE, true); // adjust stack
				// down to hold
				// 64bit value
				machineCodes.addAll(extraCodes);
				if (SSE2_BASE) {
					/*
					 * asm.emitMOVQ_Reg_RegIdx(XMM0, T1, T0, Assembler.BYTE,
					 * NO_SLOT); // XMM0 is field value
					 */
					machineCodes.add("mov %XMM0 %T1");
					machineCodes.add("mov %SP %XMM0");
					// asm.emitMOVQ_RegInd_Reg(SP, XMM0); // place value on
					// stack
				} else {
					/*
					 * asm.emitFLD_Reg_RegIdx_Quad(FP0, T1, T0, Assembler.BYTE,
					 * NO_SLOT); // FP0 is field value
					 * asm.emitFSTP_RegInd_Reg_Quad(SP, FP0); // place value on
					 * // stack
					 */
					machineCodes.add("fld %FP0");
					machineCodes.add("fstp %SP");
				}
			} else {
				if (!fieldType.isWordLikeType()) {
					extraCodes = adjustStack(-WORDSIZE, true); // add empty slot
					machineCodes.addAll(extraCodes);
				}
				// asm.emitPUSH_RegIdx(T1, T0, Assembler.BYTE, NO_SLOT); //
				// place
				// value
				// on
				// stack
				machineCodes.add("push %T1");
			}
		}
		return machineCodes;
	}

	private List<String> emit_resolved_getfield(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		TypeReference fieldType = fieldRef.getFieldContentsType();
		if (fieldType.isReferenceType()) {
			machineCodes.add("pop %T0");
			machineCodes.add("push %T0");
		} else if (fieldType.isBooleanType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T0");
		} else if (fieldType.isByteType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isShortType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isCharType()) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else if (fieldType.isIntType()
				|| fieldType.isFloatType()
				|| (AddrJVM_32_or_64.equals("32") && fieldType.isWordLikeType())) {
			machineCodes.add("pop %S0");
			machineCodes.add("mov %S0 %T0");
			machineCodes.add("push %T1");
		} else {
			// 64bit load
			// asm.emitPOP_Reg(T1); // T1 is object reference
			machineCodes.add("pop %T0");
			if (AddrJVM_32_or_64.equals("32")) {
				// NB this is a 64bit copy from memory to the stack so implement
				// as a slightly optimized Intel memory copy using the FPU
				extraCodes = adjustStack(-2 * WORDSIZE, true); // adjust stack
				// down to hold
				// 64bit value
				machineCodes.addAll(extraCodes);
				if (SSE2_BASE) {
					/*
					 * asm.emitMOVQ_Reg_RegIdx(XMM0, T1, T0, Assembler.BYTE,
					 * NO_SLOT); // XMM0 is field value
					 */
					machineCodes.add("mov %XMM0 %T1");
					machineCodes.add("mov %SP %XMM0");
					// asm.emitMOVQ_RegInd_Reg(SP, XMM0); // place value on
					// stack
				} else {
					/*
					 * asm.emitFLD_Reg_RegIdx_Quad(FP0, T1, T0, Assembler.BYTE,
					 * NO_SLOT); // FP0 is field value
					 * asm.emitFSTP_RegInd_Reg_Quad(SP, FP0); // place value on
					 * // stack
					 */
					machineCodes.add("fld %FP0");
					machineCodes.add("fstp %SP");
				}
			} else {
				if (!fieldType.isWordLikeType()) {
					extraCodes = adjustStack(-WORDSIZE, true); // add empty slot
					machineCodes.addAll(extraCodes);
				}
				// asm.emitPUSH_RegIdx(T1, T0, Assembler.BYTE, NO_SLOT); //
				// place
				// value
				// on
				// stack
				machineCodes.add("push %T0");
			}
		}
		return machineCodes;
	}

	private List<String> emit_unresolved_putfield(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_resolved_putfield(FieldReference fieldRef) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	protected List<String> genParameterRegisterLoad(MethodReference method,
			boolean hasThisParam) {
		List<String> machineCodes = new ArrayList<String>();
		int max = NUM_PARAMETER_GPRS + NUM_PARAMETER_FPRS;
		if (max == 0)
			return machineCodes; // quit looking when all registers are full
		int gpr = 0; // number of general purpose registers filled
		int fpr = 0; // number of floating point registers filled
		// GPR T = T0; // next GPR to get a parameter
		int params = method.getParameterWords() + (hasThisParam ? 1 : 0);
		// Offset offset = Offset.fromIntSignExtend((params - 1) <<
		// LG_WORDSIZE); // stack
		// offset
		// of
		// first
		// parameter
		// word
		if (hasThisParam) {
			if (gpr < NUM_PARAMETER_GPRS) {
				// stackMoveHelper(T, offset);
				machineCodes.add("mov %T %T");
				// T = T1; // at most 2 parameters can be passed in general
				// purpose
				// registers
				gpr++;
				max--;
			}
			// offset = offset.minus(WORDSIZE);
		}
		for (TypeReference type : method.getParameterTypes()) {
			if (max == 0)
				return machineCodes; // quit looking when all registers are full
			TypeReference t = type;
			if (t.isLongType()) {
				if (gpr < NUM_PARAMETER_GPRS) {
					if (WORDSIZE == 4) {
						// stackMoveHelper(T, offset); // lo register := hi mem
						// (==
						// hi order word)
						// T = T1; // at most 2 parameters can be passed in
						// general
						// purpose registers
						machineCodes.add("mov %T %T");
						gpr++;
						max--;
						if (gpr < NUM_PARAMETER_GPRS) {
							// stackMoveHelper(T, offset.minus(WORDSIZE)); // hi
							// register
							// := lo
							// mem
							// (==
							// lo
							// order
							// word)
							gpr++;
							max--;
							machineCodes.add("mov %T %T");
						}
					} else {
						// initially offset will point at junk word, move down
						// and over
						// stackMoveHelper(T, offset.minus(WORDSIZE));
						// T = T1; // at most 2 parameters can be passed in
						// general
						// purpose registers
						machineCodes.add("mov %T %T");
						gpr++;
						max--;
					}
				}
				// offset = offset.minus(2 * WORDSIZE);
			} else if (t.isFloatType()) {
				if (fpr < NUM_PARAMETER_FPRS) {
					if (SSE2_FULL) {
						// asm.emitMOVSS_Reg_RegDisp(XMM.lookup(fpr), SP,
						// offset);
						machineCodes.add("mov %XMM %SP");
					} else {
						// asm.emitFLD_Reg_RegDisp(FP0, SP, offset);
						machineCodes.add("fld %FP0");
					}
					fpr++;
					max--;
				}
				// offset = offset.minus(WORDSIZE);
			} else if (t.isDoubleType()) {
				if (fpr < NUM_PARAMETER_FPRS) {
					if (SSE2_FULL) {
						// asm.emitMOVLPD_Reg_RegDisp(XMM.lookup(fpr), SP,
						// offset
						// .minus(WORDSIZE));
						machineCodes.add("mov %XMM %SP");
					} else {
						/*
						 * asm.emitFLD_Reg_RegDisp_Quad(FP0, SP, offset
						 * .minus(WORDSIZE));
						 */
						machineCodes.add("mov %FP0 %SP");
					}
					fpr++;
					max--;
				}
				// offset = offset.minus(2 * WORDSIZE);
			} else if (t.isReferenceType() || t.isWordLikeType()) {
				if (gpr < NUM_PARAMETER_GPRS) {
					// stackMoveHelper(T, offset);
					// T = T1; // at most 2 parameters can be passed in general
					// purpose registers
					machineCodes.add("mov %T %T");
					gpr++;
					max--;
				}
				// offset = offset.minus(WORDSIZE);
			} else { // t is object, int, short, char, byte, or boolean
				if (gpr < NUM_PARAMETER_GPRS) {
					machineCodes.add("mov %T %SP");
					// T = T1; // at most 2 parameters can be passed in general
					// purpose registers
					gpr++;
					max--;
				}
				// offset = offset.minus(WORDSIZE);
			}
		}
		/*
		 * if (VM.VerifyAssertions)
		 * VM._assert(offset.EQ(Offset.fromIntSignExtend(-WORDSIZE)));
		 */
		return machineCodes;
	}

	private List<String> genResultRegisterUnload(MethodReference m) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		TypeReference t = m.getReturnType();
		if (t.isVoidType()) {
			// nothing to do
		} else if (t.isLongType()) {
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitPUSH_Reg(T0); // high half asm.emitPUSH_Reg(T1); //
				 * low half
				 */
				machineCodes.add("push %T0");
				machineCodes.add("push %T1");
			} else {
				extraCodes = adjustStack(-WORDSIZE, true);
				machineCodes.addAll(extraCodes);
				// asm.emitPUSH_Reg(T0); // long value
				machineCodes.add("push %T0");
			}
		} else if (t.isFloatType()) {
			extraCodes = adjustStack(-WORDSIZE, true);
			machineCodes.addAll(extraCodes);
			if (SSE2_FULL) {
				// asm.emitMOVSS_RegInd_Reg(SP, XMM0);
				machineCodes.add("mov %SP %XMM0");
			} else {
				// asm.emitFSTP_RegInd_Reg(SP, FP0);
				machineCodes.add("fstp %SP");
			}
		} else if (t.isDoubleType()) {
			extraCodes = adjustStack(-2 * WORDSIZE, true);
			machineCodes.addAll(extraCodes);
			if (SSE2_FULL) {
				// asm.emitMOVLPD_RegInd_Reg(SP, XMM0);
				machineCodes.add("mov %SP %XMM0");
			} else {
				// asm.emitFSTP_RegInd_Reg_Quad(SP, FP0);
				machineCodes.add("fstp %SP");
			}
		} else { // t is object, int, short, char, byte, or boolean
			// asm.emitPUSH_Reg(T0);
			machineCodes.add("push %T0");
		}
		return machineCodes;
	}

	private List<String> emit_unresolved_invokevirtual(MethodReference methodRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = emitDynamicLinkingSequence(methodRef, true);
		machineCodes.addAll(extraCodes);
		// stackMoveHelper(T1, objectOffset); // T1 has "this" parameter
		machineCodes.add("mov %T0 %T1");
		// baselineEmitLoadTIB(asm, S0, T1); // S0 has TIB
		if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitMOV_Reg_RegDisp(GPR.lookup(dest), GPR.lookup(object),
			 * tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		} else {
			/*
			 * asm.emitMOV_Reg_RegDisp_Quad(GPR.lookup(dest),
			 * GPR.lookup(object), tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		}
		// asm.emitMOV_Reg_RegIdx(S0, S0, T0, Assembler.BYTE, NO_SLOT); // S0
		// has address of virtual method
		machineCodes.add("mov %S0 %T0");
		extraCodes = genParameterRegisterLoad(methodRef, true);
		machineCodes.addAll(extraCodes);
		// asm.emitCALL_Reg(S0); // call virtual method
		machineCodes.add("cal %S0");
		extraCodes = genResultRegisterUnload(methodRef); // push return value,
		// if any
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_resolved_invokevirtual(MethodReference methodRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		//int methodRefparameterWords = methodRef.getParameterWords() + 1; // +1 for "this" parameter
	    /*Offset methodRefOffset = methodRef.peekResolvedMethod().getOffset();
	    Offset objectOffset =
	      Offset.fromIntZeroExtend(methodRefparameterWords << LG_WORDSIZE).minus(WORDSIZE); // object offset into stack*/
	    //stackMoveHelper(T1, objectOffset);                               // T1 has "this" parameter
	    machineCodes.add("mov %T1 %T1");     
	    machineCodes.add("mov %S0 %T1");                                // S0 has TIB
	    extraCodes = genParameterRegisterLoad(methodRef, true);
	    machineCodes.addAll(extraCodes);
	    // asm.emitCALL_RegDisp(S0, methodRefOffset);                       // call virtual method
	    machineCodes.add("cal %S0");     
	    extraCodes = genResultRegisterUnload(methodRef);                              // push return value, if any
	    machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_resolved_invokespecial(MethodReference methodRef,
			Object target) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		if (false /*target.isObjectInitializer()*/) {
			extraCodes = genParameterRegisterLoad(methodRef, true);
			machineCodes.addAll(extraCodes);
			//asm.emitCALL_Abs(Magic.getTocPointer().plus(target.getOffset()));
			machineCodes.add("cal $0");
			extraCodes = genResultRegisterUnload(methodRef /*target.getMemberRef().asMethodReference()*/);
			machineCodes.addAll(extraCodes);
		} else {
			/*if (VM.VerifyAssertions)
				VM._assert(!target.isStatic());*/
			// invoke via class's tib slot
			//Offset methodRefOffset = target.getOffset();
			if (AddrJVM_32_or_64.equals("32")) {
				/*asm.emitMOV_Reg_Abs(S0, Magic.getTocPointer().plus(
						target.getDeclaringClass().getTibOffset()));*/
				machineCodes.add("mov %S0 $0");
			} else {
				/*asm.emitMOV_Reg_Abs_Quad(S0, Magic.getTocPointer().plus(
						target.getDeclaringClass().getTibOffset()));*/
				machineCodes.add("mov %S0 $0");
			}
			extraCodes = genParameterRegisterLoad(methodRef, true);
			machineCodes.addAll(extraCodes);
			// asm.emitCALL_RegDisp(S0, methodRefOffset);
			machineCodes.add("cal %S0");
			extraCodes = genResultRegisterUnload(methodRef);
			machineCodes.addAll(extraCodes);
		}
		return machineCodes;
	}

	private List<String> emit_unresolved_invokespecial(MethodReference methodRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		extraCodes = emitDynamicLinkingSequence(methodRef, true);
		machineCodes.addAll(extraCodes);
	    extraCodes = genParameterRegisterLoad(methodRef, true);
	    machineCodes.addAll(extraCodes);
	    // asm.emitCALL_RegDisp(S0, Magic.getTocPointer().toWord().toOffset());
	    machineCodes.add("cal %S0");
	    extraCodes = genResultRegisterUnload(methodRef);
	    machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_unresolved_invokestatic(MethodReference methodRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		extraCodes = emitDynamicLinkingSequence(methodRef, true);
		machineCodes.addAll(extraCodes);
	    extraCodes = genParameterRegisterLoad(methodRef, false);
	    machineCodes.addAll(extraCodes);
	    //asm.emitCALL_RegDisp(S0, Magic.getTocPointer().toWord().toOffset());
	    machineCodes.add("cal %S0");
	    extraCodes = genResultRegisterUnload(methodRef);
	    machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_resolved_invokestatic(MethodReference methodRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		//Offset methodOffset = methodRef.peekResolvedMethod().getOffset();
	    extraCodes = genParameterRegisterLoad(methodRef, false);
	    machineCodes.addAll(extraCodes);
	    //asm.emitCALL_Abs(Magic.getTocPointer().plus(methodOffset));
	    machineCodes.add("cal $0");
	    extraCodes = genResultRegisterUnload(methodRef);
	    machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_invokeinterface(MethodReference methodRef) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		final int count = methodRef.getParameterWords() + 1; // +1 for "this"
		// parameter

		RVMMethod resolvedMethod = null;
		resolvedMethod = methodRef.peekInterfaceMethod();

		// (1) Emit dynamic type checking sequence if required to do so inline.
		// TODO: Check, VM.BuildForIMTInterfaceInvocation what is this?
		if (false) {
			if (methodRef.isMiranda()) {
				// TODO: It's not entirely clear that we can just assume that
				// the class actually implements the interface.
				// However, we don't know what interface we need to be checking
				// so there doesn't appear to be much else we can do here.
			} else {
				if (resolvedMethod == null) {
					// Can't successfully resolve it at compile time.
					// Call uncommon case typechecking routine to do the right
					// thing when this code actually executes.
					// T1 = "this" object
					/*
					 * stackMoveHelper(T1, Offset .fromIntZeroExtend((count - 1)
					 * << LG_WORDSIZE));
					 */
					machineCodes.add("mov %T1 %T1");
					// asm.emitPUSH_Imm(methodRef.getId()); // push dict id of
					// target
					// asm.emitPUSH_Reg(T1); // push "this"
					machineCodes.add("push $0");
					machineCodes.add("push %T1");
					// genParameterRegisterLoad(asm, 2); // pass 2 parameter
					// word
					machineCodes.add("mov %T1 %T1");
					// check that "this" class implements the interface
					/*
					 * asm .emitCALL_Abs(Magic .getTocPointer() .plus(
					 * Entrypoints.unresolvedInvokeinterfaceImplementsTestMethod
					 * .getOffset()));
					 */
					machineCodes.add("cal $0");
				} else {
					RVMClass interfaceClass = resolvedMethod
							.getDeclaringClass();
					int interfaceIndex = interfaceClass.getDoesImplementIndex();
					int interfaceMask = interfaceClass
							.getDoesImplementBitMask();
					// T1 = "this" object
					/*
					 * stackMoveHelper(T1, Offset .fromIntZeroExtend((count - 1)
					 * << LG_WORDSIZE));
					 */
					machineCodes.add("mov %T1 %T1");
					// baselineEmitLoadTIB(asm, S0, T1); // S0 = tib of "this"
					// object
					if (AddrJVM_32_or_64.equals("32")) {
						/*
						 * asm.emitMOV_Reg_RegDisp(GPR.lookup(dest),
						 * GPR.lookup(object), tibOffset);
						 */
						machineCodes.add("mov %T0 %T1");
					} else {
						/*
						 * asm.emitMOV_Reg_RegDisp_Quad(GPR.lookup(dest),
						 * GPR.lookup(object), tibOffset);
						 */
						machineCodes.add("mov %T0 %T1");
					}
					if (AddrJVM_32_or_64.equals("32")) {
						/*
						 * asm .emitMOV_Reg_RegDisp( S0, S0, Offset
						 * .fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX <<
						 * LG_WORDSIZE)); // implements // bit // vector
						 */
						machineCodes.add("mov %S0, %S0");
					} else {
						/*
						 * asm .emitMOV_Reg_RegDisp_Quad( S0, S0, Offset
						 * .fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX <<
						 * LG_WORDSIZE)); // implements // bit // vector
						 */
						machineCodes.add("mov %S0, %S0");
					}

					if (/*
						 * DynamicTypeCheck.MIN_DOES_IMPLEMENT_SIZE <=
						 * interfaceIndex
						 */true) {
						// must do arraybounds check of implements bit vector
						/*
						 * if (JavaHeaderConstants.ARRAY_LENGTH_BYTES == 4) {
						 * asm.emitCMP_RegDisp_Imm(S0, ObjectModel
						 * .getArrayLengthOffset(), interfaceIndex); } else {
						 * asm.emitCMP_RegDisp_Imm_Quad(S0, ObjectModel
						 * .getArrayLengthOffset(), interfaceIndex); }
						 */
						machineCodes.add("cmp %S0 $0");
						// asm.emitBranchLikelyNextInstruction();
						// ForwardReference fr = asm.forwardJcc(Assembler.LGT);
						// asm.emitINT_Imm(RuntimeEntrypoints.TRAP_MUST_IMPLEMENT
						// + RVM_TRAP_BASE);
						// fr.resolve(asm);
					}

					// Test the appropriate bit and if set, branch around
					// another trap imm
					if (interfaceIndex == 0) {
						// asm.emitTEST_RegInd_Imm(S0, interfaceMask);
						machineCodes.add("test %S0, $0");
					} else {
						/*
						 * asm .emitTEST_RegDisp_Imm( S0, Offset
						 * .fromIntZeroExtend(interfaceIndex <<
						 * LOG_BYTES_IN_INT), interfaceMask);
						 */
						machineCodes.add("test %S0, $0");
					}
					/*
					 * asm.emitBranchLikelyNextInstruction(); ForwardReference
					 * fr = asm.forwardJcc(Assembler.NE);
					 * asm.emitINT_Imm(RuntimeEntrypoints.TRAP_MUST_IMPLEMENT +
					 * RVM_TRAP_BASE); fr.resolve(asm);
					 */
				}
			}
		}

		// (2) Emit interface invocation sequence.
		if (true /* VM.BuildForIMTInterfaceInvocation */) {
			/*
			 * InterfaceMethodSignature sig = InterfaceMethodSignature
			 * .findOrCreate(methodRef);
			 * 
			 * // squirrel away signature ID
			 * ThreadLocalState.emitMoveImmToField(asm,
			 * ArchEntrypoints.hiddenSignatureIdField.getOffset(), sig
			 * .getId()); // T1 = "this" object /*stackMoveHelper(T1, Offset
			 * .fromIntZeroExtend((count - 1) << LG_WORDSIZE));
			 */
			machineCodes.add("mov %T1, %T1");
			// baselineEmitLoadTIB(asm, S0, T1);
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm.emitMOV_Reg_RegDisp(GPR.lookup(dest), GPR.lookup(object),
				 * tibOffset);
				 */
				machineCodes.add("mov %S0 %T1");
			} else {
				/*
				 * asm.emitMOV_Reg_RegDisp_Quad(GPR.lookup(dest),
				 * GPR.lookup(object), tibOffset);
				 */
				machineCodes.add("mov %S0 %T1");
			}
			// Load the IMT Base into S0
			if (AddrJVM_32_or_64.equals("32")) {
				/*
				 * asm .emitMOV_Reg_RegDisp( S0, S0, Offset
				 * .fromIntZeroExtend(TIB_INTERFACE_DISPATCH_TABLE_INDEX <<
				 * LG_WORDSIZE));
				 */
				machineCodes.add("mov %S0 %S0");
			} else {
				/*
				 * asm .emitMOV_Reg_RegDisp_Quad( S0, S0, Offset
				 * .fromIntZeroExtend(TIB_INTERFACE_DISPATCH_TABLE_INDEX <<
				 * LG_WORDSIZE));
				 */
				machineCodes.add("mov %S0 %S0");
			}
			extraCodes = genParameterRegisterLoad(methodRef, true);
			machineCodes.addAll(extraCodes);
			// asm.emitCALL_RegDisp(S0, sig.getIMTOffset()); // the interface
			// call
		} else {
			int itableIndex = -1;
			/*
			 * if (VM.BuildForITableInterfaceInvocation && resolvedMethod !=
			 * null) { // get the index of the method in the Itable itableIndex
			 * = InterfaceInvocation.getITableIndex(resolvedMethod
			 * .getDeclaringClass(), methodRef.getName(), methodRef
			 * .getDescriptor()); }
			 */
			if (itableIndex == -1) {
				// itable index is not known at compile-time.
				// call "invokeInterface" to resolve object + method id into
				// method address
				int methodRefId = methodRef.getId();
				// "this" parameter is obj
				if (count == 1) {
					// asm.emitPUSH_RegInd(SP);
					machineCodes.add("push %SP");
				} else {
					/*
					 * asm.emitPUSH_RegDisp(SP, Offset .fromIntZeroExtend((count
					 * - 1) << LG_WORDSIZE));
					 */
					machineCodes.add("push %SP");
				}
				// asm.emitPUSH_Imm(methodRefId); // id of method to call
				machineCodes.add("push $0");
				// genParameterRegisterLoad(asm, 2); // pass 2 parameter words
				machineCodes.add("mov %T1 %T1");
				// invokeinterface(obj, id) returns address to call
				/*
				 * asm.emitCALL_Abs(Magic.getTocPointer().plus(
				 * Entrypoints.invokeInterfaceMethod.getOffset()));
				 */
				machineCodes.add("cal $0");
				if (AddrJVM_32_or_64.equals("32")) {
					// asm.emitMOV_Reg_Reg(S0, T0); // S0 has address of method
					machineCodes.add("mov %S0 %T0");
				} else {
					// asm.emitMOV_Reg_Reg_Quad(S0, T0); // S0 has address of
					// method
					machineCodes.add("mov %S0 %T0");
				}
				extraCodes = genParameterRegisterLoad(methodRef, true);
				machineCodes.addAll(extraCodes);
				// asm.emitCALL_Reg(S0); // the interface method (its parameters
				// are on stack)
				machineCodes.add("cal %S0");
			} else {
				// itable index is known at compile-time.
				// call "findITable" to resolve object + interface id into
				// itable address
				// T0 = "this" object
				/*
				 * stackMoveHelper(T0, Offset .fromIntZeroExtend((count - 1) <<
				 * LG_WORDSIZE)); baselineEmitLoadTIB(asm, S0, T0);
				 * asm.emitPUSH_Reg(S0);
				 * asm.emitPUSH_Imm(resolvedMethod.getDeclaringClass()
				 * .getInterfaceId()); // interface id
				 * genParameterRegisterLoad(asm, 2); // pass 2 parameter words
				 * asm.emitCALL_Abs(Magic.getTocPointer().plus(
				 * Entrypoints.findItableMethod.getOffset())); //
				 * findItableOffset(tib, // id) // returns // iTable
				 */
				machineCodes.add("mov %S0 %T0");
				machineCodes.add("mov %S0 %T0");
				machineCodes.add("push %S0");
				machineCodes.add("push $0");
				machineCodes.add("mov %S0 %T0");
				machineCodes.add("cal $0");
				if (AddrJVM_32_or_64.equals("32")) {
					// asm.emitMOV_Reg_Reg(S0, T0); // S0 has iTable
					machineCodes.add("mov %S0 %T0");
				} else {
					// asm.emitMOV_Reg_Reg_Quad(S0, T0); // S0 has iTable
					machineCodes.add("mov %S0 %T0");
				}
				extraCodes = genParameterRegisterLoad(methodRef, true);
				machineCodes.addAll(extraCodes);
				// the interface call
				/*
				 * asm.emitCALL_RegDisp(S0, Offset
				 * .fromIntZeroExtend(itableIndex << LG_WORDSIZE));
				 */
				machineCodes.add("ca; %S0");
			}
		}
		extraCodes = genResultRegisterUnload(methodRef);
		machineCodes.addAll(extraCodes);
		return machineCodes;
	}

	private List<String> emit_resolved_new(RVMClass typeRef) {
		List<String> machineCodes = new ArrayList<String>();

		/*
		 * asm.emitPUSH_Imm(instanceSize);
		 * asm.emitPUSH_Abs(Magic.getTocPointer().plus(tibOffset)); // put tib
		 * on stack asm.emitPUSH_Imm(typeRef.hasFinalizer() ? 1 : 0); // does
		 * the class have a finalizer? asm.emitPUSH_Imm(whichAllocator);
		 * asm.emitPUSH_Imm(align); asm.emitPUSH_Imm(offset);
		 * asm.emitPUSH_Imm(site); genParameterRegisterLoad(asm, 7); // pass 7
		 * parameter words
		 * asm.emitCALL_Abs(Magic.getTocPointer().plus(Entrypoints
		 * .resolvedNewScalarMethod.getOffset())); asm.emitPUSH_Reg(T0);
		 */
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("mov %T0 %SP");
		machineCodes.add("cal $0");
		machineCodes.add("push $0");

		return machineCodes;
	}

	private List<String> emit_unresolved_new(TypeReference typeRef) {
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * asm.emitPUSH_Imm(typeRef.getId()); asm.emitPUSH_Imm(site); // site
		 * genParameterRegisterLoad(asm, 2); // pass 2 parameter words
		 * asm.emitCALL_Abs
		 * (Magic.getTocPointer().plus(Entrypoints.unresolvedNewScalarMethod
		 * .getOffset())); asm.emitPUSH_Reg(T0);
		 */
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("mov %T0 %SP");
		machineCodes.add("cal $0");
		machineCodes.add("push $0");
		return machineCodes;
	}

	private List<String> emit_resolved_newarray(RVMArray array) {
		List<String> machineCodes = new ArrayList<String>();
		/*int width = array.getLogElementSize();
	    Offset tibOffset = array.getTibOffset();
	    int headerSize = ObjectModel.computeHeaderSize(array);
	    int whichAllocator = MemoryManager.pickAllocator(array, method);
	    int site = MemoryManager.getAllocationSite(true);
	    int align = ObjectModel.getAlignment(array);
	    int offset = ObjectModel.getOffsetForAlignment(array, false);
	    // count is already on stack- nothing required
	    asm.emitPUSH_Imm(width);                 // logElementSize
	    asm.emitPUSH_Imm(headerSize);            // headerSize
	    asm.emitPUSH_Abs(Magic.getTocPointer().plus(tibOffset));   // tib
	    asm.emitPUSH_Imm(whichAllocator);        // allocator
	    asm.emitPUSH_Imm(align);
	    asm.emitPUSH_Imm(offset);
	    asm.emitPUSH_Imm(site);
	    genParameterRegisterLoad(asm, 8);        // pass 8 parameter words
	    asm.emitCALL_Abs(Magic.getTocPointer().plus(Entrypoints.resolvedNewArrayMethod.getOffset()));
	    asm.emitPUSH_Reg(T0);*/
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
		machineCodes.add("push $0");
	    machineCodes.add("mov %T0 %SP");
	    machineCodes.add("cal $0");
	    machineCodes.add("push %T0");
		
		return machineCodes;
	}

	private List<String> emit_unresolved_newarray(TypeReference tRef) {
		List<String> machineCodes = new ArrayList<String>();
		//int site = MemoryManager.getAllocationSite(true);
	    // count is already on stack- nothing required
	    /*asm.emitPUSH_Imm(tRef.getId());
	    asm.emitPUSH_Imm(site);           // site
	    genParameterRegisterLoad(asm, 3); // pass 3 parameter words
	    asm.emitCALL_Abs(Magic.getTocPointer().plus(Entrypoints.unresolvedNewArrayMethod.getOffset()));
	    asm.emitPUSH_Reg(T0);*/
		machineCodes.add("push $0");
		machineCodes.add("push $0");
	    machineCodes.add("mov %T0 %SP");
	    machineCodes.add("cal $0");
	    machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_multianewarray(TypeReference typeRef,
			int dimensions) {
		List<String> machineCodes = new ArrayList<String>();
		List<String> extraCodes = new ArrayList<String>();
		// TODO: implement direct call to RuntimeEntrypoints.buildTwoDimensionalArray
	    // Calculate the offset from FP on entry to newarray:
	    //      1 word for each parameter, plus 1 for return address on
	    //      stack and 1 for code technique in Linker
	    final int PARAMETERS = 4;
	    final int OFFSET_WORDS = PARAMETERS + 2;

	    // setup parameters for newarrayarray routine
	    /*asm.emitPUSH_Imm(method.getId());           // caller
	    asm.emitPUSH_Imm(dimensions);               // dimension of arrays
	    asm.emitPUSH_Imm(typeRef.getId());          // type of array elements
	    asm.emitPUSH_Imm((dimensions + OFFSET_WORDS) << LG_WORDSIZE);  // offset to dimensions from FP on entry to newarray

	    genParameterRegisterLoad(asm, PARAMETERS);
	    asm.emitCALL_Abs(Magic.getTocPointer().plus(ArchEntrypoints.newArrayArrayMethod.getOffset()));
	    adjustStack(dimensions * WORDSIZE, true);   // clear stack of dimensions
	    asm.emitPUSH_Reg(T0); */
	    machineCodes.add("push $0");
	    machineCodes.add("push $0");
	    machineCodes.add("push $0");
	    machineCodes.add("push $0");
	    
	    machineCodes.add("mov %T0 %SP");
	    
	    machineCodes.add("cal $0");
	    extraCodes = adjustStack(dimensions * WORDSIZE, true);
	    machineCodes.addAll(extraCodes);
	    machineCodes.add("push %T0");
		return machineCodes;
	}

	private List<String> emit_arraylength() {
		List<String> machineCodes = new ArrayList<String>();
		//asm.emitPOP_Reg(T0);                // T0 is array reference
		machineCodes.add("pop %T0");
	    if (ARRAY_LENGTH_BYTES == 4) {
	      if (AddrJVM_32_or_64.equals("32")) {
	        //asm.emitPUSH_RegDisp(T0, ObjectModel.getArrayLengthOffset());
	    	  machineCodes.add("push %T0");
	      } else {
	        //asm.emitMOV_Reg_RegDisp(T0, T0, ObjectModel.getArrayLengthOffset());
	        //asm.emitPUSH_Reg(T0);
	        machineCodes.add("mov %T0 %T0");
	        machineCodes.add("push %T0");
	      }
	    } else {
	    	//asm.emitPUSH_RegDisp(T0, ObjectModel.getArrayLengthOffset());
	    	machineCodes.add("push %T0");
	    }
		return machineCodes;
	}

	private List<String> emit_athrow() {
		List<String> machineCodes = new ArrayList<String>();
		/* genParameterRegisterLoad(asm, 1);          // pass 1 parameter word
	    asm.emitCALL_Abs(Magic.getTocPointer().plus(Entrypoints.athrowMethod.getOffset()));*/
		machineCodes.add("mov %T0 %SP");
		machineCodes.add("cal $0");
		return machineCodes;
	}

	private List<String> emit_checkcast(TypeReference typeRef) {
		List<String> machineCodes = new ArrayList<String>();
		/*
		 * asm.emitPUSH_RegInd(SP); // duplicate the object ref on the stack
		 * asm.emitPUSH_Imm(typeRef.getId()); // TypeReference id.
		 * genParameterRegisterLoad(asm, 2); // pass 2 parameter words
		 * asm.emitCALL_Abs
		 * (Magic.getTocPointer().plus(Entrypoints.checkcastMethod
		 * .getOffset())); // checkcast(obj, type reference id);
		 */
		machineCodes.add("push %SP");
		machineCodes.add("push $0");
		machineCodes.add("mov %T0 %SP");
		machineCodes.add("cal $0");
		return machineCodes;
	}

	private List<String> emit_checkcast_resolvedInterface(RVMClass type) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_checkcast_resolvedClass(RVMClass type) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_checkcast_final(RVMType type) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_instanceof(TypeReference typeRef) {
		List<String> machineCodes = new ArrayList<String>();

		/*
		 * asm.emitPUSH_Imm(typeRef.getId()); genParameterRegisterLoad(asm, 2);
		 * // pass 2 parameter words
		 * asm.emitCALL_Abs(Magic.getTocPointer().plus(
		 * Entrypoints.instanceOfMethod.getOffset())); asm.emitPUSH_Reg(T0);
		 */
		machineCodes.add("push $0");
		machineCodes.add("mov %T0 %SP");
		machineCodes.add("cal $0");
		machineCodes.add("push %T0");

		return machineCodes;
	}

	private List<String> emit_instanceof_resolvedInterface(RVMClass type) {
		List<String> machineCodes = new ArrayList<String>();
		
	    int interfaceIndex = type.getDoesImplementIndex();
	    int interfaceMask = type.getDoesImplementBitMask();

	    //asm.emitPOP_Reg(S0);                 // load object from stack
	    machineCodes.add("pop %S0");
	    if (AddrJVM_32_or_64.equals("32")) {
	    	machineCodes.add("test %S0 %S0");//asm.emitTEST_Reg_Reg(S0, S0);      // test for null
	    } else {
	    	machineCodes.add("test %S0 %S0");//asm.emitTEST_Reg_Reg_Quad(S0, S0); // test for null
	    }
	    //ForwardReference isNull = asm.forwardJcc(Assembler.EQ);

	    //baselineEmitLoadTIB(asm, S0, S0);    // S0 = TIB of object
	    if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitMOV_Reg_RegDisp(GPR.lookup(dest), GPR.lookup(object),
			 * tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		} else {
			/*
			 * asm.emitMOV_Reg_RegDisp_Quad(GPR.lookup(dest),
			 * GPR.lookup(object), tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		}
	    // S0 = implements bit vector
	    if (AddrJVM_32_or_64.equals("32")) {
	    	machineCodes.add("mov %S0 %S0");//asm.emitMOV_Reg_RegDisp(S0, S0, Offset.fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX << LG_WORDSIZE));
	    } else {
	    	machineCodes.add("mov %S0 %S0");//asm.emitMOV_Reg_RegDisp_Quad(S0, S0, Offset.fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX << LG_WORDSIZE));
	    }
	    //ForwardReference outOfBounds = null;
	    /*if (DynamicTypeCheck.MIN_DOES_IMPLEMENT_SIZE <= interfaceIndex) {
	      // must do arraybounds check of implements bit vector
	      if (JavaHeaderConstants.ARRAY_LENGTH_BYTES == 4) {
	        asm.emitCMP_RegDisp_Imm(S0, ObjectModel.getArrayLengthOffset(), interfaceIndex);
	      } else {
	        asm.emitCMP_RegDisp_Imm_Quad(S0, ObjectModel.getArrayLengthOffset(), interfaceIndex);
	      }
	      outOfBounds = asm.forwardJcc(Assembler.LLE);
	    }*/
	    machineCodes.add("cmp %S0 $0");

	    // Test the implements bit and push true if it is set
	    machineCodes.add("test %S0 $0");
	    //asm.emitTEST_RegDisp_Imm(S0, Offset.fromIntZeroExtend(interfaceIndex << LOG_BYTES_IN_INT), interfaceMask);
	    //ForwardReference notMatched = asm.forwardJcc(Assembler.EQ);
	    machineCodes.add("push $0");
	    //asm.emitPUSH_Imm(1);
	    //ForwardReference done = asm.forwardJMP();

	    // push false
	    /*isNull.resolve(asm);
	    if (outOfBounds != null) outOfBounds.resolve(asm);
	    notMatched.resolve(asm);
	    asm.emitPUSH_Imm(0);

	    done.resolve(asm);
		*/
	    machineCodes.add("push $0");
	    
		return machineCodes;
	}

	private List<String> emit_instanceof_resolvedClass(RVMClass type) {
		List<String> machineCodes = new ArrayList<String>();
	    /*
	    asm.emitPOP_Reg(S0);                 // load object from stack
	    if (VM.BuildFor32Addr) {
	      asm.emitTEST_Reg_Reg(S0, S0);      // test for null
	    } else {
	      asm.emitTEST_Reg_Reg_Quad(S0, S0); // test for null
	    }
	    ForwardReference isNull = asm.forwardJcc(Assembler.EQ);

	    // get superclass display from object's TIB
	    baselineEmitLoadTIB(asm, S0, S0);
	    if (VM.BuildFor32Addr) {
	      asm.emitMOV_Reg_RegDisp(S0, S0, Offset.fromIntZeroExtend(TIB_SUPERCLASS_IDS_INDEX << LG_WORDSIZE));
	    } else {
	      asm.emitMOV_Reg_RegDisp_Quad(S0, S0, Offset.fromIntZeroExtend(TIB_SUPERCLASS_IDS_INDEX << LG_WORDSIZE));
	    }*/
	    machineCodes.add("pop %S0");
	    if (AddrJVM_32_or_64.equals("32")) {
	    	machineCodes.add("test %S0 %S0");//asm.emitTEST_Reg_Reg(S0, S0);      // test for null
	    } else {
	    	machineCodes.add("test %S0 %S0");//asm.emitTEST_Reg_Reg_Quad(S0, S0); // test for null
	    }
	    //ForwardReference isNull = asm.forwardJcc(Assembler.EQ);

	    //baselineEmitLoadTIB(asm, S0, S0);    // S0 = TIB of object
	    if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitMOV_Reg_RegDisp(GPR.lookup(dest), GPR.lookup(object),
			 * tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		} else {
			/*
			 * asm.emitMOV_Reg_RegDisp_Quad(GPR.lookup(dest),
			 * GPR.lookup(object), tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		}
	    // S0 = implements bit vector
	    if (AddrJVM_32_or_64.equals("32")) {
	    	machineCodes.add("mov %S0 %S0");//asm.emitMOV_Reg_RegDisp(S0, S0, Offset.fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX << LG_WORDSIZE));
	    } else {
	    	machineCodes.add("mov %S0 %S0");//asm.emitMOV_Reg_RegDisp_Quad(S0, S0, Offset.fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX << LG_WORDSIZE));
	    }
	    /*
	    ForwardReference outOfBounds = null;
	    if (DynamicTypeCheck.MIN_SUPERCLASS_IDS_SIZE <= LHSDepth) {
	      // must do arraybounds check of superclass display
	      if (JavaHeaderConstants.ARRAY_LENGTH_BYTES == 4) {
	        asm.emitCMP_RegDisp_Imm(S0, ObjectModel.getArrayLengthOffset(), LHSDepth);
	      } else {
	        asm.emitCMP_RegDisp_Imm_Quad(S0, ObjectModel.getArrayLengthOffset(), LHSDepth);
	      }
	      outOfBounds = asm.forwardJcc(Assembler.LLE);
	    }*/
	    machineCodes.add("cmp %S0 $0");
	    /*
	    // Load id from display at required depth and compare against target id; push true if matched
	    asm.emitMOVZX_Reg_RegDisp_Word(S0, S0, Offset.fromIntZeroExtend(LHSDepth << LOG_BYTES_IN_SHORT));
	    asm.emitCMP_Reg_Imm(S0, LHSId);
	    ForwardReference notMatched = asm.forwardJcc(Assembler.NE);
	    asm.emitPUSH_Imm(1);
	    ForwardReference done = asm.forwardJMP();

	    // push false
	    isNull.resolve(asm);
	    if (outOfBounds != null) outOfBounds.resolve(asm);
	    notMatched.resolve(asm);
	    asm.emitPUSH_Imm(0);

	    done.resolve(asm); */
	    machineCodes.add("mov %S0 %S0");
	    machineCodes.add("cmp %S0 $0");
	    machineCodes.add("push $0");
	    machineCodes.add("push $0");
		return machineCodes;
	}

	private List<String> emit_instanceof_final(RVMType type) {
		List<String> machineCodes = new ArrayList<String>();
		//asm.emitPOP_Reg(S0);                 // load object from stack
		machineCodes.add("pop %S0");
	    /*if (VM.BuildFor32Addr) {
	      asm.emitTEST_Reg_Reg(S0, S0);      // test for null
	    } else {
	      asm.emitTEST_Reg_Reg_Quad(S0, S0); // test for null
	    }
	    ForwardReference isNull = asm.forwardJcc(Assembler.EQ);

	    // compare TIB of object to desired TIB and push true if equal
	    baselineEmitLoadTIB(asm, S0, S0);
	    if (VM.BuildFor32Addr) {
	      asm.emitCMP_Reg_Abs(S0, Magic.getTocPointer().plus(type.getTibOffset()));
	    } else {
	      asm.emitCMP_Reg_Abs_Quad(S0, Magic.getTocPointer().plus(type.getTibOffset()));
	    }*/
		if (AddrJVM_32_or_64.equals("32")) {
	    	machineCodes.add("test %S0 %S0");//asm.emitTEST_Reg_Reg(S0, S0);      // test for null
	    } else {
	    	machineCodes.add("test %S0 %S0");//asm.emitTEST_Reg_Reg_Quad(S0, S0); // test for null
	    }
	    //ForwardReference isNull = asm.forwardJcc(Assembler.EQ);

	    //baselineEmitLoadTIB(asm, S0, S0);    // S0 = TIB of object
	    if (AddrJVM_32_or_64.equals("32")) {
			/*
			 * asm.emitMOV_Reg_RegDisp(GPR.lookup(dest), GPR.lookup(object),
			 * tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		} else {
			/*
			 * asm.emitMOV_Reg_RegDisp_Quad(GPR.lookup(dest),
			 * GPR.lookup(object), tibOffset);
			 */
			machineCodes.add("mov %T0 %T1");
		}
	    // S0 = implements bit vector
	    if (AddrJVM_32_or_64.equals("32")) {
	    	machineCodes.add("cmp %S0 $0");//asm.emitMOV_Reg_RegDisp(S0, S0, Offset.fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX << LG_WORDSIZE));
	    } else {
	    	machineCodes.add("cmp %S0 $0");//asm.emitMOV_Reg_RegDisp_Quad(S0, S0, Offset.fromIntZeroExtend(TIB_DOES_IMPLEMENT_INDEX << LG_WORDSIZE));
	    }
	    /*
	    ForwardReference notMatched = asm.forwardJcc(Assembler.NE);
	    asm.emitPUSH_Imm(1);
	    ForwardReference done = asm.forwardJMP();

	    // push false
	    isNull.resolve(asm);
	    notMatched.resolve(asm);
	    asm.emitPUSH_Imm(0);

	    done.resolve(asm);*/
	    machineCodes.add("push $0");
	    machineCodes.add("push $0");
	    
		return machineCodes;
	}

	private List<String> emit_monitorenter() {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_monitorexit() {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_deferred_prologue() {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_invoke_compiledmethod(CompiledMethod cm) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_loadretaddrconst(int bcIndex) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_pending_goto(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();

		return machineCodes;
	}

	private List<String> emit_nop(int bTarget) {
		List<String> machineCodes = new ArrayList<String>();
		machineCodes.add("nop");
		return machineCodes;
	}

}
