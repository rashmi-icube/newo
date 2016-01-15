package org.icube.owen.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class FilterList extends TheBorg{
	
	
	static DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
	
	public static void main(String[] args) {
		try (Transaction tx = dch.graphDb.beginTx()) {
			String filter = "Function";
			FilterList fl = new FilterList();
			fl.getFilterValues(filter);
			tx.success();
		}

		dch.shutDown();
	}

	//TODO retrieve a list of filters without any specific filterName - overload getFilterValues function

	
	/**
	 * Retrieves all the objects which belong to a particular filter item
	 * 
	 * @param filterName - Name of the filter that needs to be populated [Function, Zone, Position]
	 * @return returns the list based on filter name
	 */
//TODO always return all the filter/dimensions don't get filtername from user
	public Filter getFilterValues(String filterName) {
		org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterName : " + filterName);
		Filter f = new Filter();
		f.setFilterName(filterName);
		
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("getFilterValues method started");
			String query = "match (n:<<filterName>>) return n.Name as Name,n.Id as Id";
			query = query.replace("<<filterName>>", filterName);
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			List<Map<String, String>> filterValuesMapList = new ArrayList<>();
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				Map<String, String> filterValuesMap = new HashMap<String, String>();
				filterValuesMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
				org.apache.log4j.Logger.getLogger(FilterList.class).debug("filterValuesMap : " + filterValuesMap.toString());
				filterValuesMapList.add(filterValuesMap);
			}
			f.setFilterValues(filterValuesMapList);
			tx.success();
			org.apache.log4j.Logger.getLogger(FilterList.class).debug("Filter : " + f.toString());
			return f;
		}
	}
	

}
