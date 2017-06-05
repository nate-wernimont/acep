package edu.iastate.cs.design.asymptotic.datastructures;

import edu.iastate.cs.design.asymptotic.interfaces.impl.Analysis;
import soot.SootMethod;

/**
 * Some predefined filters
 * 
 * @author Sean Mooney
 * 
 */
public class Filters {
	// Singleton
	private Filters() {

	}

	/**
	 * Create a filter that excludes methods from any class marked as a library
	 * class.
	 * 
	 * @return
	 */
	public static Predicate<SootMethod> noLibraryFilter() {
		return new Predicate<SootMethod>() {

			@Override
			public final boolean apply(SootMethod t) {
				// boolean isNotLib = !t.getDeclaringClass().isLibraryClass();
				// boolean isNotLib =
				// !t.getDeclaringClass().isApplicationClass();
				/*boolean isNotLib = (t.getDeclaringClass().getPackageName()
						.startsWith("org.apache.lucene") || t
						.getDeclaringClass().getPackageName().startsWith(
								"org.dacapo"));*/
				boolean isNotLib = t.getDeclaringClass().isApplicationClass();
				
				// Collecting all library method calls. TODO: Remove this code later
				/*if (!isNotLib) {
					if (!t.getName().contains("<init>") && !t.getName().contains("<clinit>"))
						Analysis.lib_methods.add(t.getSignature());
				}*/
				return isNotLib;
			}
		};
	}

	/**
	 * Filter out all default constructors
	 * 
	 * @return
	 */
	public static Predicate<SootMethod> noDefaultConstructor() {
		return noMethodNamed("<init>");
	}
	
	public static Predicate<SootMethod> noApplyDeletesMethod() {
		return noMethodNamed("applyDeletes");
	}

	public static Predicate<SootMethod> noInterfaceDefaultConstructor() {
		return noMethodNamed("<clinit>");
	}

	public static Predicate<SootMethod> noMethodNamed(final String methodName) {
		return new Predicate<SootMethod>() {
			@Override
			public boolean apply(SootMethod t) {
				return !t.getName().contains(methodName);
			}
		};
	}

	/**
	 * Return a predicate made up of an arbitrary number of subpredicates. All
	 * predicates are joined by conjunction
	 * 
	 * @param <U>
	 * @param subfilters
	 * @return
	 */
	public static <U> Predicate<U> compositeConjPredicate(
			final Predicate<U>... subfilters) {
		return new Predicate<U>() {

			Predicate<U>[] filters = subfilters;

			@Override
			public final boolean apply(U t) {
				for (int i = 0; i < filters.length; i++) {
					if (!filters[i].apply(t))
						return false;
				}

				return true; // made it through all the filters.
			}
		};
	}
}
