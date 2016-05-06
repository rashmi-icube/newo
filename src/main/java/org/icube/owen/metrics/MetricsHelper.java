package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

public class MetricsHelper extends TheBorg {

	/**
	 * Retrieves the list of metrics of category team
	 * @param companyId - Comapny ID 
	 * @param initiativeTypeId - Initiative type ID
	 * @param parsedFilterListResult - A map of Filter objects
	 * @param previousScoreNeeded - true/false if previous score is required or not
	 * @return List of metrics objects
	 * @throws SQLException - If unable to retrieve the metrics list
	 */
	@SuppressWarnings("unchecked")
	public List<Metrics> getTeamMetricsList(int companyId, int initiativeTypeId, Map<String, Object> parsedFilterListResult,
			boolean previousScoreNeeded) throws SQLException {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Metrics> metricList = new ArrayList<>();
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		if (initiativeTypeId > 0) {
			primaryMetricMap = getPrimaryMetricMap(companyId, initiativeTypeId);
		}
		if ((int) parsedFilterListResult.get("countAll") == 3) {
			// if all selections are ALL then it is a organizational team metric
			CallableStatement cstmt;
			if (previousScoreNeeded) {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the getOrganizationMetricValueAggregate");
				cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getOrganizationMetricValueAggregate()}");
			} else {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the getOrganizationMetricValue");
				cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getOrganizationMetricValue()}");
			}
			ResultSet rs = cstmt.executeQuery();
			metricList = fillMetricsData(companyId, rs, primaryMetricMap, "Team");
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calculated metrics for organization : " + metricList.size());
		} else if ((int) parsedFilterListResult.get("countAll") == 2) {
			// if two of the filters are ALL then it is a dimension metric
			CallableStatement cstmt;
			if (previousScoreNeeded) {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the getDimensionMetricValueAggregate");
				org.apache.log4j.Logger.getLogger(MetricsHelper.class)
						.debug("Dimension Value ID : " + parsedFilterListResult.get("dimensionValueId"));
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Dimension ID : " + parsedFilterListResult.get("dimensionId"));
				cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getDimensionMetricValueAggregate(?,?)}");
				cstmt.setInt(1, (int) parsedFilterListResult.get("dimensionValueId"));
				cstmt.setInt(2, (int) parsedFilterListResult.get("dimensionId"));
			} else {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the getDimensionMetricValueAggregate");
				org.apache.log4j.Logger.getLogger(MetricsHelper.class)
						.debug("Dimension Value ID : " + parsedFilterListResult.get("dimensionValueId"));
				cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getDimensionMetricValue(?)}");
				cstmt.setInt(1, (int) parsedFilterListResult.get("dimensionValueId"));
			}

			ResultSet rs = cstmt.executeQuery();
			metricList = fillMetricsData(companyId, rs, primaryMetricMap, "Team");
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calculated metrics for dimension : " + metricList.size());
		} else if ((int) parsedFilterListResult.get("countAll") == 0) {
			// if none of the filters is ALL then it is a cube metric
			CallableStatement cstmt;
			if (previousScoreNeeded) {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the getTeamMetricValueAggregate");
				cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getTeamMetricValueAggregate(?, ?, ?)}");
			} else {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the getTeamMetricValue");
				cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getTeamMetricValue(?, ?, ?)}");
			}
			cstmt.setInt(1, (int) parsedFilterListResult.get("funcId"));
			cstmt.setInt(2, (int) parsedFilterListResult.get("posId"));
			cstmt.setInt(3, (int) parsedFilterListResult.get("zoneId"));
			ResultSet rs = cstmt.executeQuery();
			metricList = fillMetricsData(companyId, rs, primaryMetricMap, "Team");
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calculated metrics for team : " + metricList.size());
		} else {
			// else call metric.R
			MetricsList ml = new MetricsList();
			metricList = ml.getInitiativeMetricsForTeam(companyId, 0, (List<Filter>) parsedFilterListResult.get("filterList"));
		}

		return metricList;
	}

	/**
	 * Fills the metrics object
	 * @param initiativeTypeId - Initiative type ID
	 * @param rs - ResultSet containing the metrics details
	 * @param primaryMetricMap - Map containing the primary metric ID and name
	 * @param category - Team/Individual
	 * @return - List of metrics objects 
	 * @throws SQLException If unable to fill the metrics object
	 */
	public List<Metrics> fillMetricsData(int companyId, ResultSet rs, Map<Integer, String> primaryMetricMap, String category)
			throws SQLException {
		Map<Integer, Metrics> masterMetricsMap = getEmptyMetricScoreList(companyId, category, primaryMetricMap);
		List<Metrics> metricsList = new ArrayList<>();
		while (rs.next()) {
			Metrics m = new Metrics();
			m.setId(rs.getInt("metric_id"));
			m.setName(rs.getString("metric_name"));
			m.setScore(rs.getInt("current_score"));
			m.setDateOfCalculation(rs.getDate("calc_time"));
			m.setCategory(category);
			if (UtilHelper.hasColumn(rs, "previous_score")) {
				m.setDirection(m.calculateMetricDirection(rs.getInt("current_score"), rs.getInt("previous_score")));
			}
			if (primaryMetricMap != null && primaryMetricMap.containsKey(rs.getInt("metric_id"))) {
				m.setPrimary(true);
			} else {
				m.setPrimary(false);
			}
			if (UtilHelper.hasColumn(rs, "average_score")) {
				m.setAverage(rs.getInt("average_score"));
			}
			masterMetricsMap.put(m.getId(), m);
		}
		metricsList.addAll(masterMetricsMap.values());
		// display all metrics for testing purpose
		for (int i = 0; i < metricsList.size(); i++) {
			Metrics m = metricsList.get(i);
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug(
					m.getId() + " - " + m.getName() + " - " + m.getCategory() + " - " + m.getAverage() + " - " + m.getDirection() + " - "
							+ m.getScore());
		}

		return metricsList;
	}

	/**
	 * Retrieves the Metrics object for Team 
	 * @param companyId - Company ID
	 * @param initiativeTypeId - Initiative type ID
	 * 
	 * @param parsedFilterListResult - Map of filter objects
	 * @return A list of Metrics object
	 */
	@SuppressWarnings("unchecked")
	public List<Metrics> getDynamicTeamMetrics(int companyId, int initiativeTypeId, Map<String, Object> parsedFilterListResult) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricListForCategory = getMetricListForCategory(companyId, "Team");
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		if (initiativeTypeId > 0) {
			primaryMetricMap = getPrimaryMetricMap(companyId, initiativeTypeId);
		}
		try {

			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("R Connection Available : " + rCon.isConnected());
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Filling up parameters for rscript function");
			List<Integer> funcList = new ArrayList<>();
			List<Integer> posList = new ArrayList<>();
			List<Integer> zoneList = new ArrayList<>();
			List<Filter> filterList = (List<Filter>) parsedFilterListResult.get("filterList");
			for (Filter f : filterList) {
				if (f.getFilterName().equalsIgnoreCase("Function")) {
					funcList.addAll(f.getFilterValues().keySet());
				} else if (f.getFilterName().equalsIgnoreCase("Position")) {
					posList.addAll(f.getFilterValues().keySet());
				} else if (f.getFilterName().equalsIgnoreCase("Zone")) {
					zoneList.addAll(f.getFilterValues().keySet());
				}
			}
			rCon.assign("company_id", new int[] { companyId });
			rCon.assign("funcList", UtilHelper.getIntArrayFromIntegerList(funcList));
			rCon.assign("posList", UtilHelper.getIntArrayFromIntegerList(posList));
			rCon.assign("zoneList", UtilHelper.getIntArrayFromIntegerList(zoneList));

			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Calling the actual function in RScript TeamMetric");
			REXP teamMetricScore = rCon.parseAndEval("try(eval(TeamMetric(company_id, funcList, posList, zoneList)))");
			if (teamMetricScore.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).error("Error: " + teamMetricScore.asString());
				dch.releaseRcon();
				throw new Exception("Error: " + teamMetricScore.asString());
			} else {
				org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Metrics calculation completed for team " + teamMetricScore.asList());
			}

			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Parsing R function results");
			RList result = teamMetricScore.asList();
			REXPDouble metricIdResult = (REXPDouble) result.get("metric_id");
			int[] metricIdArray = metricIdResult.asIntegers();
			REXPDouble scoreResult = (REXPDouble) result.get("score");
			double[] scoreArray = scoreResult.asDoubles();
			REXPString dateOfCalculation = (REXPString) result.get("calc_time");
			String[] dateOfCalculationArray = dateOfCalculation.asStrings();
			Map<Integer, Integer> currentScoreMap = new HashMap<>();
			Map<Integer, Integer> previousScoreMap = new HashMap<>();
			Map<Integer, Date> dateOfCalcMap = new HashMap<>();

			for (int i = 0; i < metricIdArray.length; i++) {
				currentScoreMap.put(metricIdArray[i], (int) (scoreArray[i]));
				SimpleDateFormat sdf = new SimpleDateFormat(UtilHelper.dateTimeFormat);
				dateOfCalcMap.put(metricIdArray[i], UtilHelper.convertJavaDateToSqlDate((java.util.Date) (sdf.parse(dateOfCalculationArray[i]))));
			}

			metricsList = getMetricsList("Team", metricListForCategory, primaryMetricMap, previousScoreMap, currentScoreMap, dateOfCalcMap);
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("Successfully calculated metrics for the team");

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).error(
					"Exception while trying to retrieve metrics for category team and type ID " + initiativeTypeId, e);
		} finally {
			dch.releaseRcon();
		}
		return metricsList;
	}

	/**
	 * Retrieves the primary metric for the Initiative
	 * @param companyId - Company ID
	 * @param initiativeTypeId - Initiative type ID of the Initiative
	 * @return - The primary metric map containing the ID and name of the primary metric
	 */

	public Map<Integer, String> getPrimaryMetricMap(int companyId, int initiativeTypeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getInitiativePrimaryMetric(?)}");
			cstmt.setInt(1, initiativeTypeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				primaryMetricMap.put(rs.getInt("metric_id"), rs.getString("metric_name"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).error("Exception while getting the primary metrics for initiative", e);
		}
		return primaryMetricMap;
	}

	/**
	 * Retrieves the metric list for the specific category
	 * @param companyId - Company ID
	 * @param category - category for which the metric list is required
	 * @return - A map containing the metrics for the specified category
	 */

	public Map<Integer, String> getMetricListForCategory(int companyId, String category) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		Map<Integer, String> metricListForCategory = new HashMap<>();
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, category);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				metricListForCategory.put(rs.getInt("metric_id"), rs.getString("metric_name"));
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).error(
					"Exception while getting the metrics list for initiative of category" + category, e);
		}
		return metricListForCategory;
	}

	/**
	 * Retrieves the List of Metrics object
	 * @param category - Category of the Metrics(Team/Individual)
	 * @param metricListForCategory - List of Metrics for the specific Category
	 * @param primaryMetricMap - Map containing the primary Metric
	 * @param previousScoreMap - Map containing the previous score for the Metric
	 * @param currentScoreMap - Map containing the current score for the Metric
	 * @param dateOfCalculationMap - Map containing the date of calculation of the Metric
	 * @return - List of Metric Objects
	 */

	public List<Metrics> getMetricsList(String category, Map<Integer, String> metricListForCategory, Map<Integer, String> primaryMetricMap,
			Map<Integer, Integer> previousScoreMap, Map<Integer, Integer> currentScoreMap, Map<Integer, Date> dateOfCalculationMap) {
		List<Metrics> metricsList = new ArrayList<>();
		for (int id : metricListForCategory.keySet()) {
			Metrics m = new Metrics();
			m.setCategory(category);
			m.setId(id);
			m.setName(metricListForCategory.get(id));
			m.setScore(currentScoreMap.isEmpty() ? 0 : currentScoreMap.get(id));
			if (category == "Individual") {
				String direction = (currentScoreMap.isEmpty() || previousScoreMap.isEmpty())? "Neutral" : m.calculateMetricDirection(currentScoreMap.get(id), previousScoreMap.get(id));
				m.setDirection(direction);
			} else if (category == "Team") {
				// when metrics come from R the direction will always be neutral
				m.setDirection("Neutral");
			}
			if (primaryMetricMap.containsKey(id)) {
				m.setPrimary(true);
			} else {
				m.setPrimary(false);
			}
			m.setDateOfCalculation(dateOfCalculationMap.isEmpty() ? null : dateOfCalculationMap.get(id));
			metricsList.add(m);
		}
		return metricsList;
	}

	/**
	 * Retrieves a metrics list with score set as empty
	 * @param companyId - Company ID
	 * @param category - Should be set to Individual
	 * @return A map of metric ID and Metrics object
	 */
	public Map<Integer, Metrics> getEmptyMetricScoreList(int companyId, String category, Map<Integer, String> primaryMetricMap) {
		Map<Integer, Metrics> metricsMasterMap = new HashMap<>();
		MetricsHelper mh = new MetricsHelper();
		Map<Integer, String> metricListMap = mh.getMetricListForCategory(companyId, category);
		for (int metric_id : metricListMap.keySet()) {
			Metrics m = new Metrics();
			m.setId(metric_id);
			m.setName(metricListMap.get(metric_id));
			m.setCategory("Individual");
			m.setScore(0);
			m.setDateOfCalculation(Date.from(Instant.now()));
			m.setDirection("Neutral");
			m.setAverage(0);
			if (primaryMetricMap != null && primaryMetricMap.containsKey(metric_id)) {
				m.setPrimary(true);
			} else {
				m.setPrimary(false);
			}
			metricsMasterMap.put(metric_id, m);
		}

		return metricsMasterMap;
	}
}
