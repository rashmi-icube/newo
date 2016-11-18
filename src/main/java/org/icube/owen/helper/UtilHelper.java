package org.icube.owen.helper;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.icube.owen.filter.Filter;

public class UtilHelper {

	public static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss"; // 2016-01-01 00:00:00
	public static final String dateFormat = "yyyy-MM-dd"; // 2016-01-01;

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

	public static java.sql.Timestamp convertJavaDateToSqlTimestamp(java.util.Date date) {
		return new java.sql.Timestamp(date.getTime());
	}

	public static String getConfigProperty(String propertyName) {
		String propertyValue = "";

		Properties prop = new Properties();
		String propFileName = "config.properties";
		InputStream inputStream = UtilHelper.class.getClassLoader().getResourceAsStream(propFileName);
		if (inputStream != null) {
			try {
				prop.load(inputStream);
			} catch (IOException e) {
				org.apache.log4j.Logger.getLogger(UtilHelper.class).error("property file '" + propFileName + "' not found in classpath");
			}
			propertyValue = prop.getProperty(propertyName);
		} else {
			org.apache.log4j.Logger.getLogger(UtilHelper.class).error("property file '" + propFileName + "' not found in classpath");
		}

		try {
			inputStream.close();
		} catch (IOException e) {
			org.apache.log4j.Logger.getLogger(UtilHelper.class).error("Error while closing the inputStream");
		}
		return propertyValue;
	}

	/**
	 * Parses the filter list for metric value calculations 
	 * @param filterList - List of filter objects
	 * @return Map of values of filter object 
	 */
	public static Map<String, Object> parseFilterList(List<Filter> filterList) {
		org.apache.log4j.Logger.getLogger(UtilHelper.class).info("HashMap created!!!");
		Map<String, Object> result = new HashMap<>();

		int funcListSize = 0, posListSize = 0, zoneListSize = 0, countAll = 0, dimensionId = 0, dimensionValueId = 0, funcId = 0, posId = 0, zoneId = 0;
		for (Filter filter : filterList) {
			if (filter.getFilterValues().containsKey(0)) {
				countAll++;
			}
			if (filter.getFilterName().equalsIgnoreCase("Function")) {
				funcId = filter.getFilterValues().keySet().iterator().next();
				funcListSize = filter.getFilterValues().size();
			} else if (filter.getFilterName().equalsIgnoreCase("Position")) {
				posId = filter.getFilterValues().keySet().iterator().next();
				posListSize = filter.getFilterValues().size();
			} else if (filter.getFilterName().equalsIgnoreCase("Zone")) {
				zoneId = filter.getFilterValues().keySet().iterator().next();
				zoneListSize = filter.getFilterValues().size();
			}

			// check for if only two filter values are 0
			for (int filterValueId : filter.getFilterValues().keySet()) {
				if (filterValueId > 0) {
					dimensionId = filter.getFilterId();
					dimensionValueId = filterValueId;
				}
			}
		}

		result.put("filterList", filterList);
		result.put("funcListSize", funcListSize);
		result.put("posListSize", posListSize);
		result.put("zoneListSize", zoneListSize);
		result.put("countAll", countAll);
		result.put("dimensionId", dimensionId);
		result.put("dimensionValueId", dimensionValueId);
		result.put("funcId", funcId);
		result.put("posId", posId);
		result.put("zoneId", zoneId);

		return result;
	}

	public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columns = rsmd.getColumnCount();
		for (int x = 1; x <= columns; x++) {
			if (columnName.equals(rsmd.getColumnName(x))) {
				return true;
			}
		}
		return false;
	}

	public static Date getStartOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	public static Date getEndOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}
}