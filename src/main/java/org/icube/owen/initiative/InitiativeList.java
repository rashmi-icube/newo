package org.icube.owen.initiative;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import scala.collection.convert.Wrappers.SeqWrapper;

public class InitiativeList extends TheBorg {

	/**
	 * Retrieves the list of Initiatives along with all its attributes and connections
	 * 
	 * @return - A list of Initiatives
	 */
	public List<Initiative> getInitiativeList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String initiativeListQuery = "match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a)"
					+ " return i.Id as Id,i.Name as Name,i.StartDate as StartDate,"
					+ "i.EndDate as EndDate,collect(distinct(a.Name))as PartOf, labels(a) as Filters,"
					+ "collect(distinct (o.EmpID)) as OwnersOf,i.Comment as Comments,i.Type as Type";
			Result res = dch.graphDb.execute(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Executed query for retrieving initiative list");
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				int initiativeId = Integer.valueOf(resultMap.get("Id").toString());
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					setPartOfConnections(resultMap, i);
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					setInitiativeValues(resultMap, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}
			tx.success();

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("List of initiatives : " + initiativeList.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Exception while getting the initiative list", e);
		}
		return initiativeList;

	}

	/**
	 * @param resultMap
	 * - A map containing the Initiative attributes and connections
	 * @param i
	 * - An Initiative object
	 */
	private void setInitiativeValues(Map<String, Object> resultMap, Initiative i) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting initiative values");
		try {
			i.setInitiativeId(Integer.valueOf(resultMap.get("Id").toString()));
			i.setInitiativeName((String) resultMap.get("Name"));
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			Date startDate = sdf.parse(resultMap.get("StartDate").toString());
			Date endDate = sdf.parse(resultMap.get("EndDate").toString());

			i.setInitiativeStartDate(startDate);
			i.setInitiativeEndDate(endDate);
			i.setInitiativeComment((String) resultMap.get("Comment"));
			i.setInitiativeType((String) resultMap.get("Type"));
			i.setOwnerOf(getOwnerOfList(resultMap));

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Error in setting initiative values", e);
			
		}
		List<Filter> existingFilterList = i.getFilterList();
		existingFilterList.add(setPartOfConnections(resultMap, i));
		i.setFilterList(existingFilterList);
	}

	/**
	 * @param resultMap
	 * - A map containing the Initiative attributes and connections
	 * @param i
	 * - An Initiative object
	 */
	private Filter setPartOfConnections(Map<String, Object> resultMap, Initiative i) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting part of connections");
		Filter f = new Filter();
		f.setFilterName(resultMap.get("Filters").toString());
		SeqWrapper sw = (SeqWrapper) resultMap.get("PartOf");
		f.setFilterValues(getFilterValueMapFromResult(sw));
		return f;
	}

	/**
	 * @param resultMap
	 * - A map containing the Initiative attributes and connections
	 * @param columnName
	 * - Name of the column to iterate through from the resultMap
	 * @return - Returns a list of strings from the resultMap
	 */
	private Map<String, String> getFilterValueMapFromResult(SeqWrapper sw) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("getFilterValueMapFromResult");
		Map<String, String> result = new HashMap<>();
		Iterator iter = sw.iterator();
		int count = 0;
		while (iter.hasNext()) {
			count++;
			result.put(String.valueOf(count), (String) iter.next());
		}
		return result;
	}

	/**
	 * Returns the owners of initiative
	 * 
	 * @param resultMap
	 * @return list of employee object who are owners of the initiative
	 */
	private List<Employee> getOwnerOfList(Map<String, Object> resultMap) {
		SeqWrapper sw = (SeqWrapper) resultMap.get("OwnerOf");
		Iterator iter = sw.iterator();
		List<Employee> employeeList = new ArrayList<>();
		while (iter.hasNext()) {
			String employeeId = (String) iter.next();
			Employee e = new Employee();
			employeeList.add(e.get(employeeId));
		}
		return employeeList;
	}

}