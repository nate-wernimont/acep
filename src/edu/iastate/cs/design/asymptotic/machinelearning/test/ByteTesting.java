package edu.iastate.cs.design.asymptotic.machinelearning.test;

import edu.iastate.cs.design.asymptotic.machinelearning.test.example.ByteTest;

//import org.nustaq.serialization.FSTConfiguration;

import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class ByteTesting {
	
	//static FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);

	public static void main(String[] args){
		ByteTest l;
		String _classpath = "/usr/java/jdk1.6.0_45/jre/lib/rt.jar:/usr/java/jdk1.6.0_45/jre/lib/jce.jar:/home/nate/Documents/acep/src:/usr/java/jdk1.8.0_131/jre/lib";
		Options.v().set_keep_line_number(true);
		Options.v().set_soot_classpath(_classpath);
		System.out.println(Options.v().soot_classpath());
		Options.v().set_whole_program(true);
		Scene.v().loadNecessaryClasses();
		SootClass sClass = Scene.v().forceResolve("edu.iastate.cs.design.asymptotic.machinelearning.test.example.isPrime", SootClass.BODIES);
		sClass.setApplicationClass();
		Options.v().set_output_format(Options.output_format_jimple);
		Scene.v().setMainClass(sClass);
		soot.Main.main(new String[]{sClass.getName()});
		
	}
	
}
