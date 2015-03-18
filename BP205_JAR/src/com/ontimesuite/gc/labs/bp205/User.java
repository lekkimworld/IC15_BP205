package com.ontimesuite.gc.labs.bp205;

import java.io.Serializable;

import dk.intravision.json.Json.JValue;

public class User extends BaseJsonObject implements Serializable {
	// declarations
	private String key = null;
	private String guid = null;
	private String uid = null;
	private String email = null;
	private boolean external = false;

	
	public User(JValue obj) {
		this.key = this.getValueIfPresent(obj, "Key");
		this.email = this.getValueIfPresent(obj, "Email");
	}
	
	public User(String key, String guid, String uid, String email, boolean external) {
		this.key = key;
		this.guid = guid;
		this.uid = uid;
		this.email = email;
		this.external = external;
	}

	public String getKey() {
		return key;
	}

	public String getGUID() {
		return guid;
	}
	
	public String getUid() {
		return uid;
	}

	public String getEmail() {
		return email;
	}

	public boolean isExternal() {
		return this.external;
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj || !(obj instanceof User)) return false;
		return ((User)obj).getKey().equals(this.key);
	}

	@Override
	public String toString() {
		return "[USER - email<" + email + "> guid<" + guid + ">  uid<" +uid + "> key<" + key + ">]";
	}

}
