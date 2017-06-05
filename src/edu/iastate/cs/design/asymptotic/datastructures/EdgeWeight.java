package edu.iastate.cs.design.asymptotic.datastructures;

import soot.SootMethod;
import soot.Unit;

public class EdgeWeight {

	private Unit _from;
	private Unit _to;
	private double _cfreq = 0.0;
	private SootMethod _method;
	
	public EdgeWeight (Unit from, Unit to) {
		_from = from;
		_to = to;
	}
	
	public EdgeWeight (Pair<Unit, Unit> edge) {
		_from = edge.first();
		_to = edge.second();
	}
	
	public void equals (double freq) {
		_cfreq = freq;
	}
	
	public double cfreq() {
		return _cfreq;
	}
	
	public void set_cfreq(double _cfreq) {
		this._cfreq = _cfreq;
	}
}
