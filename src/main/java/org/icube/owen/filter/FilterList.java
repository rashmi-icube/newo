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
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

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

		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("getFilterValues method started");
			String query = "match (n:" + filterName + ") return n.Name as Name,n.Id as Id";
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			Map<Integer, String> filterValuesMap = new HashMap<>();
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				filterValuesMap.put(Integer.valueOf(resultMap.get("Id").toString()), resultMap.get("Name").toString());
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterValuesMap : " + filterValuesMap.toString());
			}
			f.setFilterValues(filterValuesMap);
			tx.success();
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("Filter : " + f.toString());

		} catch (Exception e) {
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
		for (String filterName : filterLabelMap.values()) {
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterName : " + filterName);
			Filter f = new Filter();
			f.setFilterName(filterName);

			try (Transaction tx = dch.graphDb.beginTx()) {
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("getFilterValues method started");
				String query = "match (n:" + filterName + ") return n.Name as Name,n.Id as Id";
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("query : " + query);
				Result res = dch.graphDb.execute(query);
				Map<Integer, String> filterValuesMap = new HashMap<>();
				while (res.hasNext()) {
					Map<String, Object> resultMap = res.next();
					filterValuesMap.put(Integer.valueOf(resultMap.get("Id").toString()), resultMap.get("Name").toString());
					org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterValuesMap : " + filterValuesMap.toString());

				}
				f.setFilterValues(filterValuesMap);
				tx.success();
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("Filter : " + f.toString());
				allFiltersList.add(f);

			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(FilterList.class).error("Exception in  getFilterValues for filter : " + filterName, e);

			}
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
				filterLabelMap.put(rs.getInt(1), rs.getString(2));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return filterLabelMap;
	}
}
