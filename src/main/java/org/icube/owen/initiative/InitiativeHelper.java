package org.icube.owen.initiative;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class InitiativeHelper extends TheBorg {

	/**
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 * @return - List of Filter objects
	 * @throws SQLException 
	 */
	@SuppressWarnings("unchecked")
	public List<Filter> setPartOfConnections(ResultSet res, Initiative i) throws SQLException {
		List<Filter> existingFilterList = (i.getFilterList() == null ? new ArrayList<>() : i.getFilterList());
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting part of connections");
		FilterList fl = new FilterList();
		Filter f = new Filter();
		Map<Integer, String> filterLabelMap = fl.getFilterLabelMap();
		String filterName = res.getString("Filters").substring(1, res.getString("Filters").length() - 1);
		for (Entry<Integer, String> entry : filterLabelMap.entrySet()) {
			if (filterName.equals(entry.getValue())) {
				f.setFilterId(entry.getKey());
			}
		}
		f.setFilterName(filterName);
		List<Integer> partOfIdList = (List<Integer>) res.getObject("PartOfID");
		List<String> partOfNameList = (List<String>) res.getObject("PartOfName");
		Map<Integer, String> filterValuesMap = new HashMap<>();
		for (int j = 0; j < partOfIdList.size(); j++) {
			filterValuesMap.put(partOfIdList.get(j), partOfNameList.get(j));
			f.setFilterValues(filterValuesMap);
		}

		existingFilterList.add(f);

		return existingFilterList;
	}

	/**
	 * Returns the owners of initiative
	 * 
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @return list of employee object who are owners of the initiative
	 * @throws SQLException 
	 */
	@SuppressWarnings("unchecked")
	public List<Employee> getOwnerOfList(ResultSet resultMap) throws SQLException {
		List<Integer> resultList = (List<Integer>) resultMap.getObject("OwnersOf");
		List<Employee> employeeList = new ArrayList<>();
		if (!resultList.isEmpty()) {
			for (int employeeId : resultList) {
				Employee e = new Employee();
				employeeList.add(e.get(employeeId));
			}
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
		try {
			String query = "match (i:Init) where i.Status='Active' or i.Status='Completed' with  distinct(i.Status) as stat match (z:Init) "
					+ "with distinct(z.Category) as cat,stat match (j:Init {Category:cat}) with distinct(j.Type) as TYP,stat,cat optional "
					+ "match (a:Init) where a.Status=stat and a.Type=TYP return cat as category,TYP as initiativeType,stat as status ,count(a) as totalInitiatives";
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(query);
			while (res.next()) {
				Map<String, Object> initiativeCountMap = new HashMap<>();
				initiativeCountMap.put("status", res.getString("status"));
				initiativeCountMap.put("category", res.getString("category"));
				initiativeCountMap.put("initiativeType", res.getString("initiativeType"));
				initiativeCountMap.put("totalInitiatives", res.getInt("totalInitiatives"));

				initiativeCountMapList.add(initiativeCountMap);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).error("Exception while getting the initiative list", e);
		}
		return initiativeCountMapList;
	}

	@SuppressWarnings("unchecked")
	public List<Employee> setPartOfEmployeeList(ResultSet res, Initiative i) throws SQLException {
		List<Employee> existingEmployeeList = (i.getPartOfEmployeeList() == null ? new ArrayList<>() : i.getPartOfEmployeeList());
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting part of employee list");
		List<Integer> employeeIdList = new ArrayList<>();
		employeeIdList = (List<Integer>) res.getObject("PartOfID");
		for (int employeeId : employeeIdList) {
			Employee e = new Employee();
			existingEmployeeList.add(e.get(employeeId));
		}
		return existingEmployeeList;
	}

}
