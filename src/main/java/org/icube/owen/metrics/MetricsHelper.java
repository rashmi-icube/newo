package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

public class MetricsHelper {
	@SuppressWarnings("unchecked")
	public List<Metrics> getTeamMetricsList(int initiativeTypeId, Map<String, Object> parsedFilterListResult, boolean previousScoreNeeded)
			throws SQLException {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Metrics> metricList = new ArrayList<>();
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		if (initiativeTypeId > 0) {
			primaryMetricMap = getPrimaryMetricMap(initiativeTypeId);
		}
		if ((int) parsedFilterListResult.get("countAll") == 3) {
			// if all selections are ALL then it is a organizational team metric
			CallableStatement cstmt;
			if (previousScoreNeeded) {
				cstmt = dch.mysqlCon.prepareCall("{call getOrganizationMetricValueAggregate()}");
			} else {
				cstmt = dch.mysqlCon.prepareCall("{call getOrganizationMetricValue()}");
			}
			ResultSet rs = cstmt.executeQuery();
			metricList = fillMetricsData(initiativeTypeId, rs, primaryMetricMap, "Team");
		} else if ((int) parsedFilterListResult.get("countAll") == 2) {
			// if two of the filters are ALL then it is a dimension metric
			CallableStatement cstmt;
			if (previousScoreNeeded) {
				cstmt = dch.mysqlCon.prepareCall("{call getDimensionMetricValueAggregate(?,?)}");
				cstmt.setInt(1, (int) parsedFilterListResult.get("dimensionValueId"));
				cstmt.setInt(2, (int) parsedFilterListResult.get("dimensionId"));
			} else {
				cstmt = dch.mysqlCon.prepareCall("{call getDimensionMetricValue(?)}");
				cstmt.setInt(1, (int) parsedFilterListResult.get("dimensionValueId"));
			}

			ResultSet rs = cstmt.executeQuery();
			metricList = fillMetricsData(initiativeTypeId, rs, primaryMetricMap, "Team");
		} else if ((int) parsedFilterListResult.get("countAll") == 0) {
			// if none of the filters is ALL then it is a cube metric
			CallableStatement cstmt;
			if (previousScoreNeeded) {
				cstmt = dch.mysqlCon.prepareCall("{call getTeamMetricValueAggregate(?, ?, ?)}");
			} else {
				cstmt = dch.mysqlCon.prepareCall("{call getTeamMetricValue(?, ?, ?)}");
			}
			cstmt.setInt(1, (int) parsedFilterListResult.get("funcId"));
			cstmt.setInt(2, (int) parsedFilterListResult.get("posId"));
			cstmt.setInt(3, (int) parsedFilterListResult.get("zoneId"));
			ResultSet rs = cstmt.executeQuery();
			metricList = fillMetricsData(initiativeTypeId, rs, primaryMetricMap, "Team");

		} else {
			// else call metric.R
			MetricsList ml = new MetricsList();
			metricList = ml.getInitiativeMetricsForTeam(0, (List<Filter>) parsedFilterListResult.get("filterList"));
		}

		return metricList;
	}

	public List<Metrics> fillMetricsData(int initiativeTypeId, ResultSet rs, Map<Integer, String> primaryMetricMap, String category)
			throws SQLException {
		List<Metrics> metricsList = new ArrayList<>();
		if (rs != null) {
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
				metricsList.add(m);
			}
		} else {
			org.apache.log4j.Logger.getLogger(MetricsHelper.class).debug("No metrics returned");
		}
		return metricsList;
	}

	@SuppressWarnings("unchecked")
	public List<Metrics> getDynamicTeamMetrics(int initiativeTypeId, Map<String, Object> parsedFilterListResult) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricListForCategory = getMetricListForCategory("Team");
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		if (initiativeTypeId > 0) {
			primaryMetricMap = getPrimaryMetricMap(initiativeTypeId);
		}
		try {
			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("R Path for eval " + s);
			dch.rCon.eval(s);
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Filling up parameters for rscript function");
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
			dch.rCon.assign("funcList", UtilHelper.getIntArrayFromIntegerList(funcList));
			dch.rCon.assign("posList", UtilHelper.getIntArrayFromIntegerList(posList));
			dch.rCon.assign("zoneList", UtilHelper.getIntArrayFromIntegerList(zoneList));

			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Calling the actual function in RScript TeamMetric");
			REXP teamMetricScore = dch.rCon.parseAndEval("try(eval(TeamMetric(funcList, posList, zoneList)))");
			if (teamMetricScore.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(MetricsList.class).error("Error: " + teamMetricScore.asString());
				throw new Exception("Error: " + teamMetricScore.asString());
			} else {
				org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Metrics calculation completed for team " + teamMetricScore.asList());
			}

			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Parsing R function results");
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
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Successfully calculated metrics for the team");

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category team and type ID " + initiativeTypeId, e);
		}
		return metricsList;
	}

	/**
	 * Retrieves the primary metric for the Initiative
	 * @param initiativeTypeId - Initiative type ID of the Initiative
	 * @return - The primary metric map containing the ID and name of the primary metric
	 */
	public Map<Integer, String> getPrimaryMetricMap(int initiativeTypeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getInitiativePrimaryMetric(?)}");
			cstmt.setInt(1, initiativeTypeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				primaryMetricMap.put(rs.getInt("metric_id"), rs.getString("metric_name"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error("Exception while getting the primary metrics for initiative", e);
		}
		return primaryMetricMap;
	}

	/**
	 * Retrieves the metric list for the specific category
	 * @param category - category for which the metric list is required
	 * @return - A map containing the metrics for the specified category
	 */
	public Map<Integer, String> getMetricListForCategory(String category) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> metricListForCategory = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, category);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				metricListForCategory.put(rs.getInt("metric_id"), rs.getString("metric_name"));
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
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
			m.setScore(currentScoreMap.get(id));
			if (category == "Individual") {
				String direction = m.calculateMetricDirection(currentScoreMap.get(id), previousScoreMap.get(id));
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
			m.setDateOfCalculation(dateOfCalculationMap.get(id));
			metricsList.add(m);
		}
		return metricsList;
	}
}