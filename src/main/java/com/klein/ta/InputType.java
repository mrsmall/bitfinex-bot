package com.klein.ta;

public enum InputType {

	INT, DECIMAL, STRING, SERIES, TA_FUNC;

	public boolean isValide(String value) {
		try {
			if (this == INT) {
				Integer.parseInt(value);
				return true;
			} else if (this == DECIMAL) {
				Double.parseDouble(value);
				return true;
			} else
				return true;

		} catch (Exception e) {
			return false;
		}
	}

	public boolean isOptimizable(){
		return this!=STRING;
	}
}
