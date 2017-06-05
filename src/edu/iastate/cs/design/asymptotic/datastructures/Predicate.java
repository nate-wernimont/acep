package edu.iastate.cs.design.asymptotic.datastructures;

/**
 * Defines an abstract interface for filter on a predicate of somesort.
 * Basically a copy of any interface that exists in the apache commons collection
 * project, but without introducing that dependency.
 * 
 * @author Sean Mooney
 *
 * @param <T>
 */
public interface Predicate<T> {
	boolean apply(T t);
}
