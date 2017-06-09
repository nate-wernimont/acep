package edu.iastate.cs.design.asymptotic.branching.test.example;

public class MethodsIntermingling {

	public void method1(){
		method2();
		method3();
		method4();
	}
	
	public void method2(){
		int x = 3;
	}
	
	public void method3(){
		int y = 5;
	}
	
	public void method4(){
		
	}
	
	
}
