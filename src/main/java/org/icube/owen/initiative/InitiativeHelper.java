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
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;

public class InitiativeHelper extends TheBorg {

	/**
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
		String filterName = res.getString("Filters").substring(2, res.getString("Filters").length() - 2);
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
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug("Initiative Name : " + i.getInitiativeName());
		for (Filter ff : existingFilterList) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug(
					"Filter list being passed : " + ff.getFilterId() + " " + ff.getFilterName() + " " + ff.getFilterValues().toString());

		}
		

		return existingFilterList;
	}

	/**
	 * Returns the owners of initiative
	 * 
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @return list of employee object who are owners of the initiative
	 *  @throws SQLException - if error in getting employee list
	 */
	@SuppressWarnings("unchecked")
	public List<Employee> getOwnerOfList(int companyId, ResultSet resultMap) throws SQLException {
		List<Integer> resultList = (List<Integer>) resultMap.getObject("OwnersOf");
		List<Employee> employeeList = new ArrayList<>();
		if (!resultList.isEmpty()) {
			for (int employeeId : resultList) {
				Employee e = new Employee();
				employeeList.add(e.get(companyId, employeeId));
			}
		}
		return employeeList;
	}

	/**
	 * Retrieves the initiative count for the view initiatives page
	 * @return map of details required for the graphical representation
	 *
	 */
	public List<Map<String, Object>> getInitiativeCount(int companyId) {
		List<Map<String, Object>> initiativeCountMapList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			String query = "match (i:Init) where i.Status='Active' or i.Status='Completed' with  distinct(i.Status) as stat match (z:Init) "
					+ "with distinct(z.Category) as cat,stat match (j:Init {Category:cat}) with distinct(j.Type) as TYP,stat,cat optional "
					+ "match (a:Init) where a.Status=stat and a.Type=TYP return cat as category,TYP as initiativeType,stat as status ,count(a) as totalInitiatives";
			Statement stmt = dch.companyNeoConnectionPool.get(companyId).createStatement();
			ResultSet res = stmt.executeQuery(query);
			while (res.next()) {
				Map<String, Object> initiativeCountMap = new HashMap<>();
				initiativeCountMap.put("status", res.getString("status"));
				initiativeCountMap.put("category", res.getString("category"));
				initiativeCountMap.put("initiativeType", res.getString("initiativeType"));
				initiativeCountMap.put("totalInitiatives", res.getInt("totalInitiatives"));
				initiativeCountMapList.add(initiativeCountMap);
			}
			stmt.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).error("Exception while getting the initiative list", e);
		}
		return initiativeCountMapList;
	}

	@SuppressWarnings("unchecked")
	public List<Employee> setPartOfEmployeeList(int companyId, ResultSet res, Initiative i) throws SQLException {
		List<Employee> existingEmployeeList = (i.getPartOfEmployeeList() == null ? new ArrayList<>() : i.getPartOfEmployeeList());
		org.apache.log4j.Logger.getLogger(InitiativeHelper.class).debug("Setting part of employee list");
		List<Integer> employeeIdList = new ArrayList<>();
		employeeIdList = (List<Integer>) res.getObject("PartOfID");
		for (int employeeId : employeeIdList) {
			Employee e = new Employee();
			existingEmployeeList.add(e.get(companyId, employeeId));
		}
		return existingEmployeeList;
	}

	/**
	 * Sets the values for Metrics object
	 * @param i - Initiative object
	 * @return - List of Metrics object
	 */
	public List<Metrics> setInitiativeMetrics(int companyId, Initiative i) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Metrics> metricsList = new ArrayList<>();
		try {
			dch.getCompanyConnection(companyId);
			if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
				CallableStatement cs = dch.companySqlConnectionPool.get(companyId).prepareCall(
						"{call getIndividualInitiativeMetricValueAggregate(?)}");
				int empId = i.getPartOfEmployeeList().get(0).getEmployeeId();
				cs.setInt(1, empId);
				ResultSet rs = cs.executeQuery();
				while (rs.next()) {
					Metrics m = new Metrics();
					m.setId(rs.getInt("metric_id"));
					m.setName(rs.getString("metric_name"));
					m.setScore(rs.getInt("current_score"));
					m.setCategory("Individual");
					m.setDateOfCalculation(rs.getDate("calc_time"));
					String direction = m.calculateMetricDirection(rs.getInt("current_score"), rs.getInt("previous_score"));
					m.setDirection(direction);
					metricsList.add(m);
				}
			} else if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {

				CallableStatement cs = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getTeamInitiativeMetricValueAggregate(?)}");
				int initId = i.getInitiativeId();
				cs.setInt(1, initId);
				ResultSet rs = cs.executeQuery();
				while (rs.next()) {
					Metrics m = new Metrics();
					m.setId(rs.getInt("metric_id"));
					m.setName(rs.getString("metric_name"));
					m.setCategory("Team");
					m.setScore(rs.getInt("current_score"));
					m.setDateOfCalculation(rs.getDate("calc_time"));
					String direction = (m.calculateMetricDirection(rs.getInt("current_score"), rs.getInt("previous_score")));
					m.setDirection(direction);
					metricsList.add(m);
				}

			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(InitiativeHelper.class).error(
					"Exception while setting the metrics for initiative with ID " + i.getInitiativeId(), e);
		}
		return metricsList;

	}

}
