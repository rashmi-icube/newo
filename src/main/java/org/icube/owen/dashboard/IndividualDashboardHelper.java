package org.icube.owen.dashboard;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.mail.MessagingException;

import org.apache.commons.lang3.time.DateUtils;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.explore.ExploreHelper;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.icube.owen.individual.Login;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.initiative.InitiativeHelper;
import org.icube.owen.initiative.InitiativeList;
import org.icube.owen.jobScheduler.EmailSender;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsHelper;
import org.icube.owen.survey.Question;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

public class IndividualDashboardHelper extends TheBorg {

	String charList = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

	/**
	 * Retrieves the list of 3 metrics to be displayed for the individual
	 * @param companyId - Company ID
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return A list of Metrics
	 */

	public List<Metrics> getIndividualMetrics(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		MetricsHelper mh = new MetricsHelper();
		List<Metrics> metricsList = new ArrayList<>();
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getIndividualMetricValueForIndividual(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			List<Metrics> initialMetricsList = mh.fillMetricsData(companyId, rs, null, "Individual");
			// removing 2 metrics since only 3 default ones are shown in the panel i.e. Expertise, Mentorship, Influence
			for (Metrics m : initialMetricsList) {
				if (m.getId() == 1 || m.getId() == 2 || m.getId() == 4) {
					metricsList.add(m);
				}
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving individual metrics data", e);
		}

		return metricsList;
	}

	/**
	 * Retrieves the time series data
	 * @param companyId - company ID
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return Time series data to be displayed
	 */

	public Map<Integer, List<Map<Date, Integer>>> getIndividualMetricsTimeSeries(int companyId, int employeeId) {
		Map<Integer, List<Map<Date, Integer>>> metricsTimeSeriesMasterMap = getEmptyTimeSeriesMap(companyId, "Individual");
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		ExploreHelper eh = new ExploreHelper();
		Map<Integer, List<Map<Date, Integer>>> metricsListMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getIndividualMetricTimeSeriesForIndividual(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			metricsListMap = eh.getTimeSeriesMap(rs);
			for (int i : metricsListMap.keySet()) {
				metricsTimeSeriesMasterMap.put(i, metricsListMap.get(i));
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving individual metrics data", e);
		}

		return metricsTimeSeriesMasterMap;
	}

	/**
	 * Retrieves an empty time series map
	 * @param companyId - Company ID
	 * @param category - Should be Individual
	 * @return A map of metric Id and list of maps of date and metric score set to 0
	 */
	private Map<Integer, List<Map<Date, Integer>>> getEmptyTimeSeriesMap(int companyId, String category) {
		Map<Integer, List<Map<Date, Integer>>> metricsTimeSeriesMasterMap = new HashMap<>();
		MetricsHelper mh = new MetricsHelper();
		Map<Integer, String> metricListMap = mh.getMetricListForCategory(companyId, category);
		for (int metric_id : metricListMap.keySet()) {
			List<Map<Date, Integer>> metricsTimeSeriesList = new ArrayList<>();
			if (metric_id == 1 || metric_id == 2 || metric_id == 4) {
				Map<Date, Integer> metricsTimeSeriesMap = new HashMap<>();
				metricsTimeSeriesMap.put(Date.from(Instant.now()), 0);
				metricsTimeSeriesList.add(metricsTimeSeriesMap);
				metricsTimeSeriesMasterMap.put(metric_id, metricsTimeSeriesList);
			}
		}
		return metricsTimeSeriesMasterMap;
	}

	/**
	 * Retrieves the list of active initiatives for which the employee is an owner of
	 * @param companyId - Company ID
	 * @param employeeId - Employee Id of the individual who is logged in
	 * @return A list of Initiative objects
	 */

	public List<Initiative> getIndividualInitiativeList(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		InitiativeHelper ih = new InitiativeHelper();
		InitiativeList il = new InitiativeList();
		List<Initiative> initiativeList = new ArrayList<>();
		org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Get initiative list");
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try {
			String initiativeListQuery = "match(i:Init {Status:'Active'})<-[r:owner_of]-(e:Employee {emp_id:"
					+ employeeId
					+ "}) with i as ini match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a)"
					+ " where i=ini return i.Name as Name,i.StartDate as StartDate, i.EndDate as EndDate, i.CreatedOn as CreationDate,"
					+ "i.Id as Id,case i.Category when 'Individual' then collect(distinct(a.emp_id)) else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, "
					+ "labels(a) as Filters,collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status;";
			Statement stmt = dch.companyNeoConnectionPool.get(companyId).createStatement();
			ResultSet res = stmt.executeQuery(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Executed query for retrieving initiative list");
			while (res.next()) {

				int initiativeId = res.getInt("Id");
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					i.setFilterList(ih.setPartOfConnections(companyId, res, i));
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					il.setInitiativeValues(companyId, res, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("List of initiatives : " + initiativeList.toString());
			stmt.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while getting the initiative list", e);
		}

		return initiativeList;

	}

	/**
	 * Retrieves the Activity Feed for the employee
	 * @param companyId - Company ID
	 * @param employeeId - Employee Id of the individual who is logged in (pass employeeId = 208 for testing)
	 * @param pageNumber - page number of the page to be displayed
	 * @return A list of ActivityFeed objects
	 */

	public Map<Date, List<ActivityFeed>> getActivityFeedList(int companyId, int employeeId, int pageNumber) {
		org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug(
				"Entering getActivityFeedList with employee ID : " + employeeId + " page number " + pageNumber);
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Date, List<ActivityFeed>> result = new TreeMap<>(Collections.reverseOrder());

		org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Get ActivityFeed list");
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getAppreciationActivity(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			String initiativeListQuery = "MATCH (i:Init {Status:'Active'})<-[:owner_of]-(e:Employee {emp_id:" + employeeId
					+ "}) return i.Name as Name ,i.CreatedOn as CreatedOn";
			Statement stmt = dch.companyNeoConnectionPool.get(companyId).createStatement();
			ResultSet res = stmt.executeQuery(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Executed query for retrieving initiative list");
			SimpleDateFormat parserSDF = new SimpleDateFormat(UtilHelper.dateTimeFormat);
			List<ActivityFeed> afList = new ArrayList<>();
			while (res.next()) {
				ActivityFeed af = new ActivityFeed();
				af.setActivityType("Initiative");
				af.setHeaderText("Initiative created");
				af.setBodyText("You were added to the " + res.getString("Name") + " initiative");
				af.setDate(parserSDF.parse(res.getString("CreatedOn")));
				afList.add(af);
			}
			while (rs.next()) {
				org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Appreciation from database");
				ActivityFeed af = new ActivityFeed();
				af.setHeaderText("Appreciation received");
				af.setBodyText("You were appreciated for " + rs.getString("metric_name"));
				af.setActivityType("Appreciation");
				af.setDate(parserSDF.parse(rs.getString("response_time")));
				org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug(
						af.getDate() + ":" + af.getActivityType() + " : " + af.getBodyText() + ":" + af.getHeaderText());
				afList.add(af);
			}
			if (!afList.isEmpty()) {
				Collections.sort(afList, new Comparator<ActivityFeed>() {
					@Override
					public int compare(ActivityFeed af1, ActivityFeed af2) {
						return af2.getDate().compareTo(af1.getDate());
					}
				});

				// return a sublist of result based on the page number
				int feedThreshold = 25;
				int fromIndex = pageNumber == 1 ? 0 : (pageNumber - 1) * feedThreshold + 1;
				int toIndex = pageNumber == 1 ? feedThreshold : pageNumber * feedThreshold;

				ArrayList<ActivityFeed> afSubList = new ArrayList<ActivityFeed>(afList.subList(fromIndex, toIndex > afList.size() ? afList.size()
						: toIndex));

				for (ActivityFeed af1 : afSubList) {
					Date d = DateUtils.truncate(af1.getDate(), Calendar.DAY_OF_MONTH);
					if (result.containsKey(d)) {
						result.get(d).add(af1);
					} else {
						List<ActivityFeed> resultAfList = new ArrayList<>();
						resultAfList.add(af1);
						result.put(d, resultAfList);
					}
				}

				for (List<ActivityFeed> afl : result.values()) {
					for (ActivityFeed af : afl) {
						org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug(
								af.getDate() + ":" + af.getActivityType() + " : " + af.getBodyText() + ":" + af.getHeaderText());

					}

				}
			}
			stmt.close();
		} catch (SQLException | ParseException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while retrieving the activity feed data", e);
		}

		return result;
	}

	/**
	 * Retrieves the metric relationship type mapping
	 * @param companyId - Company ID
	 * @return - map of the mapping between Metric and RelationshipType
	 */
	private Map<Integer, Integer> getMetricRelationshipTypeMapping(int companyId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Integer, Integer> result = new HashMap<>();
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getMetricRelationshipType()}");
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
	 * @param companyId - Company ID
	 * @param employeeId - Employee Id of the individual who is logged in 
	 * @param metricId - Metric Id of the selected Metric
	 * @return - A list of Employee objects
	 */

	public List<Employee> getSmartList(int companyId, int employeeId, int metricId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Integer, Employee> employeeRankMap = new LinkedHashMap<>();
		List<Employee> employeeList = new ArrayList<>();
		Map<Integer, Integer> MetricRelationshipTypeMap = getMetricRelationshipTypeMapping(1);
		try {
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("R Connection Available : " + rCon.isConnected());
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Filling up parameters for rscript function");
			rCon.assign("company_id", new int[] { companyId });
			rCon.assign("emp_id", new int[] { employeeId });
			rCon.assign("rel_id", new int[] { MetricRelationshipTypeMap.get(metricId) });
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Calling the actual function in RScript SmartListResponse");
			REXP employeeSmartList = rCon.parseAndEval("try(eval(SmartListResponse(company_id, emp_id, rel_id)))");
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

			for (int i = 0; i < empIdArray.length; i++) {
				Employee e = new Employee();
				e = e.get(companyId, empIdArray[i]);
				employeeRankMap.put(rankArray[i], e);
			}
			Map<Integer, Employee> sorted_map = new TreeMap<Integer, Employee>(employeeRankMap);
			employeeList = new ArrayList<Employee>(sorted_map.values());

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error(
					"Error while trying to retrieve the smart list for employee from question", e);
		} finally {
			dch.releaseRcon();
		}

		return employeeList;
	}

	/**
	 * Saves the Appreciation response
	 * @param appreciationResponseMap - Map of employee object and ranking
	 * @param companyId - Company Id of the employee who is logged in
	 * @param employeeId - Company Id of the employee who is logged in
	 * @param metricId - ID of the metric
	 * @return true/false
	 */

	public boolean saveAppreciation(int companyId, int employeeId, int metricId, Map<Employee, Integer> appreciationResponseMap) {
		boolean responseSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Integer, Integer> metricRelationshipTypeMap = getMetricRelationshipTypeMapping(companyId);
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
					org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Successfully saved the appreciation ");
				} else {
					org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Error in saving the appreciation ");
				}

			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while saving the appreciation ", e);
		}
		return responseSaved;
	}

	/**
	 * Updates the password 
	 * Password Guidelines : Minimum 8 characters and should contain at least 1 numeric digit
	 * @param companyId - Company ID
	 * @param employeeId - ID of the employee who is logged in
	 * @param currentPassword - current password
	 * @param newPassword - new password
	 * @return true/false
	 */
	public boolean changePassword(int companyId, int employeeId, String currentPassword, String newPassword) {
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
				org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Current password is incorrect");
				throw new Exception("Current password is incorrect");
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while validating password ", e);
		}
		return passwordChanged;
	}

	/**
	 * @param emailId - email id of the employee whose password needs to be reset
	 * @throws Exception 
	 */
	public boolean generateNewPassword(String emailId) throws Exception {
		boolean passwordChanged = false;
		EmailSender es = new EmailSender();
		List<String> address = Arrays.asList(emailId);

		// generate a random password
		int RandomPasswordLength = 8;
		StringBuffer randStr = new StringBuffer();
		for (int i = 0; i < RandomPasswordLength; i++) {
			int number = getRandomNumber();
			char ch = charList.charAt(number);
			randStr.append(ch);
		}

		// save the new password in the database

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Connection companySqlCon = null;
		int index = emailId.indexOf('@');
		String companyDomain = emailId.substring(index + 1);
		int companyId = 0;
		try {
			CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyDb(?)}");
			cstmt.setString(1, companyDomain);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				companyId = rs.getInt("comp_id");
				dch.getCompanyConnection(companyId);
				companySqlCon = dch.companySqlConnectionPool.get(companyId);
			}
			Statement stmt = companySqlCon.createStatement();
			int updatePassword = stmt.executeUpdate("update login_table set password = " + '"' + randStr.toString() + '"' + " where login_id = "
					+ '"' + emailId + '"' + "");
			if (updatePassword == 0) {
				org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Current password is incorrect");
				throw new Exception("Error in resetting the password");
			} else {
				passwordChanged = true;
			}

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(Login.class).error("Exception while retrieving the company database", e1);
		}

		// send the new password to the employee
		try {
			es.sendNewPasswordEmail(address, randStr.toString());
		} catch (MessagingException e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Error in sending email", e);
		}

		return passwordChanged;
	}

	/**
	 * @return random numbers
	 */
	private int getRandomNumber() {
		int randomInt = 0;
		Random randomGenerator = new Random();
		randomInt = randomGenerator.nextInt(charList.length());
		if (randomInt - 1 == -1) {
			return randomInt;
		} else {
			return randomInt - 1;
		}
	}

	/**
	 * Updates the timestamp when the user visits the individual dashboard page
	 * Timestamp is used for getting the notification count on the individual dashboard page
	 * @param companyId - Company ID
	 * @param employeeId - ID of the employee who is logged in
	 * @return true/false
	 */

	public boolean updateNotificationTimestamp(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		boolean timestampUpdated = false;
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call updateNotificationTime(?,?)}");
			cstmt.setInt("empid", employeeId);
			cstmt.setTimestamp("noti_time", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				timestampUpdated = true;
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while updating notification timestamp", e);
		}
		return timestampUpdated;
	}

