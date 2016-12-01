package org.icube.owen.explore;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsHelper;
import org.icube.owen.survey.Question;

public class ExploreHelper extends TheBorg {

	/**
	 * Retrieves data for metrics 
	 * @param companyId - Company ID of the employee
	 * @param teamListMap - Map with the (teamName, filterList) pair, can have as many teams as desired by the UI
	 * @return metricsMapList - Map with (teamName, metricList) pair
	 */
	public Map<String, List<Metrics>> getTeamMetricsData(int companyId, Map<String, List<Filter>> teamListMap) {
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
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
		dch.refreshCompanyConnection(companyId);
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
		Map<String, Map<Integer, List<Map<Date, Integer>>>> result = new HashMap<>();

		for (String teamName : teamListMap.keySet()) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
			Map<Integer, List<Map<Date, Integer>>> timeSeriesMap = new HashMap<>();
			List<Filter> filterList = teamListMap.get(teamName);
			Map<String, Object> parsedFilterListResult = UtilHelper.parseFilterList(filterList);
			try {
				if ((int) parsedFilterListResult.get("countAll") == 3) {
					// if all selections are ALL then it is a organizational team metric
					try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
							"{call getOrganizationMetricTimeSeries()}");
							ResultSet rs = cstmt.executeQuery()) {
						timeSeriesMap = getTimeSeriesMap(rs);
					}

				} else if ((int) parsedFilterListResult.get("countAll") == 2) {
					// if two of the filters are ALL then it is a dimension metric
					try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
							"{call getDimensionMetricTimeSeries(?)}")) {
						cstmt.setInt(1, (int) parsedFilterListResult.get("dimensionValueId"));
						try (ResultSet rs = cstmt.executeQuery()) {
							timeSeriesMap = getTimeSeriesMap(rs);
						}
					}

				} else if ((int) parsedFilterListResult.get("countAll") == 0) {
					// if none of the filters is ALL then it is a cube metric
					try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
							"{call getTeamMetricTimeSeries(?,?,?)}")) {
						cstmt.setInt(1, (int) parsedFilterListResult.get("funcId"));
						cstmt.setInt(2, (int) parsedFilterListResult.get("posId"));
						cstmt.setInt(3, (int) parsedFilterListResult.get("zoneId"));
						try (ResultSet rs = cstmt.executeQuery()) {
							timeSeriesMap = getTimeSeriesMap(rs);
						}
					}

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
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
		Map<Employee, List<Metrics>> result = new HashMap<>();
		try {
			dch.refreshCompanyConnection(companyId);
			for (Employee e : employeeList) {
				List<Metrics> metricsList = new ArrayList<>();
				try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
						"{call getIndividualMetricValue(?)}")) {
					cstmt.setInt(1, e.getEmployeeId());
					try (ResultSet rs = cstmt.executeQuery()) {
						MetricsHelper mh = new MetricsHelper();
						metricsList = mh.fillMetricsData(companyId, rs, null, "Individual");
						result.put(e, metricsList);
					}
				}

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
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
		Map<Employee, Map<Integer, List<Map<Date, Integer>>>> result = new HashMap<>();

		try {
			dch.refreshCompanyConnection(companyId);
			for (Employee e : employeeList) {
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
				Map<Integer, List<Map<Date, Integer>>> metricsList = new HashMap<>();
				try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
						"{call getIndividualMetricTimeSeries(?)}")) {
					cstmt.setInt(1, e.getEmployeeId());
					try (ResultSet rs = cstmt.executeQuery()) {
						metricsList = getTimeSeriesMap(rs);
						result.put(e, metricsList);
					}
				}

			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while retrieving individual metrics data", e);
		}
		return result;

	}

	/**
	 * Retrieves the Time series map
	 * @param rs - Resultset
	 * @return A map of metric ID and list of calculation date and respective score
	 * @throws SQLException - if the time series map is not retrieved
	 */
	public Map<Integer, List<Map<Date, Integer>>> getTimeSeriesMap(ResultSet rs) throws SQLException {
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();

		while (rs.next()) {
			if (result.containsKey(rs.getInt("metric_id"))) {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
				Map<Date, Integer> metricScoreMap = new HashMap<>();
				metricScoreMapList = result.get(rs.getInt("metric_id"));
				metricScoreMap.put(rs.getDate("calc_time"), rs.getInt("Score"));
				metricScoreMapList.add(metricScoreMap);
				result.put(rs.getInt("metric_id"), metricScoreMapList);
			} else {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
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
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
		Map<String, List<?>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.refreshCompanyConnection(companyId);
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
		try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement(); ResultSet res = stmt.executeQuery(query)) {
			List<Integer> empIdList = new ArrayList<>();
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("getTeamNetworkDiagram query for all teams  : " + query);
			while (res.next()) {
				empIdList.add(res.getInt("emp_id"));
				Node n = new Node();
				n.setEmployeeId(res.getInt("emp_id"));
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
		dch.refreshCompanyConnection(companyId);
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
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

		try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement(); ResultSet res = stmt.executeQuery(query)) {
			List<Integer> empIdList = new ArrayList<>();
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).debug("getIndividualNetworkDiagram query  : " + query);
			while (res.next()) {
				empIdList.add(res.getInt("emp_id"));
				Node n = new Node();
				n.setEmployeeId(res.getInt("emp_id"));
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
		dch.refreshCompanyConnection(companyId);
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
		try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement(); ResultSet res = stmt.executeQuery(query)) {
			while (res.next()) {
				Edge e = new Edge();
				e.setFromEmployeId(res.getInt("from"));
				e.setToEmployeeId(res.getInt("to"));
				e.setRelationshipType(res.getString("rel_type"));
				e.setWeight(res.getDouble("weight"));
				result.add(e);

			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception whil getting edgeList", e);
		}
		return result;
	}

	/**
	 * Returns a map of relationship type ID + relationship type Name
	 * @param companyId - Company ID
	 * @return Map of relationship type ID and relationship type name
	 */
	public Map<Integer, String> getRelationshipTypeMap(int companyId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
		Map<Integer, String> relationshipTypeMap = new HashMap<>();
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getRelationTypeList()}");
				ResultSet rs = cstmt.executeQuery()) {
			while (rs.next()) {
				relationshipTypeMap.put(rs.getInt("rel_id"), rs.getString("rel_name"));
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Error while retrieving relationship type map", e);
		}

		return relationshipTypeMap;
	}

	/**
	 * Returns a map of relationship type ID + relationship type Name with "others" added with ID 0
	 * @param companyId - Company ID
	 * @return Map of relationship type ID and relationship type name
	 */
	public Map<Integer, String> getMeQuestionRelationshipTypeMap(int companyId) {
		Map<Integer, String> relationshipTypeMap = getRelationshipTypeMap(companyId);
		relationshipTypeMap.put(0, "others");
		TreeMap<Integer, String> reversedRelationshipTypeMap = new TreeMap<>();
		for (int i : relationshipTypeMap.keySet()) {
			reversedRelationshipTypeMap.put(i, relationshipTypeMap.get(i));
		}
		return reversedRelationshipTypeMap.descendingMap();
	}

	/**
	 * Returns a list of MeResponseAnalysis objects
	 * @param companyId - company ID
	 * @param relationshipTypeId - relationship type ID
	 * @return List of MeResponseAnalysis objects which include Question object,MeResponse object and MeResponse object with aggregate
	 */
	public List<MeResponseAnalysis> getMeResponseAnalysisForOrg(int companyId, int relationshipTypeId) {
		List<MeResponseAnalysis> result = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getMeResponseAnalysisForOrg(?,?)}")) {
			dch.refreshCompanyConnection(companyId);
			cstmt.setTimestamp(1, UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt(2, relationshipTypeId);
			try (ResultSet rs = cstmt.executeQuery()) {
				while (rs.next()) {
					// fill the question object and the MeResponse object
					Question q = new Question();
					MeResponse meResponse = new MeResponse();
					MeResponse meResponseAggregate = new MeResponse();
					org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
					Map<String, MeResponse> orgMeResponseMap = new HashMap<>();
					MeResponseAnalysis meResponseAnalysis = new MeResponseAnalysis();
					q.setQuestionId(rs.getInt("que_id"));
					q.setQuestionText(rs.getString("question"));
					q.setStartDate(rs.getDate("start_date"));
					q.setEndDate(rs.getDate("end_date"));
					q.setRelationshipTypeId(rs.getInt("rel_id"));
					int meResponseSum = rs.getInt("agree") + rs.getInt("disagree") + rs.getInt("neutral") + rs.getInt("strongly_agree")
							+ rs.getInt("strongly_disagree");
					double meResponseRate = Math.round((double) meResponseSum / rs.getInt("total_employee") * 100);
					q.setResponsePercentage(meResponseRate);
					meResponse.setAgree(rs.getInt("agree"));
					meResponse.setDisagree(rs.getInt("disagree"));
					meResponse.setNeutral(rs.getInt("neutral"));
					meResponse.setStronglyAgree(rs.getInt("strongly_agree"));
					meResponse.setStronglyDisagree(rs.getInt("strongly_disagree"));
					meResponseAggregate.setAgree(rs.getInt("agree"));
					meResponseAggregate.setDisagree(rs.getInt("disagree"));
					meResponseAggregate.setNeutral(rs.getInt("neutral"));
					meResponseAggregate.setStronglyAgree(rs.getInt("strongly_agree"));
					meResponseAggregate.setStronglyDisagree(rs.getInt("strongly_disagree"));
					double meResponseAverage = Math.round(((double) ((rs.getInt("agree") * 4) + (rs.getInt("disagree") * 2)
							+ (rs.getInt("neutral") * 3) + (rs.getInt("strongly_agree") * 5) + (rs.getInt("strongly_disagree") * 1)))
							/ meResponseSum);
					meResponseAggregate.setAverage(meResponseAverage);
					meResponse.setAverage(meResponseAverage);
					orgMeResponseMap.put("org", meResponse);
					meResponseAnalysis.setQuestion(q);
					meResponseAnalysis.setTeamResponseMap(orgMeResponseMap);
					meResponseAnalysis.setMeResponseAggregate(meResponseAggregate);
					result.add(meResponseAnalysis);
				}
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while retrieving the Me response details for organizaton", e);
		}

		return result;
	}

	/**
	 * Returns a list of MeResponseAnalysis objects
	 * @param companyId - Company ID
	 * @param relationshipTypeId - Relationship type ID
	 * @param teamListMap - List of filters for each team selected
	 * @return List of MeResponseAnalysis objects which include Question object,MeResponse object and MeResponse object with aggregate
	 */
	public List<MeResponseAnalysis> getMeResponseAnalysisForTeam(int companyId, int relationshipTypeId, Map<String, List<Filter>> teamListMap) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<MeResponseAnalysis> result = new ArrayList<>();
		List<Integer> questionIdList = new ArrayList<>();
		int totalEmployees = 0;
		try {
			try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
					"{call getCompletedMeQuestionList(?,?)}")) {
				cstmt.setTimestamp(1, UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
				cstmt.setInt(2, relationshipTypeId);
				try (ResultSet rs = cstmt.executeQuery()) {
					org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
					Map<Integer, Question> questionMap = new HashMap<>();

					// fill the Question object
					while (rs.next()) {
						Question q = new Question();
						q.setQuestionId(rs.getInt("que_id"));
						q.setQuestionText(rs.getString("question"));
						q.setStartDate(rs.getDate("start_date"));
						q.setEndDate(rs.getDate("end_date"));
						q.setRelationshipTypeId(rs.getInt("rel_id"));
						questionIdList.add(rs.getInt("que_id"));
						questionMap.put(rs.getInt("que_id"), q);
					}

					dch.refreshCompanyConnection(companyId);

					// fill the MeResponse object for team
					org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
					Map<Integer, Map<String, MeResponse>> mer = new HashMap<>();
					Map<String, MeResponse> teamMeResponseList = new HashMap<>();
					for (String teamName : teamListMap.keySet()) {
						List<Filter> filterList = teamListMap.get(teamName);
						Map<String, Object> parsedFilterMap = UtilHelper.parseFilterList(filterList);
						System.out.println(parsedFilterMap.get("posId"));
						try (CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
								"{call getMeResponseAnalysisForTeam(?,?,?,?)}")) {
							cstmt1.setString("que_list", questionIdList.toString().substring(1, questionIdList.toString().length() - 1).replaceAll(
									" ", ""));
							cstmt1.setInt("fun", (int) parsedFilterMap.get("funcId"));
							cstmt1.setInt("pos", (int) parsedFilterMap.get("posId"));
							cstmt1.setInt("zon", (int) parsedFilterMap.get("zoneId"));
							try (ResultSet rs1 = cstmt1.executeQuery()) {
								while (rs1.next()) {
									int questionId = rs1.getInt("que_id");
									if (mer.containsKey(questionId)) {
										teamMeResponseList = mer.get(questionId);
									} else {
										org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
										teamMeResponseList = new HashMap<>();
									}

									MeResponse meResponse = new MeResponse();
									meResponse.setAgree(rs1.getInt("agree"));
									meResponse.setDisagree(rs1.getInt("disagree"));
									meResponse.setNeutral(rs1.getInt("neutral"));
									meResponse.setStronglyAgree(rs1.getInt("strongly_agree"));
									meResponse.setStronglyDisagree(rs1.getInt("strongly_disagree"));
									int meResponseSum = rs1.getInt("agree") + rs1.getInt("disagree") + rs1.getInt("neutral")
											+ rs1.getInt("strongly_agree") + rs1.getInt("strongly_disagree");
									double meResponseAverage = Math
											.round(((double) ((rs1.getInt("agree") * 4) + (rs1.getInt("disagree") * 2) + (rs1.getInt("neutral") * 3)
													+ (rs1.getInt("strongly_agree") * 5) + (rs1.getInt("strongly_disagree") * 1)))
													/ meResponseSum);
									meResponse.setAverage(meResponseAverage);
									teamMeResponseList.put(teamName, meResponse);
									mer.put(questionId, teamMeResponseList);

								}

								// calculate total number of employees as sum of employees in each team which will be used in calculating the response
								// rate for a question

								rs1.first();
								totalEmployees = totalEmployees + rs1.getInt("total_employee");
							}
						}

					}

					// fill the MeResponseAggregate object for team
					org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
					Map<Integer, MeResponseAnalysis> meResAnalysis = new HashMap<>();
					for (Integer qId : questionMap.keySet()) {
						MeResponseAnalysis meResponseAnalysisWithResponse = new MeResponseAnalysis();
						MeResponse meResponseAggregate = new MeResponse();
						org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
						Map<String, MeResponse> meResponseList = new HashMap<>();

						// if there is an existing MeResponse object for that question ID fetch that object
						if (mer.containsKey(qId)) {
							meResponseList = mer.get(qId);
							for (MeResponse meRes : meResponseList.values()) {
								meResponseAggregate.setStronglyDisagree(meResponseAggregate.getStronglyDisagree() + meRes.getStronglyDisagree());
								meResponseAggregate.setDisagree(meResponseAggregate.getDisagree() + meRes.getDisagree());
								meResponseAggregate.setNeutral(meResponseAggregate.getNeutral() + meRes.getNeutral());
								meResponseAggregate.setAgree(meResponseAggregate.getAgree() + meRes.getAgree());
								meResponseAggregate.setStronglyAgree(meResponseAggregate.getStronglyAgree() + meRes.getStronglyAgree());

							}

						}

						// if there is no existing MeResponse object in case of no responses for a question, send the object with value 0
						else {
							org.apache.log4j.Logger.getLogger(ExploreHelper.class).info("HashMap created!!!");
							meResponseList = new HashMap<>();
							for (String teamName : teamListMap.keySet()) {
								meResponseList.put(teamName, meResponseAggregate);
							}
						}
						meResponseAnalysisWithResponse.setTeamResponseMap(meResponseList);
						meResponseAnalysisWithResponse.setMeResponseAggregate(meResponseAggregate);
						meResAnalysis.put(qId, meResponseAnalysisWithResponse);
					}

					// create the final MeResponseAnalysis object for team by mapping the question object created at start of the function
					MeResponseAnalysis finalMeResponseanalysis = new MeResponseAnalysis();
					for (int qId : questionMap.keySet()) {
						finalMeResponseanalysis = meResAnalysis.get(qId);
						Question q = questionMap.get(qId);

						// calculate the response rate for each question
						int meResponseSum = finalMeResponseanalysis.getMeResponseAggregate().getAgree()
								+ finalMeResponseanalysis.getMeResponseAggregate().getDisagree()
								+ finalMeResponseanalysis.getMeResponseAggregate().getNeutral()
								+ finalMeResponseanalysis.getMeResponseAggregate().getStronglyAgree()
								+ finalMeResponseanalysis.getMeResponseAggregate().getStronglyDisagree();
						double meResponseRate = Math.round((double) meResponseSum / totalEmployees * 100);
						System.out.println("response rate for question Id" + qId + "is " + meResponseRate);
						q.setResponsePercentage(meResponseRate);
						finalMeResponseanalysis.setQuestion(q);
						result.add(finalMeResponseanalysis);

					}

				}
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while retrieving the Me response details for team", e);
		}

		return result;
	}

}
