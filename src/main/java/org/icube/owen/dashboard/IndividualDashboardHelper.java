package org.icube.owen.dashboard;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.explore.ExploreHelper;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.initiative.InitiativeHelper;
import org.icube.owen.initiative.InitiativeList;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.survey.Question;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.RList;

public class IndividualDashboardHelper extends TheBorg {

	/**
	 * Retrieves the list of 3 metrics to be displayed for the individual
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return A list of Metrics
	 */
	public List<Metrics> getIndividualMetrics(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		ExploreHelper eh = new ExploreHelper();
		List<Metrics> metricsList = new ArrayList<>();
		try {

			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getIndividualMetricValueForIndividual(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			metricsList = eh.fillMetricsData(rs, "Individual");

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving individual metrics data", e);
		}

		return metricsList;
	}

	/**
	 * Retrieves the time series data
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return Time series data to be displayed
	 */
	public Map<Integer, List<Map<Date, Integer>>> getIndividualMetricsTimeSeries(int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		ExploreHelper eh = new ExploreHelper();
		Map<Integer, List<Map<Date, Integer>>> metricsListMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getIndividualMetricTimeSeriesForIndividual(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			metricsListMap = eh.getTimeSeriesMap(rs);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving individual metrics data", e);
		}

		return metricsListMap;
	}

	/**
	 * Retrieves the list of active initiatives for which the employee is an owner of
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return A list of Initiative objects
	 */
	public List<Initiative> individualInitiativeList(int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		InitiativeHelper ih = new InitiativeHelper();
		InitiativeList il = new InitiativeList();
		List<Initiative> initiativeList = new ArrayList<>();
		org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Get initiative list");
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try {
			String initiativeListQuery = "match(i:Init {Status:'Active'})<-[r:owner_of]-(e:Employee {emp_id:"
					+ employeeId
					+ "}) with i as ini match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a)"
					+ " where i=ini return i.Name as Name,i.StartDate as StartDate, i.EndDate as EndDate, "
					+ "i.Id as Id,case i.Category when 'Individual' then collect(distinct(a.emp_id)) else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, "
					+ "labels(a) as Filters,collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status;";

			ResultSet res = dch.neo4jCon.createStatement().executeQuery(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Executed query for retrieving initiative list");
			while (res.next()) {

				int initiativeId = res.getInt("Id");
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					i.setFilterList(ih.setPartOfConnections(res, i));
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					il.setInitiativeValues(res, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("List of initiatives : " + initiativeList.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while getting the initiative list", e);
		}
		return initiativeList;

	}

	/**
	 * Retrieves the Activity Feed for the employee
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return A list of ActivityFeed objects
	 */

	public List<ActivityFeed> getActivityFeedList(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<ActivityFeed> result = new ArrayList<>();
		org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Get ActivityFeed list");
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getAppreciationActivity(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			String initiativeListQuery = "MATCH (i:Init {Status:'Active'})<-[:owner_of]-(e:Employee {emp_id:" + employeeId
					+ "}) return i.Name as Name ,i.CreatedOn as CreatedOn";
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Executed query for retrieving initiative list");
			while (res.next()) {
				ActivityFeed afini = new ActivityFeed();
				afini.setActivityType("Initiative");
				afini.setHeaderText("Initiative created");
				afini.setBodyText("You were added to the " + res.getString("Name") + " initiative");
				SimpleDateFormat parserSDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzz yyyy", Locale.ENGLISH);
				afini.setDate(parserSDF.parse(res.getString("CreatedOn")));
				result.add(afini);
			}
			while (rs.next()) {
				ActivityFeed afapp = new ActivityFeed();
				afapp.setHeaderText("Appreciation received");
				afapp.setBodyText("You were appreciated for " + rs.getString("metric_name"));
				afapp.setActivityType("Appreciation");
				afapp.setDate(rs.getTimestamp("response_time"));
				result.add(afapp);
			}

			Collections.sort(result, new Comparator<ActivityFeed>() {
				public int compare(ActivityFeed af1, ActivityFeed af2) {
					return af1.getDate().compareTo(af2.getDate());
				}
			});

		} catch (SQLException | ParseException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving the activity feed data", e);
		}

		return result;
	}

	private Map<Integer, Integer> getMetricRelationshipTypeMapping() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, Integer> result = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricRelationshipType()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				result.put(rs.getInt("metric_id"), rs.getInt("rel_id"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving metrics relationship type id data",
					e);
		}

		return result;
	}

	/**
	 * Retrieves the smart list of employees 
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @param metricId - Metric Id of the selected Metric
	 * @return - A map of ranking and Employee object
	 */
	public Map<Integer, Employee> getSmartList(int employeeId, int metricId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, Employee> employeeScoreMap = new HashMap<>();
		Map<Integer, Integer> MetricRelationshipTypeMap = getMetricRelationshipTypeMapping();
		try {
			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("R Path for eval " + s);
			dch.rCon.eval(s);
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Filling up parameters for rscript function");
			dch.rCon.assign("emp_id", new int[] { employeeId });
			dch.rCon.assign("rel_id", new int[] { MetricRelationshipTypeMap.get(metricId) });
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Calling the actual function in RScript SmartListResponse");
			REXP employeeSmartList = dch.rCon.parseAndEval("try(eval(SmartListResponse(emp_id, rel_id)))");
			if (employeeSmartList.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(Question.class).error("Error: " + employeeSmartList.asString());
				throw new Exception("Error: " + employeeSmartList.asString());
			} else {
				org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug(
						"Retrieval of the employee smart list completed " + employeeSmartList.asList());
			}

			RList result = employeeSmartList.asList();
			REXPInteger empIdResult = (REXPInteger) result.get("emp_id");
			int[] empIdArray = empIdResult.asIntegers();
			REXPInteger rankResult = (REXPInteger) result.get("Rank");
			int[] rankArray = rankResult.asIntegers();

			for (int i = 0; i < rankArray.length; i++) {
				Employee e = new Employee();
				e = e.get(empIdArray[i]);
				employeeScoreMap.put(rankArray[i], e);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error(
					"Error while trying to retrieve the smart list for employee from question", e);
		}

		return employeeScoreMap;
	}

	/**
	 * Saves the Appreciation response
	 * @param appreciationResponseMap - Map of employee object and ranking
	 * @param companyId - Company Id of the employee who is logged in
	 * @param employeeId - Company Id of the employee who is logged in
	 * @param metricId
	 * @return true/false
	 */
	public boolean saveAppreciation(Map<Employee, Integer> appreciationResponseMap, int companyId, int employeeId, int metricId) {
		boolean responseSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Integer, Integer> metricRelationshipTypeMap = getMetricRelationshipTypeMapping();
		try {
			for (Employee e : appreciationResponseMap.keySet()) {
				CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call insertAppreciation(?,?,?,?,?)}");
				cstmt.setInt(1, employeeId);
				cstmt.setTimestamp(2, Timestamp.from(Instant.now()));
				cstmt.setInt(3, e.getEmployeeId());
				cstmt.setInt(4, metricRelationshipTypeMap.get(metricId));
				cstmt.setInt(5, appreciationResponseMap.get(e));
				ResultSet rs = cstmt.executeQuery();
				rs.next();
				if (rs.getString("op").equalsIgnoreCase("true")) {
					responseSaved = true;
					org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Successfully saved the response ");
				} else {
					org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Error in saving the response ");
				}

			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while saving the response ", e);
		}
		return responseSaved;
	}

	/**
	 * Updates the password 
	 * Password Guidelines : Minimum 8 characters and should contain at least 1 numeric digit
	 * @param currentPassword
	 * @param newPassword
	 * @return true/false 
	 */
	public boolean changePassword(String currentPassword, String newPassword, int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		boolean passwordChanged = false;
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call updateEmployeePassword(?,?,?)}");
			cstmt.setInt(1, employeeId);
			cstmt.setString(2, currentPassword);
			cstmt.setString(3, newPassword);

			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				passwordChanged = true;
			} else {
				org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Invalid username/password");
				throw new Exception("Invalid credentials!!!");
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while validating password ", e);
		}
		return passwordChanged;
	}
}
