package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PrintInfo {
	
	public static final String DIVIDER = "~~~";

	public static final String FILE_LOCATION = "/home/ganeshau/nat/";//Local: /Users/natemw/Documents/acep/profilingOutput/
	
	private static final int GB = 1024*1024*1024;
	
	private static BufferedWriter bw;
	
	private static int count;
	
	private static File toWrite;
	
	private static String originalName;
	
	private static int fileCount;
	
	private BufferedWriter logBW;
	
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
			bw.write(s);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to write '"+s+"' failed");
		}
	}
	
	public static void close(){
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to close BufferedWriter failed");
		}
		System.out.println("Finished, total instructions: "+count);
	}
	
}
