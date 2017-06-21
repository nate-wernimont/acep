package edu.iastate.cs.design.asymptotic.machinelearning.calculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PrintInfo {
	
	public static final String DIVIDER = "~~~";

	public static final String FILE_LOCATION = "/Users/natemw/Documents/acep/profilingOutput/";
	
	private static BufferedWriter bw;
	
	private static int count;
	
	public static void makeBW(String filename){
		File toWrite = new File(FILE_LOCATION+filename+".txt");
		try {
			toWrite.delete();
			toWrite.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to create file "+filename+" failed");
		}
		try {
			bw = new BufferedWriter(new FileWriter(new File(FILE_LOCATION+filename+".txt"), true));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to create writer on file "+filename+" failed");
		}
		count = 0;
	}
	
	public static void print(String s){
		try {
			bw.write(s);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to write '"+s+"' failed");
		}
		count++;
		if(count % 100000 == 0)
			System.out.print(count+": "+s);
	}
	
	public static void close(){
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Trying to close BufferedWriter failed");
		}
		System.out.println("Finished");
	}
	
}
