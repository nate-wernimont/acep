package edu.iastate.cs.design.asymptotic.datastructures;

import soot.Unit;

public class UnitInfo {

	int _type; // SRC_UNIT/BAF_UNIT
	Unit _unit;// SRC/BAF
	
	public UnitInfo (Unit unit) {
		_type = UnitType.BAF_UNIT;
		_unit = unit;
		update();
	}
	
	/**
	 * This method has to parse the unit and update the
	 * information about it
	 */
	void update () {
		// TODO:
	}
	
	Unit unit() {
		return _unit;
	}
}

interface UnitType {
	int SRC_UNIT = 0;
	int BAF_UNIT = 1;
};
