package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PrintInfo {
	
	public static final String DIVIDER = "~~~";

	public static final String FILE_LOCATION = "/home/nate/Documents/acep/playground/";//Local: /Users/natemw/Documents/acep/profilingOutput/
	
	private static final int GB = 1024*1024*1024;
	
	private static BufferedWriter bw;
	
	private static int count;
	
	private static File toWrite;
	
	private static String originalName;
	
	private static int fileCount;
	
	private BufferedWriter logBW;
	
	private static Map<String, String> uniqueStrings;
	
	private static BufferedWriter lookup;
	
	public PrintInfo(String name){
		makeLog(name);
	}
	
	private void makeLog(String name){
		File logDir = new File(FILE_LOCATION+"logs");
		logDir.mkdir();
		File log = new File(FILE_LOCATION+"logs/log_"+name+".txt");
		log.delete();
		
		try {
			log.createNewFile();
			logBW = new BufferedWriter(new FileWriter(log));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error creating Log");
		}
		
	}
	
	public void closeLog(){
		try {
			logBW.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error trying to close log");
		}
	}
	
	public void log(String s){
		try {
			System.out.println(s);
			logBW.write(s);
			logBW.newLine();
			logBW.flush();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error writing to log!");
		}
	}
	
	public static void makeBW(String filename){
		if(originalName == null){
			File f = new File(FILE_LOCATION+filename+"lookup.txt");
			f.delete();
			try {
				f.createNewFile();
				lookup = new BufferedWriter(new FileWriter(f));
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error creating lookup file writer!");
			}
			uniqueStrings = new HashMap<>();
			originalName = filename;
			fileCount = 0;
		}
		fileCount++;
		filename = filename + fileCount;
		toWrite = new File(FILE_LOCATION+filename+".txt");
		try {
			toWrite.delete();
			toWrite.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to create file "+filename+" failed");
		}
		try {
			bw = new BufferedWriter(new FileWriter(toWrite));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to create writer on file "+filename+" failed");
		}
		count = 0;
	}
	
	public static void print(String s){
		count++;
		if(count % 1000000 == 0 && toWrite.length() > GB){//Check every million instructions
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error trying to close file "+fileCount);
			}
			System.out.println("["+originalName+"] New file!");
			makeBW(originalName);
		}
		try {
			if(uniqueStrings.containsKey(s)){
				bw.write(uniqueStrings.get(s)+System.lineSeparator());
			} else {
				String newLoc = ""+uniqueStrings.size();
				uniqueStrings.put(s, newLoc);
				bw.write(newLoc+System.lineSeparator());
				lookup.write(s);
				lookup.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to write '"+s+"' failed");
		}
	}
	
	public static void close(){
		try {
			lookup.close();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to close BufferedWriter failed");
		}
		System.out.println("Finished, total instructions: "+count);
	}
	
}
