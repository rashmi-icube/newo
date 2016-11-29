package org.icube.owen.initiative;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsHelper;

public class InitiativeHelper extends TheBorg {

	/**
	 * @param companyId - Company ID
	 * @param res - A resultset containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 * @return - List of Filter objects
	 * @throws SQLException - if partOf connections are not set
	 */

	@SuppressWarnings("unchecked")
	public List<Filter> setPartOfConnections(int companyId, ResultSet res, Initiative i) throws SQLException {
		List<Filter> existingFilterList = (i.getFilterList() == null ? new ArrayList<>() : i.getFilterList());
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug("Setting part of connections");
		FilterList fl = new FilterList();
		Filter f = new Filter();
		Map<Integer, String> filterLabelMap = fl.getFilterLabelMap(companyId);
		// substring-ing it to get rid of [] in the list
		String filterName = res.getString("Filters").substring(2, res.getString("Filters").length() - 2);
		for (Entry<Integer, String> entry : filterLabelMap.entrySet()) {
			if (filterName.equals(entry.getValue())) {
				f.setFilterId(entry.getKey());
			}
		}
		f.setFilterName(filterName);
		List<Integer> partOfIdList = (List<Integer>) res.getObject("PartOfID");
		List<String> partOfNameList = (List<String>) res.getObject("PartOfName");
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).info("HashMap created!!!");
		Map<Integer, String> filterValuesMap = new HashMap<>();
		for (int j = 0; j < partOfIdList.size(); j++) {
			filterValuesMap.put(partOfIdList.get(j), partOfNameList.get(j));
			f.setFilterValues(filterValuesMap);
		}

