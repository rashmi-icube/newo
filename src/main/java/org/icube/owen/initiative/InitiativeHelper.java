package org.icube.owen.initiative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import scala.collection.convert.Wrappers.SeqWrapper;

public class InitiativeHelper {

	/**
	 * @param resultMap
	 * - A map containing the Initiative attributes and connections
	 * @param i
	 * - An Initiative object
	 */
	public List<Filter> setPartOfConnections(Map<String, Object> resultMap, Initiative i) {
		List<Filter> existingFilterList = (i.getFilterList() == null ? new ArrayList<>() : i.getFilterList());
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting part of connections");

		Filter f = new Filter();
		f.setFilterName(resultMap.get("Filters").toString().substring(1, resultMap.get("Filters").toString().length() - 1));
		SeqWrapper swId = (SeqWrapper) resultMap.get("PartOfID");
		SeqWrapper swValue = (SeqWrapper) resultMap.get("PartOfName");
		f.setFilterValues(getFilterValueMapFromResult(swId, swValue));
		existingFilterList.add(f);

		return existingFilterList;
	}

	/**
	 * @param resultMap
	 * - A map containing the Initiative attributes and connections
	 * @param columnName
	 * - Name of the column to iterate through from the resultMap
	 * @return - Returns a list of strings from the resultMap
	 */
	public Map<String, String> getFilterValueMapFromResult(SeqWrapper swId, SeqWrapper swValue) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("getFilterValueMapFromResult");
		Map<String, String> result = new HashMap<>();
		Iterator iterId = swId.iterator();
		Iterator iterValue = swValue.iterator();

		while (iterId.hasNext() && iterValue.hasNext()) {
			result.put((String) iterId.next(), (String) iterValue.next());
		}
		return result;
	}

	/**
	 * Returns the owners of initiative
	 * 
	 * @param resultMap
	 * @return list of employee object who are owners of the initiative
	 */
	public List<Employee> getOwnerOfList(Map<String, Object> resultMap) {
		SeqWrapper sw = (SeqWrapper) resultMap.get("OwnersOf");
		Iterator iter = sw.iterator();
		List<Employee> employeeList = new ArrayList<>();
		while (iter.hasNext()) {
			String employeeId = (String) iter.next();
			Employee e = new Employee();
			employeeList.add(e.get(employeeId));
		}
		return employeeList;
	}
	
	/**
	 * Retrieves the initiative count for the view initiatives page
	 * @return map of details required for the graphical representation
	 *
	 */
	public List<Map<String, Object>> getInitiativeCount() {
		List<Map<String, Object>> initiativeCountMapList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String query = "match (i:Init) where i.Status='Active' or i.Status='Complete' return i.Category as category,"
					+ "i.Type as initiativeType,i.Status as status,count(i) as totalInitiatives";
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				initiativeCountMapList.add(resultMap);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).error("Exception while getting the initiative list", e);
		}
		return initiativeCountMapList;
	}

}