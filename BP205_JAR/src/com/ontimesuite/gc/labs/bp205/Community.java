package com.ontimesuite.gc.labs.bp205;

import dk.intravision.json.Json.JValue;

public class Community {
	// declarations
	private String id = null;
	private String name = null;
	
	public Community(JValue obj) {
		this.id = obj.get("ID").getStr();
		if (!obj.get("Name").isNothing()) this.name = obj.get("Name").getStr();
	}
	
	public Community(String id) {
		this.id = id;
	}

	public Community(String id, String name) {
		this(id);
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj || !(obj instanceof Community)) return false;
		return this.id.equals(((Community)obj).getId());
	}

	@Override
	public String toString() {
		return "[COMMUNITTY - id<" + id + "> name <" + name + ">]";
	}
	
}