	/**
	 * Returns the notifications count for the individual dashboard page
	 * @param companyId - Company ID
	 * @param employeeId - ID of the employee who is logged in
	 * @return the number of notifications
	 */
	public Integer getNotificationsCount(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		int notificationCount = 0;
		Date lastNotificationDate = null;
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getAppreciationActivityLatestCount(?)}");
			cstmt.setInt("empid", employeeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				notificationCount += rs.getInt("appreciation_count");
				lastNotificationDate = rs.getDate("last_notified");
			}

			SimpleDateFormat sdf = new SimpleDateFormat(UtilHelper.dateTimeFormat);
			String notificationCountQuery = "MATCH (e:Employee {emp_id:" + employeeId + "})-[:owner_of]->(i:Init) where i.CreatedOn>'"
					+ sdf.format(lastNotificationDate) + "' return count(i) as initiative_count";
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug(
					"Query to get notifications count from neo4j : " + notificationCountQuery);
			Statement stmt = dch.companyNeoConnectionPool.get(companyId).createStatement();
			ResultSet res = stmt.executeQuery(notificationCountQuery);
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).debug("Executed query for retrieving initiative list");
			while (res.next()) {
				notificationCount += res.getInt("initiative_count");
			}
			stmt.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(IndividualDashboardHelper.class).error("Exception while updating notification timestamp", e);
		}
		return notificationCount;
	}
}
