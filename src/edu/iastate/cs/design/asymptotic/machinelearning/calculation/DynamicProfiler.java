package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nustaq.serialization.FSTConfiguration;

import edu.iastate.cs.design.asymptotic.datastructures.Pair;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.options.Options;

/**
 * 
 * @author Nate Wernimont
 * Instruments a class and converts execution data into paths
 *
 */
public class DynamicProfiler {

	/**
	 * The main application class
	 */
	SootClass _class;
	
	private BufferedReader _reader;
	
	private PrintInfo _logger;
	
	private ArrayList<Pair<Path<Unit>, Integer>> _pathCounts;
	
	private int _fileNumber;
	
	private int _count;
	
	private long _startTime;
	
	private int _unitDeletion;
	
	/**
	 * Where to find the files
	 */
	private String FILE_LOCATION = "";
	
	private FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration().setForceSerializable(true);
	
	/**
	 * Initializes an instance of the profiler
	 * @param _class
	 */
	public DynamicProfiler(SootClass _class){
		this._class = _class;
	}
	
	/**
	 * Instruments the class of this profiler
	 * @param output_format The output format of the instrumented code
	 */
	public void addTransformer(int output_format){
		PackManager.v().getPack("jtp").add(new Transform("jtp.statementLogger", new Instrumenter()));
		Options.v().set_output_format(output_format);
//		File dir = new File("./profilingOutput/");
//		dir.mkdirs();
//		for(File f : dir.listFiles()){
//			f.delete();
//		}
		soot.Main.main(new String[]{_class.getName()});
	}
	
