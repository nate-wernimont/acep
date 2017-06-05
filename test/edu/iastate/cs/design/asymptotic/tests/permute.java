package edu.iastate.cs.design.asymptotic.tests;

public class permute {
	
	public static void main(String[] args) {
		int max;
		char a[];
		max = atoi(args[1], args[1].length());
		a= new char[max];
		permute(a,0,max);
	}
	
	public static void permute (char a[], int n, int max) {
		if (n == max){
			repeat_one(a,max);
		} else {
			permute_next_pos(a,n,max);
		}
	}
	
	public static void permute_next_pos(char a[], int n, int max) {
		for (int i = 0; i < max; i++) {
			if(!in_prefix(i,a,n)) {
				a[n] = a[i];
				permute (a, n+1, max);
			}
		}
	}
	
	public static void repeat_one(char a[], int max) {
		for (int i = 0; i < max; i++) {
			System.out.println (a[i]+'0');
		}
		System.out.println();
	}

	public static boolean in_prefix (int i, char a[], int n) {
		int found = 0;
		for (int j = 0; j < n; j++) {
			if (a[j] == i) {
				return true;
			}
		}
		
		return false;
	}
	
	public static int atoi (String s, int size) {
		int val;
		if ( size != 0) {
			val = 0;
			char str[] = s.toCharArray();
			for (int j = 0; j < size; j++) {
				val = val * 10 + str[j] - '0';
			}
			return val;
		} else {
			System.out.println ("Invalid input");
			return 0;
		}
	}
}
