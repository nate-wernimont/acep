package edu.iastate.cs.design.asymptotic.machinelearning.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.objenesis.strategy.StdInstantiatorStrategy;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.Path;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PathEnumerator;
import edu.iastate.cs.design.asymptotic.machinelearning.calculation.PrintInfo;
import edu.iastate.cs.design.asymptotic.tests.benchmarks.Test;
import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.jimple.internal.JIdentityStmt;

public class UniquePathsTest {
	
	static FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);

	public static void main(String[] args) throws Exception{
		if(args.length < 1){
			System.out.println("No benchmark supplied");
			return;
		}
		String b = args[0];
		String config = b + File.separator + "config.xml";
		new Test(config);
		for(SootClass _class : Scene.v().getApplicationClasses()){
			if(_class.isLibraryClass() || _class.isJavaLibraryClass() || !_class.isConcrete()){
				continue;
			}
			PathEnumerator pe = new PathEnumerator(_class, false);
			pe.run();
			System.out.println("==="+_class.getShortName()+"===");
			List<Path> paths = pe.getPaths();
			System.out.println("Total Paths: "+paths.size());
			Set<Path> uniqPaths = new HashSet<>(paths);
			System.out.println("Unique Paths: "+uniqPaths.size());
			//uniqPaths.forEach((v)->System.out.println(v.toString()));
			//kryo.setDefaultSerializer(BeanSerializer.class);
//			File f = new File("serializationTest.txt");
//			f.delete();
//			f.createNewFile();
//			byte[] barray = conf.asByteArray(paths);
//			try(FileOutputStream out = new FileOutputStream(f)){
//				out.write(barray);
//			}
//			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))){
//				for(Path<Unit> path : paths){
//					out.writeObject(conf.asByteArray(path));
//				}
//			}
			//kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
//			System.out.println("Done writing");
//			List<Path<Unit>> deserializedPaths = null;
//			try(FileInputStream in = new FileInputStream(f)){
//				byte[] result = new byte[(int) f.length()];
//				in.read(result);
//				deserializedPaths = (List<Path<Unit>>) conf.asObject(result);
//			}
			//deserializedPaths.forEach((v)->System.out.println(v.toString()));
//			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))){
//				Path<Unit> path = null;
//				while((path = conf.asObject(in.read)) != null){
//					deserializedPaths.add(path);
//				}
//			}
			Set<String> uniqStrings = new HashSet<>();
			for(Path path : paths){
				if(!uniqStrings.add(path.toString())){
					System.out.println(path.toString());
				}
			}
//			Set<Path<Unit>> uniqPathsSerialized = new HashSet<>(deserializedPaths);
			System.out.println("Unique Strings: "+uniqStrings.size());
			
		}
		
		File f = new File(PrintInfo.FILE_LOCATION+"results/results_"+Scene.v().getMainClass()+".txt");
		ArrayList<Pair<ArrayList<Unit>, Integer>> paths = null;
		try (FileInputStream fis = new FileInputStream(f)){
			byte[] in = new byte[(int)f.length()];
			fis.read(in);
			paths = (ArrayList<Pair<ArrayList<Unit>, Integer>>) conf.asObject(in);
		}
		System.out.println("==="+Scene.v().getMainClass()+"===");
		System.out.println("Total Paths: "+paths.size());
		System.out.println("Unique Paths: "+new HashSet<>(paths).size());
		Set<ArrayList<Unit>> set = new HashSet<>();
		for(Pair<ArrayList<Unit>, Integer> path : paths){
			if(!set.add(path.first())){
				System.out.println("Already in: "+path.first().toString());
				for(ArrayList<Unit> setPath : set){
					if(setPath.equals(path))
						System.out.println("Matched: "+setPath.toString());
				}
			}
		}
		
	}
	
}