	/**
	 * Run the instrumented class
	 */
	public void runNewClass(){//TODO: try to get running
		Options.v().set_soot_classpath(Options.v().output_dir()+":"+Options.v().soot_classpath());
		try {
			Object newClass = ClassLoader.getSystemClassLoader().loadClass(_class.getName()).newInstance();
			((InstrumentedClass) newClass).main();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			Runtime.getRuntime().exec("./sootOutput/"+_class.getName().replace('.', '/')+".class");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String analyzeFilesHelper(String currLine){
		Path<Unit> currPath = new Path<>();
		Set<Path<Unit>> backEdges = new HashSet<>();
		Path<Unit> currLoopSegment = new Path<>();
		int repCount = 0;
		Unit lastUnit = null;
		
		String line = currLine;
		SootMethod thisMeth = Scene.v().getMethod(line.split(PrintInfo.DIVIDER)[0]);
		SootMethod meth = thisMeth;
		mainLoop:
		while(line != null){
			try {
				String methodSignature = line.split(PrintInfo.DIVIDER)[0];
				String unitString = line.split(PrintInfo.DIVIDER)[1];
				meth = Scene.v().getMethod(methodSignature);
				
				_count++;
				
				if(_count % 1000000 == 0){
					_logger.log("["+_class.getShortName()+"] Number of statements processed: "+_count+
							", \n\tAmount of back paths: "+backEdges.size()+
							", \n\tCurrent repCount: "+repCount+
							", \n\tNumber of found paths: "+_pathCounts.size()+
							", \n\tThe total length of those paths: "+pathSize(_pathCounts)+
							", \n\tNumber of times those paths have been traversed: "+traversedPaths(_pathCounts)+
							", \n\tThe amount of units used:"+(_count-_unitDeletion)+
							", \n\tTime elapsed:"+(System.currentTimeMillis()-_startTime)/1000);
				}
				
				if(meth.isStatic() && meth.isEntryMethod() && meth.getName().equals("<clinit>")){
					line = _reader.readLine();
					continue;//throw away all static initializers. They are only executed once.
				}
				
				boolean found = false;
				
				for(Unit unit : meth.retrieveActiveBody().getUnits()){
					if(unit.toString().equals(unitString)){
						found = true;
						
						if(!meth.equals(thisMeth)){
							if(lastUnit instanceof JReturnStmt || lastUnit instanceof JReturnVoidStmt || lastUnit instanceof JRetStmt){
								if(!thisMeth.getDeclaringClass().equals(meth.getDeclaringClass())){
									//Path ends, go back to last path we were making
									addPath(currPath, repCount, backEdges);
									return line;
								}
							} else if(!meth.getDeclaringClass().equals(thisMeth.getDeclaringClass())){
								//Had to be an invoke, and it is to a different class
								line = analyzeFilesHelper(line);
								continue mainLoop;
								
							}
						}
						//What is the point? All we are doing is modifying the call stack for what will eventually be returned with no added benefits.
//						} else {//we are in the same method. Check for recursion
//							if(lastUnit instanceof JReturnStmt || lastUnit instanceof JReturnVoidStmt || lastUnit instanceof JRetStmt){
//								callStack.pop();
//							} else if(lastUnit instanceof JInvokeStmt || lastUnit instanceof JAssignStmt){//Only checks for recursion
//								SootMethod method_called = getMethodCalled(lastUnit);
//								if(method_called != null){
//									if(Scene.v().getApplicationClasses().contains(method_called.getDeclaringClass()))
//										callStack.push(meth);
//								}
//							}
//						}
						
						if(!currPath.contains(unit)){
							currPath.add(unit);
							currLoopSegment = new Path<>();
						} else {
							_unitDeletion++;
							if(currLoopSegment.contains(unit)){
								//this is looPrintInfong on itself
								boolean foundPath = false;
								for(Path<Unit> loopedPath : backEdges){
									if(loopedPath.equals(currLoopSegment)){
										foundPath = true;
										repCount++;
										break;
									}
								}
								if(!foundPath){
									backEdges.add(currLoopSegment);
								}
								currLoopSegment = new Path<>();
							} else {
								currLoopSegment.add(unit);
							}
						}
						
						lastUnit = unit;
						break;
						
					}
				}
				if(found){
					if((line = _reader.readLine()) == null){
						newFile();
						line = _reader.readLine();
						if(line == null){
							finished(currPath, repCount, backEdges);
							return null;
						}
					}
					continue;
				}
				throw new Error("Unit not found at line "+_count+": "+unitString+":"+meth.retrieveActiveBody().getUnits());
		
			} catch(IOException e){
				e.printStackTrace();
				throw new Error("Encountered a problem while reading the file");
			}
		}
		throw new Error("Shouldn't be here");
		
	}
	
	private boolean finished = false;
	
	private void finished(Path<Unit> currPath, int repCount, Set<Path<Unit>> backEdges){
		if(finished)
			throw new Error("Already finished");
		finished = true;
		_logger.log("["+_class.getShortName()+"] Finished");
		addPath(currPath, repCount, backEdges);
	}
	
	private void addPath(Path<Unit> currPath, int repCount, Set<Path<Unit>> backEdges) {
		boolean inMap = false;
		int location;
		for(location = 0; location < _pathCounts.size(); location++){
			if(_pathCounts.get(location).first().equals(currPath)){
				inMap = true;
				break;
			}
		}
		if(inMap){
			//logger.log("["+originalName+"] Went along a previous path");
			_pathCounts.get(location).setSecond(new Integer(_pathCounts.get(location).second()+1+repCount+backEdges.size()));
		} else {
			//logger.log("["+originalName+"] Found a new path");
			_pathCounts.add(new Pair<>(currPath, new Integer(1+repCount+backEdges.size())));
		}
		
	}

	private void newFile() {
		_logger.log("["+_class.getShortName()+"] Finished file "+_fileNumber);
		_fileNumber++;
		File toRead = new File(PrintInfo.FILE_LOCATION+_class.getShortName()+_fileNumber+".txt");
		if(toRead.exists()){
			try {
				_reader = new BufferedReader(new FileReader(toRead));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new Error("Error finding next file");
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error reading next line");
			}
		}
		
	}

	/**
	 * Analyze the files that were generated from this profilers instrumented application class
	 */
	public void analyzeFiles(){
		FILE_LOCATION=PrintInfo.FILE_LOCATION;
		String line;
		_logger = new PrintInfo(_class.getShortName());
		
		_logger.log("["+_class.getShortName()+"] Started executing!");
		
		_pathCounts = new ArrayList<>();
		_count = 0;
		_startTime = System.currentTimeMillis();
		_unitDeletion = 0;
		
		File toRead = new File(PrintInfo.FILE_LOCATION+_class.getShortName()+"1.txt");
		_fileNumber = 1;
		
		String firstLine = null;
		
		try {
			_reader = new BufferedReader(new FileReader(toRead));
			firstLine = _reader.readLine();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			throw new Error("The first file couldn't be found!");
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new Error("The first file couldn't be read!");
		}
		
		if(analyzeFilesHelper(firstLine) != null)
			throw new Error("Main function didn't end the file");
		
		File resultDir = new File(PrintInfo.FILE_LOCATION+"results/");
		resultDir.mkdir();
		File f = new File(PrintInfo.FILE_LOCATION+"results/results_"+_class.getShortName()+".txt");
		
		try(FileOutputStream fos = new FileOutputStream(f)){
			f.createNewFile();
			fos.write(conf.asByteArray(_pathCounts));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error occurred while writing to the results file!");
		}
		_logger.closeLog();
	}
	
	/**
	 * Fetches the size of all of the given paths
	 * @param pathCounts The paths to analyze
	 * @return The unit count of all of the paths
	 */
	private int pathSize(List<Pair<Path<Unit>, Integer>> pathCounts){
		int total = 0;
		for(Pair<Path<Unit>, Integer> p : pathCounts){
			total += p.first().size();
		}
		return total;
	}
	
	/**
	 * Fetches the number of times the given paths have been traversed
	 * @param pathCounts The paths to analyze
	 * @return The total amount of times these paths have been walked over
	 */
	private int traversedPaths(List<Pair<Path<Unit>, Integer>>  pathCounts){
		int total = 0;
		for(Pair<Path<Unit>, Integer> p : pathCounts){
			total += p.second().intValue();
		}
		return total;
	}
	
}
