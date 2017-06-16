package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.options.Options;

public class DynamicProfiler {

	SootClass _class;
	
	public DynamicProfiler(SootClass _class){
		this._class = _class;
	}
	
	public void addTransformer(int output_format) throws IOException{
		PackManager.v().getPack("jtp").add(new Transform("jtp.statementLogger", new Instrumenter()));
		Options.v().set_output_format(output_format);
		File dir = new File("./profilingOutput/");
		dir.mkdirs();
		soot.Main.main(new String[]{_class.getName()});
	}
	
	public void runNewClass() throws IOException{
		Options.v().set_soot_classpath(Options.v().output_dir()+":"+Options.v().soot_classpath());
		Runtime.getRuntime().exec("./sootOutput/"+_class.getName().replace('.', '/')+".class");
	}
	
}
