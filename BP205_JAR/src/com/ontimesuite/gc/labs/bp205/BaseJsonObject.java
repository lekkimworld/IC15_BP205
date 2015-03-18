package com.ontimesuite.gc.labs.bp205;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import dk.intravision.json.Json.JValue;

public abstract class BaseJsonObject {
	// constants
	private static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	// declarations
	protected SimpleDateFormat format = null; 
	
	{
		format = new SimpleDateFormat(DATEFORMAT);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public BaseJsonObject() {
		
	}
	
	protected String getValueIfPresent(JValue obj, String key) {
		if (obj.get(key).isNothing()) return null;
		String value = obj.get(key).getStr();
		if (StringUtil.isEmpty(value, true)) return null;
		return value;
	}
	
	protected boolean getValueIfPresentBool(JValue obj, String key, boolean defaultValue) {
		String s = this.getValueIfPresent(obj, key);
		if (null == s) return defaultValue;
		try {
			return Boolean.parseBoolean(s);
		} catch (Throwable t) {
			return defaultValue;
		}
	}
	
	protected Date getValueIfPresentDate(JValue obj, String key) {
		String s = this.getValueIfPresent(obj, key);
		if (null == s) return null;
		try {
			return this.format.parse(s);
		} catch (Throwable t) {
			return null;
		}
	}

}
