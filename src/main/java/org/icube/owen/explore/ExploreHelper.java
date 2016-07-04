package org.icube.owen.explore;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsHelper;

public class ExploreHelper extends TheBorg {

	/**
	 * Retrieves data for metrics 
	 * @param companyId - Company ID of the employee
	 * @param teamListMap - Map with the (teamName, filterList) pair, can have as many teams as desired by the UI
	 * @return metricsMapList - Map with (teamName, metricList) pair
	 */
	public Map<String, List<Metrics>> getTeamMetricsData(int companyId, Map<String, List<Filter>> teamListMap) {

		Map<String, List<Metrics>> result = new HashMap<>();

		for (String teamName : teamListMap.keySet()) {
			List<Metrics> metricList = new ArrayList<>();
			List<Filter> filterList = teamListMap.get(teamName);
			try {
				MetricsHelper mh = new MetricsHelper();
				metricList = mh.getTeamMetricsList(companyId, 0, UtilHelper.parseFilterList(filterList), false);
			} catch (SQLException e) {
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while getting team metrics data : " + teamListMap.toString(),
						e);
			}
			result.put(teamName, metricList);
		}
		return result;
	}

	/**
	 * Retrieves data for the time series graph 
	 * @param companyId - Company ID of the employee
	 * @param teamListMap - Map with the (teamName, filterList) pair, can have as many teams as desired by the UI
	 * @return metricsMapList - Map with (teamName, metricList) pair
	 */
	public Map<String, Map<Integer, List<Map<Date, Integer>>>> getTeamTimeSeriesGraph(int companyId, Map<String, List<Filter>> teamListMap) {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<String, Map<Integer, List<Map<Date, Integer>>>> result = new HashMap<>();

		for (String teamName : teamListMap.keySet()) {
			Map<Integer, List<Map<Date, Integer>>> timeSeriesMap = new HashMap<>();
			List<Filter> filterList = teamListMap.get(teamName);
			Map<String, Object> parsedFilterListResult = UtilHelper.parseFilterList(filterList);
			try {
				if ((int) parsedFilterListResult.get("countAll") == 3) {
					// if all selections are ALL then it is a organizational team metric
					CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
							"{call getOrganizationMetricTimeSeries()}");
					ResultSet rs = cstmt.executeQuery();
					timeSeriesMap = getTimeSeriesMap(rs);

				} else if ((int) parsedFilterListResult.get("countAll") == 2) {
					// if two of the filters are ALL then it is a dimension metric
					CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
							"{call getDimensionMetricTimeSeries(?)}");
					cstmt.setInt(1, (int) parsedFilterListResult.get("dimensionValueId"));
					ResultSet rs = cstmt.executeQuery();
					timeSeriesMap = getTimeSeriesMap(rs);
				} else if ((int) parsedFilterListResult.get("countAll") == 0) {
					// if none of the filters is ALL then it is a cube metric
					CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
							"{call getTeamMetricTimeSeries(?,?,?)}");
					cstmt.setInt(1, (int) parsedFilterListResult.get("funcId"));
					cstmt.setInt(2, (int) parsedFilterListResult.get("posId"));
					cstmt.setInt(3, (int) parsedFilterListResult.get("zoneId"));
					ResultSet rs = cstmt.executeQuery();
					timeSeriesMap = getTimeSeriesMap(rs);

				} else {
					org.apache.log4j.Logger.getLogger(ExploreHelper.class).info(
							"No time series graph to be displayed for the selection : " + filterList.get(0).getFilterName() + " - "
									+ filterList.get(0).getFilterValues().toString() + " ; " + filterList.get(1).getFilterName() + " - "
									+ filterList.get(1).getFilterValues().toString() + " ; " + filterList.get(2).getFilterName() + " - "
									+ filterList.get(2).getFilterValues().toString() + " ; ");
				}
			} catch (SQLException e) {
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while getting team metrics data : " + teamListMap.toString(),
						e);
			}
			result.put(teamName, timeSeriesMap);
		}

		return result;
	}

	/**
	 * Retrieves the individual metrics data
	 * @param companyId - Company ID of the employee
	 * @param employeeList - list of employees selected
	 * @return - map of employee linked to a list of metrics
	 */
	public Map<Employee, List<Metrics>> getIndividualMetricsData(int companyId, List<Employee> employeeList) {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Employee, List<Metrics>> result = new HashMap<>();
		try {
			dch.getCompanyConnection(companyId);
			for (Employee e : employeeList) {
				List<Metrics> metricsList = new ArrayList<>();
				CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getIndividualMetricValue(?)}");
				cstmt.setInt(1, e.getEmployeeId());
				ResultSet rs = cstmt.executeQuery();
				MetricsHelper mh = new MetricsHelper();
				metricsList = mh.fillMetricsData(companyId, rs, null, "Individual");
				result.put(e, metricsList);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while retrieving individual metrics data", e);
		}
		return result;

	}

	/**
	 * Retrieves the individual time series graph data
	 * @param companyId - Company ID of the employee
	 * @param employeeList - list of employees selected
	 * @return map of employee linked to a list of metrics
	 */
	public Map<Employee, Map<Integer, List<Map<Date, Integer>>>> getIndividualTimeSeriesGraph(int companyId, List<Employee> employeeList) {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Employee, Map<Integer, List<Map<Date, Integer>>>> result = new HashMap<>();

		try {
			dch.getCompanyConnection(companyId);
			for (Employee e : employeeList) {
				Map<Integer, List<Map<Date, Integer>>> metricsList = new HashMap<>();
				CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
						"{call getIndividualMetricTimeSeries(?)}");
				cstmt.setInt(1, e.getEmployeeId());
				ResultSet rs = cstmt.executeQuery();
				metricsList = getTimeSeriesMap(rs);
				result.put(e, metricsList);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while retrieving individual metrics data", e);
		}
		return result;

	}

	/**
	 * Retrieves the Time series map
	 * @param rs - Resultset
	 * @return A map of metric ID and list of calculation date and and respective score
	 * @throws SQLException - if the time series map is not retrieved
	 */
	public Map<Integer, List<Map<Date, Integer>>> getTimeSeriesMap(ResultSet rs) throws SQLException {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();

		while (rs.next()) {
			if (result.containsKey(rs.getInt("metric_id"))) {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				Map<Date, Integer> metricScoreMap = new HashMap<>();
				metricScoreMapList = result.get(rs.getInt("metric_id"));
				metricScoreMap.put(rs.getDate("calc_time"), rs.getInt("Score"));
				metricScoreMapList.add(metricScoreMap);
				result.put(rs.getInt("metric_id"), metricScoreMapList);
			} else {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				Map<Date, Integer> metricScoreMap = new HashMap<>();
				metricScoreMap.put(rs.getDate("calc_time"), rs.getInt("Score"));
				metricScoreMapList.add(metricScoreMap);
				result.put(rs.getInt("metric_id"), metricScoreMapList);
			}
		}
		return result;
	}

	/**
	 * Get the node list and edge list for team network diagram
	 * @param companyId - Company ID of the employee
	 * @param teamListMap - List of filters which define the team
	 * @param relationshipType - Map of relationship id and name
	 * @return map with node list and edge list
	 */
	public Map<String, List<?>> getTeamNetworkDiagram(int companyId, Map<String, List<Filter>> teamListMap, Map<Integer, String> relationshipType) {
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("Entering getTeamNetworkDiagram method");
		Map<String, List<?>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		String query = "";
		List<Node> nodeList = new ArrayList<>();
		List<Edge> edgeList = new ArrayList<>();
		for (String teamName : teamListMap.keySet()) {
			List<Filter> filterList = teamListMap.get(teamName);
			List<Integer> funcList = new ArrayList<>(), posList = new ArrayList<>(), zoneList = new ArrayList<>();

			for (Filter filter : filterList) {
				if (filter.getFilterName().equalsIgnoreCase("Function")) {
					funcList.add(filter.getFilterValues().keySet().iterator().next());
				} else if (filter.getFilterName().equalsIgnoreCase("Position")) {
					posList.add(filter.getFilterValues().keySet().iterator().next());
				} else if (filter.getFilterName().equalsIgnoreCase("Zone")) {
					zoneList.add(filter.getFilterValues().keySet().iterator().next());
				}
			}

			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug(
					"Filter list for " + teamName + " : " + " Function : " + funcList.toString() + " Position : " + posList.toString() + " Zone : "
							+ zoneList.toString());
			String funcQuery = "", posQuery = "", zoneQuery = "";
			if (funcList.contains(0)) {
				funcQuery = "";
			} else {
				funcQuery = "f.Id = " + funcList.get(0);
			}

			if (zoneList.contains(0)) {
				zoneQuery = "";
			} else {
				zoneQuery = "z.Id = " + zoneList.get(0);
			}

			if (posList.contains(0)) {
				posQuery = "";
			} else {
				posQuery = "p.Id = " + posList.get(0);
			}

			String subQuery = "match (a:Employee)-[:has_functionality]->(f:Function), (p:Position)<-[:is_positioned]-(a)-[:from_zone]->(z:Zone) "
					+ ((!zoneQuery.isEmpty() || !funcQuery.isEmpty() || !posQuery.isEmpty()) ? " where " : "")
					+ (zoneQuery.isEmpty() ? "" : (zoneQuery + ((!funcQuery.isEmpty() || !posQuery.isEmpty() ? " and " : ""))))
					+ (funcQuery.isEmpty() ? "" : funcQuery + (!posQuery.isEmpty() ? " and " : ""))
					+ (posQuery.isEmpty() ? "" : (posQuery))
					+ "  return a.emp_id as emp_id, a.FirstName as firstName ,a.LastName as lastName,f.Name as funcName,p.Name as posName,z.Name as zoneName, '"
					+ teamName + "' as team";

			query = query.isEmpty() ? subQuery : query + " union " + subQuery;

			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("getTeamNetworkDiagram subQuery for team " + teamName + " : " + subQuery);

		}

		try {
			List<Integer> empIdList = new ArrayList<>();
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("getTeamNetworkDiagram query for all teams  : " + query);
			Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement();
			ResultSet res = stmt.executeQuery(query);
			while (res.next()) {
				empIdList.add(res.getInt("emp_id"));
				Node n = new Node();
				n.setEmployeeId(res.getInt("emp_id"));
				/*CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();
				while(rs.next()){
					dch.setCompanyConfigDetails(companyId, dch.companyConfigMap.get(companyId), rs);
				}*/
                
				if (dch.companyConfigMap.get(companyId).isDisplayNetworkName()) {
					n.setFirstName(res.getString("firstName"));
					n.setLastName(res.getString("lastName"));
				}
				n.setFunction(res.getString("funcName"));
				n.setZone(res.getString("zoneName"));
				n.setPosition(res.getString("posName"));
				n.setTeamName(res.getString("team"));
				nodeList.add(n);
			}
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("Node list size : " + nodeList.size());

			edgeList = getEdges(companyId, empIdList, relationshipType);
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("Edge list size : " + edgeList.size());
			stmt.close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Error while retrieving team networks diagram", e);
		}

		result.put("nodeList", nodeList);
		result.put("edgeList", edgeList);
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("Exiting getTeamNetworkDiagram method");
		return result;

	}

	/**
	 * Get the node list and edge list for the individual network diagram
	 * @param companyId - Company ID
	 * @param employeeList - List of employee objects
	 * @param relationshipTypeMap - Map of relationship id and name
	 * @return map with node list and edge list
	 */
	public Map<String, List<?>> getIndividualNetworkDiagram(int companyId, List<Employee> employeeList, Map<Integer, String> relationshipTypeMap) {
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("Entering getIndividualNetworkDiagram method");
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<String, List<?>> result = new HashMap<>();
		List<Node> nodeList = new ArrayList<>();
		List<Edge> edgeList = new ArrayList<>();
		List<Integer> employeeIdList = new ArrayList<>();
		String relationshipType = "";

		for (Employee e : employeeList) {
			employeeIdList.add(e.getEmployeeId());
		}

		for (int relationshipTypeId : relationshipTypeMap.keySet()) {
			relationshipType = relationshipType.isEmpty() ? relationshipTypeMap.get(relationshipTypeId) : relationshipType + " | "
					+ relationshipTypeMap.get(relationshipTypeId);
		}

		String query = "match (a:Employee)-[:has_functionality]->(f:Function),(p:Position)<-[:is_positioned]-(a)-[:from_zone]->(z:Zone) where a.emp_id in "
				+ employeeIdList
				+ " return a.emp_id as emp_id,a.FirstName as firstName,a.LastName as lastName,0 as degree,f.Name as funcName,p.Name as posName,z.Name as zoneName order by emp_id "
				+ "union "
				+ "match (a:Employee)-[r:"
				+ relationshipType
				+ "]-(b:Employee)-[:has_functionality]->(f:Function),(p:Position)<-[:is_positioned]-(b)-[:from_zone]->(z:Zone)"
				+ " where a.emp_id in "
				+ employeeIdList
				+ " and not  b.emp_id in "
				+ employeeIdList
				+ " return b.emp_id as emp_id,b.FirstName as firstName,b.LastName as lastName,1 as degree,f.Name as funcName,p.Name as posName,z.Name as zoneName order by emp_id"
				+ " union "
				+ "match (x:Employee)-[r:"
				+ relationshipType
				+ "]-(y:Employee)"
				+ " where x.emp_id in "
				+ employeeIdList
				+ " with collect(y) as firstdegree"
				+ " match (a:Employee)-[r:"
				+ relationshipType
				+ "]-(b:Employee)-[r1:"
				+ relationshipType
				+ "]-(c:Employee)-[:has_functionality]->(f:Function),(p:Position)<-[:is_positioned]-(c)-[:from_zone]->(z:Zone)"
				+ " where a.emp_id in "
				+ employeeIdList
				+ " and a<>b and not  c.emp_id in "
				+ employeeIdList
				+ " and b<>c and not(c in firstdegree)"
				+ " return c.emp_id as emp_id,c.FirstName as firstName,c.LastName as lastName,2 as degree,f.Name as funcName,p.Name as posName,z.Name as zoneName order by emp_id";

		try {
			List<Integer> empIdList = new ArrayList<>();
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("getIndividualNetworkDiagram query  : " + query);
			Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement();
			ResultSet res = stmt.executeQuery(query);
			while (res.next()) {
				empIdList.add(res.getInt("emp_id"));
				Node n = new Node();
				n.setEmployeeId(res.getInt("emp_id"));
				/*CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();
				while(rs.next()){
					dch.setCompanyConfigDetails(companyId, dch.companyConfigMap.get(companyId), rs);
				}*/
                
				if (dch.companyConfigMap.get(companyId).isDisplayNetworkName()) {
					n.setFirstName(res.getString("firstName"));
					n.setLastName(res.getString("lastName"));
				}
				n.setFunction(res.getString("funcName"));
				n.setZone(res.getString("zoneName"));
				n.setPosition(res.getString("posName"));
				n.setConnectedness(res.getInt("degree"));
				nodeList.add(n);
			}

			edgeList = getEdges(companyId, empIdList, relationshipTypeMap);
			stmt.close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Error while retrieving individual networks diagram", e);
		}

		result.put("nodeList", nodeList);
		result.put("edgeList", edgeList);
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("Entering getIndividualNetworkDiagram method");
		return result;
	}

	/**
	 * Retrieves the list of Edge objects
	 * @param companyId - Company ID
	 * @param employeeIdList - List of employee ID's
	 * @param relationshipTypeMap - Map of relationship type ID and name
	 * @return A list of Edge objects
	 */
	public List<Edge> getEdges(int companyId, List<Integer> employeeIdList, Map<Integer, String> relationshipTypeMap) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Edge> result = new ArrayList<>();
		String relationshipType = "";

		for (int relationshipTypeId : relationshipTypeMap.keySet()) {
			relationshipType = relationshipType.isEmpty() ? relationshipTypeMap.get(relationshipTypeId) : relationshipType + " | "
					+ relationshipTypeMap.get(relationshipTypeId);
		}
		String query = "match (a:Employee)-[r:" + relationshipType + "]->(b:Employee) where a.emp_id in " + employeeIdList.toString()
				+ "  and b.emp_id in " + employeeIdList.toString() + " and a<>b "
				+ "return a.emp_id as from ,b.emp_id as to,type(r) as rel_type,r.weight as weight";

		org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("getEdges query for all teams  : " + query);
		try {
			Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement();
			ResultSet res = stmt.executeQuery(query);
			while (res.next()) {
				Edge e = new Edge();
				e.setFromEmployeId(res.getInt("from"));
				e.setToEmployeeId(res.getInt("to"));
				e.setRelationshipType(res.getString("rel_type"));
				e.setWeight(res.getDouble("weight"));
				result.add(e);

			}
			stmt.close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception whil getting edgeList", e);
		}

		return result;

	}

	/**
	 * Returns a map of relationship type ID + relationship type Name
	 * @param companyId - Company ID
	 * @return relationshipTypeMap
	 */
	public Map<Integer, String> getRelationshipTypeMap(int companyId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> relationshipTypeMap = new HashMap<>();
		try {
			dch.getCompanyConnection(companyId);
			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getRelationTypeList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				relationshipTypeMap.put(rs.getInt("rel_id"), rs.getString("rel_name"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Error while retrieving relationship type map", e);
		}

		return relationshipTypeMap;
	}
}
