package com.ontimesuite.gc.labs.bp205;

// constants
public enum ACTION {ACCEPTED("A"), REJECTED("R");
	private String value = null;
	private ACTION(String value) {
		this.value = value;
	}
	public String getStringValue() {
		return this.value;
	}
	public static ACTION getAction(String s) {
		for (ACTION action : ACTION.values()) {
			if (action.getStringValue().equals(s)) return action;
		}
		return null;
	}
}