package com.ontimesuite.gc.labs.bp205;

public enum SERVICE {
	CONNECTIONS, OAUTH2, PROFILES, COMMUNITIES;
	
	public static SERVICE getService(String s) {
		for (SERVICE serv : SERVICE.values()) {
			if (s.equalsIgnoreCase(serv.name())) return serv;
		}
		return null;
	}
}
