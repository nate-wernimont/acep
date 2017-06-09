package edu.iastate.cs228.hw4;

import java.util.Arrays;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.File;

/**
 * @author Nate Werniont
 * 
 *         An application class
 */
public class Dictionary {

	/**
	 * The filename that this Dictionary reads from
	 */
	String filename;
	
	/**
	 * The EntryTree to be manipulated
	 */
	EntryTree<Character, String> entryTree;

	/**
	 * Creates a new dictionary and runs it with the given filename
	 * @param args the first input is the filename to read
	 */
	public static void main(String[] args) {
		Dictionary dict = new Dictionary(args[0]);
		try {
			dict.run();
		} catch (FileNotFoundException e) {
			System.out.println("That is an invalid filename!");
			e.printStackTrace();
		}
	}

	/**
	 * Constructs a new Dictionary object with a given filename
	 * @param filename the file that contains commands
	 */
	private Dictionary(String filename) {
		this.filename = filename;
		entryTree = new EntryTree<Character, String>();
	}
	
	/**
	 * Reads from the file associated with this instance of Dictionary and executes the commands
	 * @throws FileNotFoundException
	 */
	private void run() throws FileNotFoundException {
		String[] commands = readFile();
		for (String s : commands) {
			System.out.println("Command: "+s);
			execute(s);
		}
	}

	/**
	 * Executes a given command
	 * @param s The command to execute
	 */
	private void execute(String s) {
		String[] subCommands = new String[3];
		Scanner scanCommand = new Scanner(s);
		int counter = 0;
		while(scanCommand.hasNext()){
			subCommands[counter++] = scanCommand.next();
		}
		scanCommand.close();
		switch (subCommands[0]) {
		case "add":
			System.out.println("Result from an add: "+entryTree.add(toCharArray(subCommands[1]), subCommands[2]));
			break;
		case "remove":
			System.out.println("Result from a remove: "+entryTree.remove(toCharArray(subCommands[1])));
			break;
		case "search":
			System.out.println("Result from a search: "+entryTree.search(toCharArray(subCommands[1])));
			break;
		case "prefix":
			System.out.println("Result from a prefix: "+arrToString(entryTree.prefix(toCharArray(subCommands[1]))));
			break;
		case "showTree":
			System.out.println("Result from a showTree:");
			entryTree.showTree();
			break;
		}
	}
	
	private String arrToString(Object[] obj){
		String result = "";
		for(Object o: obj){
			result = result+o.toString();
		}
		return result;
	}
	
	/**
	 * Converts a given string to a Character[]
	 * @param s The string the convert
	 * @return A character array of the given string.
	 */
	private Character[] toCharArray(String s){
		Character[] charArr = new Character[s.length()];
		for(int i = 0; i < charArr.length; i++){
			charArr[i] = new Character(s.charAt(i));
		}
		return charArr;
	}

	/**
	 * Reads a given file and returns a string array where each string is a line of the file
	 * @return A string array of the lines of the file
	 * @throws FileNotFoundException
	 */
	private String[] readFile() throws FileNotFoundException {
		Scanner scan = new Scanner(new File(filename));
		int counter = 0;
		while(scan.hasNextLine()){//Find the size of array that I need, rather than constantly resizing an array
			scan.nextLine();
			counter++;
		}
		String[] result = new String[counter];
		scan.close();
		scan = new Scanner(new File(filename));
		counter = 0;
		while(scan.hasNextLine()){
			result[counter++] = scan.nextLine();
		}
		scan.close();
		return result;
	}
}
