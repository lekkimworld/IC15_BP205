package com.ontimesuite.gc.labs.bp205;

public class ArrayUtil {

	public static boolean isEmpty(Object[] array) {
		return null == array || array.length == 0;
	}
	
	public static int indexOf(Object[] array, Object lookfor) {
		if (!isEmpty(array)) {
			for (int i=0, k=array.length; i<k; i++) {
				if (array[i] == lookfor) return i;
			}
		}
		return -1;
	}
	
	public static int indexOfEquals(Object[] array, Object lookfor) {
		if (!isEmpty(array)) {
			for (int i=0, k=array.length; i<k; i++) {
				if (array[i] == null && null == lookfor) return i;
				if (null == lookfor || null == array[i]) continue;
				if (array[i].equals(lookfor)) return i;
			}
		}
		return -1;
	}
	
	public static boolean contains(Object[] array, Object lookfor) {
		int idx = indexOf(array, lookfor);
		return idx != -1;
	}
	
	public static boolean containsEquals(Object[] array, Object lookfor) {
		int idx = indexOfEquals(array, lookfor);
		return idx != -1;
	}
	
}
