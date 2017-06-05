package edu.iastate.cs.design.asymptotic.datastructures;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class Log {

	static FileWriter fstream;
	//static FileWriter fstream1;
	static BufferedWriter log;
	/*static BufferedWriter llog;
	static FileWriter fstream2;
	static FileWriter fstream3;
	static FileWriter fstream4;
	static BufferedWriter lllog;
	static BufferedWriter llllog;
	static BufferedWriter lllllog;
	static FileWriter fstream5;
	static BufferedWriter llllllog;*/

	/**
	 * @param args
	 */
	public static void init() {
		try {
			fstream = new FileWriter("time.txt");
			log = new BufferedWriter(fstream);
			/*fstream1 = new FileWriter("blocks.txt");
			llog = new BufferedWriter(fstream1);
			fstream2 = new FileWriter("paths.txt");
			lllog = new BufferedWriter(fstream2);
			fstream3 = new FileWriter("regions.txt");
			llllog = new BufferedWriter(fstream3);
			fstream4 = new FileWriter("dependence.txt");
			lllllog = new BufferedWriter(fstream4);
			fstream5 = new FileWriter("methodStmt.txt");
			llllllog = new BufferedWriter(fstream5);*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void printTimes(String msg) {
		try {
			log.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*public static void printBlocks(String msg) {
		try {
			llog.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printPaths(String msg) {
		try {
			lllog.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public static void printRegions (String msg) {
		try {
			llllog.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printDependencies (String msg) {
		try {
			lllllog.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printMethodStmts (String msg) {
		try {
			llllllog.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	public static void close() {
		try {
			log.close();
			/*llog.close();
			lllog.close();
			llllog.close();
			lllllog.close();
			llllllog.close();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
