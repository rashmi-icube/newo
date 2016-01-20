package org.icube.owen.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class FilterList extends TheBorg {

	static Logger logger = ObjectFactory.getLogger("org.icube.owen.filter.FilterList");

	/**
	 * Returns a filter object of the given filterName
	 * 
	 * @param filterName
	 * @return filter object
	 */
	public Filter getFilterValues(String filterName) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		logger.debug("filterName : " + filterName);
		Filter f = new Filter();
		f.setFilterName(filterName);

		try (Transaction tx = dch.graphDb.beginTx()) {
			logger.debug("getFilterValues method started");
			String query = "match (n:<<filterName>>) return n.Name as Name,n.Id as Id";
			query = query.replace("<<filterName>>", filterName);
			logger.debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			Map<String, String> filterValuesMap = new HashMap<String, String>();
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				filterValuesMap.clear();
				filterValuesMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
				logger.debug("filterValuesMap : " + filterValuesMap.toString());
			}
			f.setFilterValues(filterValuesMap);
			tx.success();
			logger.debug("Filter : " + f.toString());

		} catch (Exception e) {
			logger.error("Exception in  getFilterValues for filter : " + filterName, e);

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
		List<String> filterLabelList = getFilterLabels();
		for (String filterName : filterLabelList) {
			logger.debug("filterName : " + filterName);
			Filter f = new Filter();
			f.setFilterName(filterName);

			try (Transaction tx = dch.graphDb.beginTx()) {
				logger.debug("getFilterValues method started");
				String query = "match (n:<<filterName>>) return n.Name as Name,n.Id as Id";
				query = query.replace("<<filterName>>", filterName);
				logger.debug("query : " + query);
				Result res = dch.graphDb.execute(query);
				Map<String, String> filterValuesMap = new HashMap<String, String>();
				while (res.hasNext()) {
					Map<String, Object> resultMap = res.next();
					filterValuesMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
					logger.debug("filterValuesMap : " + filterValuesMap.toString());

				}
				f.setFilterValues(filterValuesMap);
				tx.success();
				logger.debug("Filter : " + f.toString());
				allFiltersList.add(f);

			} catch (Exception e) {
				logger.error("Exception in  getFilterValues for filter : " + filterName, e);

			}
		}
		return allFiltersList;
	}

	/**
	 * Returns the list of filter labels
	 * 
	 * @return filterLabelList
	 */
	// TODO change this to a query from SQL once db is setup
	public List<String> getFilterLabels() {
		List<String> filterLabelList = new ArrayList<>();
		filterLabelList.add("Function");
		filterLabelList.add("Zone");
		filterLabelList.add("Position");
		return filterLabelList;
	}
}
