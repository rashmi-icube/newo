package org.icube.owen.filter;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class FilterList extends TheBorg {

	/**
	 * Returns a filter object of the given filterName
	 * 
	 * @param filterName  - Name of the filter for which all values are to be returned
	 * @return filter object - A filter object of the given filterName
	 */
	public Filter getFilterValues(String filterName) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterName : " + filterName);
		Filter f = new Filter();
		f.setFilterName(filterName);
		Map<Integer, String> filterLabelMap = getFilterLabelMap();
		for (int filterId : filterLabelMap.keySet()) {
			if (filterLabelMap.get(filterId).equalsIgnoreCase(filterName)) {
				f.setFilterId(filterId);
			}
		}
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getDimensionValue(?)}");
			cstmt.setInt(1, f.getFilterId());
			ResultSet rs = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("getFilterValues method started");
			Map<Integer, String> filterValuesMap = new HashMap<>();
			filterValuesMap.put(0, "All");
			while (rs.next()) {
				filterValuesMap.put(rs.getInt("dimension_val_id"), rs.getString("dimension_val_name"));
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterValuesMap : " + filterValuesMap.toString());
			}
			f.setFilterValues(filterValuesMap);
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("Filter : " + f.toString());

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(FilterList.class).error("Exception in  getFilterValues for filter : " + filterName, e);

		}
		return f;
	}

	/**
	 * Retrieves all the objects which are filters
	 * 
	 * @return returns a list of all filter objects
	 */
	public List<Filter> getFilterValues() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		List<Filter> allFiltersList = new ArrayList<>();

		Map<Integer, String> filterLabelMap = getFilterLabelMap();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getDimensionValueList()}");
			ResultSet rs = cstmt.executeQuery();
			for (int filterId : filterLabelMap.keySet()) {
				Filter f = new Filter();
				String filterName = filterLabelMap.get(filterId);
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterName : " + filterName);

				f.setFilterId(filterId);
				f.setFilterName(filterName);
				Map<Integer, String> filterValuesMap = new HashMap<>();
				filterValuesMap.put(0, "All");
				while (rs.next()) {
					if (filterId == rs.getInt("dimension_id")) {
						filterValuesMap.put(rs.getInt("dimension_val_id"), rs.getString("dimension_val_name"));
					}
				}

				f.setFilterValues(filterValuesMap);
				org.apache.log4j.Logger.getLogger(FilterList.class).debug(
						f.getFilterId() + " - " + f.getFilterName() + " - " + f.getFilterValues().toString());
				allFiltersList.add(f);
				rs.first();

			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(FilterList.class).error("Exception while getting dimension value list : ", e);
		}
		return allFiltersList;
	}

	/**
	 * Returns the map of filter labels
	 * 
	 * @return filterLabelMap - A map of filter labels
	 */
	public Map<Integer, String> getFilterLabelMap() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> filterLabelMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getDimensionList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				filterLabelMap.put(rs.getInt("dimension_id"), rs.getString("dimension_name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return filterLabelMap;
	}
}
