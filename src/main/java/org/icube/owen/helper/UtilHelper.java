package org.icube.owen.helper;

import java.util.List;

public class UtilHelper {

	public static int[] getIntArrayFromIntegerList(List<Integer> integerList) {
		int[] result = new int[integerList.size()];
		for (int i = 0; i <= integerList.size() - 1; i++) {
			result[i] = integerList.get(i);
		}
		return result;
	}
	
	public static java.sql.Date convertJavaDateToSqlDate(java.util.Date date) {
		return new java.sql.Date(date.getTime());
	}
}
