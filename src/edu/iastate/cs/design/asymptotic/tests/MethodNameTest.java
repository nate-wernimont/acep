package edu.iastate.cs.design.asymptotic.tests;

public class MethodNameTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String methodString = "public int docFreq(org.apache.lucene.index.Term)throws java.io.IOException";
		String className = "org.apache.lucene.search.IndexSearcher";
		System.out.println (getMethodString(methodString, className));

	}
	
	static public String getMethodString (String methodString, String className) {
		String parts[] = methodString.split(" ");
		methodString = "";
		for (String string : parts) {
			if (string.contains("(")) {
				String subString = string.substring(0, string.indexOf(')')+1);
				String temp = className + "." + subString;
				methodString += temp;
				return methodString;
			} else {
				methodString += string + " ";
			}
		}
		return methodString;
	}

}
