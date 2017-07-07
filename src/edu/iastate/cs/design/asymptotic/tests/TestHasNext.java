package edu.iastate.cs.design.asymptotic.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestHasNext {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		List<Integer> integers = new ArrayList<Integer>();
		integers.add(1);
		integers.add(2);
		//integers.add(3);
		Iterator<Integer> first = integers.iterator();
		Iterator<Integer> second = integers.iterator();
		second.next();
		int sum = 0;
		while (first.hasNext() && second.hasNext()) {
			int a = first.next();
			int b = second.next();
			System.out.println (a);
			if (!second.hasNext()) {
				System.out.println (b);
			}
		}
	}

}
