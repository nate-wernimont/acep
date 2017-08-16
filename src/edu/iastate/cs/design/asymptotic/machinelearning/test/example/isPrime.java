package edu.iastate.cs.design.asymptotic.machinelearning.test.example;

public class isPrime {

	public static void main(String[] args) {
	}

	
	public boolean isPrime(int num){
		if(num <= 1)
			return false;
		if(num == 2)
			return true;
		for(int i = 2; i <= Math.sqrt(num); i++){
			if(num % i == 0)
				return false;
		}
		return true;

	}
}
