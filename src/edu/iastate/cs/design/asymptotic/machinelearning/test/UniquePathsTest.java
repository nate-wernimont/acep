package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Benchmark;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.Unit;

public class UniquePathsTest {

	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException{
		if(args.length < 1){
			System.out.println("No benchmark supplied");
			return;
		}
		String b = args[0];
		String config = b + File.separator + "config.xml";
		Benchmark benchmark = new Test(config);
		for(SootClass _class : Scene.v().getApplicationClasses()){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			PathEnumerator pe = new PathEnumerator(_class, false);
			pe.run();
			System.out.println("==="+_class.getShortName()+"===");
			List<Path<Unit>> paths = pe.getPaths();
			System.out.println("Total Paths: "+paths.size());
			Set<Path<Unit>> uniqPaths = new HashSet<>(paths);
			System.out.println("Unique Paths: "+uniqPaths.size());
			uniqPaths.forEach((v)->System.out.println(v.toString()));
			
			
			File f = new File("serializationTest.txt");
			f.createNewFile();
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))){
				for(Path<Unit> path : paths){
					Unit debug = null;
					try {
						for(Unit unit : path.getElements()){
							debug = unit;
							oos.writeObject(unit);
						}
					} catch(NotSerializableException e){
						System.out.println(debug);
						System.out.println(debug.getUseBoxes().get(0).getValue());
						throw new Error();
					}
				}
			}
			List<Path<Unit>> deserializedPaths = new ArrayList<>();
			
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))){
				Path<Unit> path = null;
				while((path = (Path<Unit>) ois.readObject()) != null){
					deserializedPaths.add(path);
				}
			}
			
			System.out.println("Unique Strings: "+deserializedPaths.size());
			
		}
		
	}
	
}