		existingFilterList.add(f);
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug("Initiative Name : " + i.getInitiativeName());
		for (Filter ff : existingFilterList) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug(
					"Filter list being passed : " + ff.getFilterId() + " " + ff.getFilterName() + " " + ff.getFilterValues().toString());

		}

		return existingFilterList;
	}

	/**
	 * Returns the owners of initiative
	 * @param companyId - Company ID
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @return list of employee object who are owners of the initiative
	 *  @throws SQLException - if error in getting employee list
	 */

	@SuppressWarnings("unchecked")
	public List<Employee> getOwnerOfList(int companyId, ResultSet resultMap) throws SQLException {
		List<Integer> resultList = (List<Integer>) resultMap.getObject("OwnersOf");
		List<Employee> employeeList = new ArrayList<>();
		if (!resultList.isEmpty()) {
			EmployeeList el = new EmployeeList();
			employeeList = el.get(companyId, resultList);
		}
		return employeeList;
	}

	/**
	 * Retrieves the initiative count for the view initiatives page
	 * @param companyId - companyId for the db connection
	 * @return map of details required for the graphical representation
	 *
	 */
	public List<Map<String, Object>> getInitiativeCount(int companyId) {
		List<Map<String, Object>> initiativeCountMapList = new ArrayList<>();
		// creating a master map of all types of initiatives if in case there are no initiatives to be returned
		// atleast the initiative types + the count of 0 will be returned to display on the UI
		Map<String, Map<String, Object>> masterMap = getEmptyInitiativeCountMap(companyId, "Team");
		masterMap.putAll(getEmptyInitiativeCountMap(companyId, "Individual"));
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement()) {
			String query = "match (i:Init) where i.Status='Active' or i.Status='Completed' with  distinct(i.Status) as stat match (z:Init) "
					+ "with distinct(z.Category) as cat,stat match (j:Init {Category:cat}) with distinct(j.Type) as TYP,stat,cat optional "
					+ "match (a:Init) where a.Status=stat and a.Type=TYP return cat as category,TYP as initiativeType,stat as status ,count(a) as totalInitiatives";
			try (ResultSet res = stmt.executeQuery(query)) {
				while (res.next()) {
					String key = res.getString("initiativeType") + "_" + res.getString("status");
					Map<String, Object> initiativeCountMap = masterMap.get(key);
					initiativeCountMap.put("status", res.getString("status"));
					initiativeCountMap.put("category", res.getString("category"));
					initiativeCountMap.put("initiativeType", res.getInt("initiativeType"));
					initiativeCountMap.put("totalInitiatives", res.getInt("totalInitiatives"));
					masterMap.put(key, initiativeCountMap);
				}
			}
			initiativeCountMapList.addAll(masterMap.values());
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).error("Exception while getting the initiative count list", e);
		}
		return initiativeCountMapList;
	}

	/**
	 * Creates a list of maps for all initiative types with the count as 0
	 * Useful for when there is no initiative of a specific type and doesn't come up in the 
	 * result for the query from neo4j for display on the list of initiative count in view initiative list
	 * @param companyId - companyId for the db connection
	 * @param category - Team / Individual
	 * @return empty initiative count map
	 */
	private Map<String, Map<String, Object>> getEmptyInitiativeCountMap(int companyId, String category) {
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).info("HashMap created!!!");
		Map<String, Map<String, Object>> initiativeCountMasterMap = new HashMap<>();
		Initiative i = new Initiative();
		Map<Integer, String> initiativeTypeMap = i.getInitiativeTypeMap(companyId, category);
		for (int initiativeTypeId : initiativeTypeMap.keySet()) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).info("HashMap created!!!");
			Map<String, Object> m = new HashMap<>();
			m.put("status", "Completed");
			m.put("category", category);
			m.put("initiativeType", initiativeTypeId);
			m.put("totalInitiatives", 0);
			initiativeCountMasterMap.put(initiativeTypeId + "_Completed", m);
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).info("HashMap created!!!");
			m = new HashMap<>();
			m.put("status", "Active");
			m.put("category", category);
			m.put("initiativeType", initiativeTypeId);
			m.put("totalInitiatives", 0);
			initiativeCountMasterMap.put(initiativeTypeId + "_Active", m);
		}
		return initiativeCountMasterMap;
	}

	/**
	 * Retrieves the list of existing partOfEmployee list
	 * @param companyId - Company ID
	 * @param res - A resultset containing the Initiative attributes and connections
	 * @param i - An initiative object
	 * @return List of employee objects
	 * @throws SQLException - if the partOfEmployee list is not retrieved 
	 */
	@SuppressWarnings("unchecked")
	public List<Employee> setPartOfEmployeeList(int companyId, ResultSet res, Initiative i) throws SQLException {
		List<Employee> existingEmployeeList = (i.getPartOfEmployeeList() == null ? new ArrayList<>() : i.getPartOfEmployeeList());
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug("Setting part of employee list");
		List<Integer> employeeIdList = (List<Integer>) res.getObject("PartOfID");
		EmployeeList el = new EmployeeList();
		existingEmployeeList = el.get(companyId, employeeIdList);
		return existingEmployeeList;
	}

	/**
	 * Sets the values for Metrics object
	 * @param companyId - Company ID
	 * @param i - Initiative object
	 * @return - List of Metrics object
	 */

	public List<Metrics> setInitiativeMetrics(int companyId, Initiative i) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Metrics> metricsList = new ArrayList<>();
		MetricsHelper mh = new MetricsHelper();
		try {
			dch.getCompanyConnection(companyId);
			if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
				try (CallableStatement cs = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
						"{call getIndividualInitiativeMetricValueAggregate(?)}")) {
					int empId = i.getPartOfEmployeeList().get(0).getEmployeeId();
					cs.setInt(1, empId);
					try (ResultSet rs = cs.executeQuery()) {
						metricsList = mh.fillMetricsData(companyId, rs, mh.getPrimaryMetricMap(companyId, i.getInitiativeTypeId()), "Individual");
					}
				}

			} else if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
				org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug(
						"setInitiativeMetrics for team  calling procedure getTeamInitiativeMetricValueAggregate for initiative ID: "
								+ i.getInitiativeId());
				try (CallableStatement cs = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
						"{call getTeamInitiativeMetricValueAggregate(?)}")) {
					int initId = i.getInitiativeId();
					cs.setInt(1, initId);
					try (ResultSet rs = cs.executeQuery()) {
						org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug("fill metric map for initiative : " + i.getInitiativeId());
						metricsList = mh.fillMetricsData(companyId, rs, mh.getPrimaryMetricMap(companyId, i.getInitiativeTypeId()), "Team");
						org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug(
								"finished fill metric map for initiative : " + i.getInitiativeId());
					}

				}

			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).error(
					"Exception while setting the metrics for initiative with ID " + i.getInitiativeId(), e);
		}
		return metricsList;

	}

}
