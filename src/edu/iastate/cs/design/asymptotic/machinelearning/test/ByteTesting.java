package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.util.ArrayList;
import java.util.List;

import edu.iastate.cs.design.asymptotic.machinelearning.test.example.ByteTest;

//import org.nustaq.serialization.FSTConfiguration;

import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class ByteTesting {
	
	//static FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);

	public static void main(String[] args){
		ByteTest l;
		String _classpath = "/usr/java/jdk1.8.0_131/jre/lib/rt.jar:/usr/java/jdk1.8.0_131/jre/lib/jce.jar:/home/nate/Documents/acep/src";
		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		System.out.println(Options.v().soot_classpath());
		Options.v().set_whole_program(true);
		Scene.v().loadNecessaryClasses();
		SootClass sClass = Scene.v().forceResolve("edu.iastate.cs.design.asymptotic.machinelearning.test.example.ByteTest", SootClass.BODIES);
		sClass.setApplicationClass();
		Scene.v().forceResolve("java.lang.Thread", SootClass.SIGNATURES);
		Scene.v().addBasicClass("java.lang.ref.Finalizer", SootClass.SIGNATURES);
		Options.v().set_output_format(Options.output_format_jimple);
		soot.Main.main(new String[]{sClass.getName()});
		
	}
	
}
