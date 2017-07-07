package edu.iastate.cs.design.asymptotic.tests;

public class test1 {

	public static void main(String[] args) {

		int j = 0;
		int k = 0;
		Object obj = null;
		
		if(obj == null) 
			return;
		
		for(int i = 0; i < 5; i++) {
			for(int m = 0; m < 2; m++) {
				j++;
				if(i < 2) {
					j++;
				} else {
					k++;
				}
			}
		}
		
		int p = 5;
		while (p-- > 0){
			
		}

		System.out.println(j);
	}
}
