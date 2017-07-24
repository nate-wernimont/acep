package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

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
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;

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
	
	private ArrayList<Pair<ArrayList<Unit>, Integer>> _pathCounts;
	
	private int _fileNumber;
	
	private int _count;
	
	private long _startTime;
	
	private int _unitDeletion;
	
	private ArrayList<Pair<SootMethod, Object>> lookupTable;
	
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
	
	private void prepare(){
		lookupTable = new ArrayList<>();
		String line = null;
		try (BufferedReader lookupReader = new BufferedReader(new FileReader(new File(PrintInfo.FILE_LOCATION+_class.getShortName()+"lookup.txt")))){
			reading:
			while((line = lookupReader.readLine()) != null){
				SootMethod sm = Scene.v().getMethod(line.split(PrintInfo.DIVIDER)[0]);
				int blockIndex = 0;
				String unitString = null;
				boolean isBlock = false;
				try {
					blockIndex = Integer.parseInt(line.split(PrintInfo.DIVIDER)[1]);
					isBlock = true;
				} catch(NumberFormatException e){
					unitString = line.split(PrintInfo.DIVIDER)[1];
				}
				if(isBlock){
					for(Block b : new BriefBlockGraph(sm.retrieveActiveBody()).getBlocks()){
						if(b.getIndexInMethod() == blockIndex){
							lookupTable.add(new Pair<SootMethod, Object>(sm, b));
							continue reading;
						}
					}
				} else {
					for(Unit u : sm.retrieveActiveBody().getUnits()){
						if(u.toString().equals(unitString)){
							lookupTable.add(new Pair<SootMethod, Object>(sm, u));
							continue reading;
						}
					}
				}
				throw new Error("Couldn't find the information: "+line+":"+isBlock+":"+blockIndex+":"+unitString+":"+sm.retrieveActiveBody().getUnits());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Error("Error finding lookup file!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error reading from lookup file!");
		}
	}
	
	private String analyzeFilesHelper(String currLine){
		ArrayList<Unit> currPath = new ArrayList<>();
		Map<Unit, Set<ArrayList<Unit>>> loopedSegments = new HashMap<>();
		Unit lastUnit = null;
		
		String line = currLine;

		Supplier<String> newLine = () -> {
			String freshLine = null;
			try {
				if((freshLine = _reader.readLine()) == null){
					newFile();
					if((freshLine = _reader.readLine()) == null){
						finished(currPath, loopedSegments);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error reading from results file!");
			}
			return freshLine;
		};
		
		SootMethod thisMeth = lookupTable.get(Integer.parseInt(line)).first();
		SootMethod meth = thisMeth;
		mainLoop:
		while(line != null){
			Pair<SootMethod, Object> pair = lookupTable.get(Integer.parseInt(line));
			meth = pair.first();
			
			_count++;
			
			if(_count % 1000000 == 0){
				_logger.log("["+_class.getShortName()+"] Number of statements processed: "+_count+
						", \n\tAmount of back paths: "+loopedSegments.size()+
						", \n\tNumber of found paths: "+_pathCounts.size()+
						", \n\tThe total length of those paths: "+pathSize(_pathCounts)+
						", \n\tNumber of times those paths have been traversed: "+traversedPaths(_pathCounts)+
						", \n\tThe amount of units used:"+(_count-_unitDeletion)+
						", \n\tTime elapsed:"+(System.currentTimeMillis()-_startTime)/1000);
			}
			
			if(meth.isStatic() && meth.isEntryMethod() && meth.getName().equals("<clinit>")){
				line = newLine.get();
				continue;//throw away all static initializers. They are only executed once.
			}
			Object blockOrUnit = pair.second();
			
			//Check for returning and invoking first (Both have to do it first, and it avoids repetition)
			if(!meth.equals(thisMeth)){
				if(lastUnit instanceof JReturnStmt || lastUnit instanceof JReturnVoidStmt || lastUnit instanceof JRetStmt){
					if(!thisMeth.getDeclaringClass().equals(meth.getDeclaringClass())){
						//Path ends, go back to last path we were making
						addPath(currPath, loopedSegments);
						return line;
					}
				} else if(!meth.getDeclaringClass().equals(thisMeth.getDeclaringClass())){
					//Had to be an invoke, and it is to a different class
					line = analyzeFilesHelper(line);
					continue mainLoop;
					
				}
			}
			
			if(blockOrUnit instanceof Block){
				Unit toAdd = ((Block) blockOrUnit).getHead();
				while(toAdd != null){
					addUnit(currPath, loopedSegments, lastUnit = toAdd);
					toAdd = ((Block) blockOrUnit).getSuccOf(toAdd);
				}
			} else {
				addUnit(currPath, loopedSegments, lastUnit = (Unit) blockOrUnit);
			}
			if((line = newLine.get()) == null)
				return null;
			continue;
		}
		throw new Error("Shouldn't be here");
		
	}
	
	private void addUnit(ArrayList<Unit> currPath, Map<Unit, Set<ArrayList<Unit>>> loopedSegments, Unit unit){
		if(!currPath.contains(unit)){
			currPath.add(unit);
		} else {
			_unitDeletion++;
			int lastHeader = currPath.lastIndexOf(unit);
			currPath.add(unit);
			Set<ArrayList<Unit>> loopedSegmentList = loopedSegments.get(unit);
			if(loopedSegmentList != null){
				loopedSegmentList.add(new ArrayList<Unit>(currPath.subList(lastHeader, currPath.size())));
			} else {
				loopedSegmentList = new HashSet<>();
				loopedSegmentList.add(new ArrayList<Unit>(currPath.subList(lastHeader, currPath.size())));
				loopedSegments.put(unit, loopedSegmentList);
			}
			currPath.subList(lastHeader+1, currPath.size()).clear();
		}
	}
	
	private boolean finished = false;
	
	private void finished(ArrayList<Unit> currPath, Map<Unit, Set<ArrayList<Unit>>> loopedSegments){
		if(finished)
			throw new Error("Already finished");
		finished = true;
		_logger.log("["+_class.getShortName()+"] Finished");
		addPath(currPath, loopedSegments);
	}
	
	private void addPath(ArrayList<Unit> currPath, Map<Unit, Set<ArrayList<Unit>>> loopedSegments) {
		Set<ArrayList<Unit>> paths = getPaths(currPath, loopedSegments);
		for(ArrayList<Unit> path : paths){
			boolean inMap = false;
			int location;
			for(location = 0; location < _pathCounts.size(); location++){
				if(_pathCounts.get(location).first().equals(path)){
					inMap = true;
					break;
				}
			}
			if(inMap){
				_pathCounts.get(location).setSecond(new Integer(_pathCounts.get(location).second()+1));
			} else {
				//logger.log("["+originalName+"] Found a new path");
				_pathCounts.add(new Pair<ArrayList<Unit>, Integer>(currPath, new Integer(1)));
			}
		}
	}

	private Set<ArrayList<Unit>> getPaths(List<Unit> path, Map<Unit, Set<ArrayList<Unit>>> loopedSegments) {
		ArrayList<ArrayList<Unit>> result = new ArrayList<>();
		result.add(new ArrayList<Unit>());
		Set<Unit> loops = loopedSegments.keySet();
		nextUnit:
		for(Unit unit : path){
			for(Iterator<Unit> iter = loops.iterator(); iter.hasNext();){
				Unit loopHeader = iter.next();
				if(unit.equals(loopHeader)){
					ArrayList<ArrayList<Unit>> originalPaths = new ArrayList<>(result);
					result = new ArrayList<>();
					for(ArrayList<Unit> originalPath : originalPaths){
						for(ArrayList<Unit> loopedPath : loopedSegments.get(loopHeader)){
//							if(loopedPath.equals(path))
//								continue;
							Set<ArrayList<Unit>> loopedLoopedPaths = getPaths(loopedPath.subList(1, loopedPath.size()-1), loopedSegments);
							for(ArrayList<Unit> loopedLoopedPath : loopedLoopedPaths){
								ArrayList<Unit> toAdd = new ArrayList<>(originalPath);
								toAdd.add(unit);
								toAdd.addAll(loopedLoopedPath);
								toAdd.add(unit);
								result.add(toAdd);
							}
						}
					}
					continue nextUnit;
				}
			}
			result.forEach((v) -> {
				v.add(unit);
			});
		}
		return new HashSet<>(result);
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
		
		prepare();
		
		if(analyzeFilesHelper(firstLine) != null)
			throw new Error("Main function didn't end the file");
		
		File resultDir = new File(PrintInfo.FILE_LOCATION+"results/");
		resultDir.mkdir();
		File f = new File(PrintInfo.FILE_LOCATION+"results/results_"+_class.getShortName()+".txt");
		f.delete();
		
		System.out.println(_pathCounts.size());
		
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
	 * @param _pathCounts2 The paths to analyze
	 * @return The unit count of all of the paths
	 */
	private int pathSize(ArrayList<Pair<ArrayList<Unit>, Integer>> _pathCounts2){
		int total = 0;
		for(Pair<ArrayList<Unit>, Integer> p : _pathCounts2){
			total += p.first().size();
		}
		return total;
	}
	
	/**
	 * Fetches the number of times the given paths have been traversed
	 * @param _pathCounts2 The paths to analyze
	 * @return The total amount of times these paths have been walked over
	 */
	private int traversedPaths(ArrayList<Pair<ArrayList<Unit>, Integer>>  _pathCounts2){
		int total = 0;
		for(Pair<ArrayList<Unit>, Integer> p : _pathCounts2){
			total += p.second().intValue();
		}
		return total;
	}
	
}
