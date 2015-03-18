package com.ontimesuite.gc.labs.bp205;

public class StringUtil {

	public static boolean isEmpty(String s) {
		return isEmpty(s, false);
	}
	
	public static boolean isEmpty(String s, boolean trim) {
		if (null == s) return true;
		if (trim) return s.trim().length() == 0;
		return s.length() == 0;
	}
	
	public static String getSubstring(String source, String s1, String s2) {
		if (isEmpty(source) || isEmpty(s1) || isEmpty(s2)) return source;
		int idx1 = source.indexOf(s1);
		if (idx1 < 0) return "";
		int idx2 = source.indexOf(s2, idx1);
		if (idx2 < 0) return "";
		return source.substring(idx1+s1.length(), idx2);
	}

}
