package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

public class MetricsList extends TheBorg {

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative for teams 
	 * @param filterList - list of filters applicable to the initiative 
	 * @param initiativeTypeId - ID of the type of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetricsForTeam(int initiativeTypeId, List<Filter> filterList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Metrics> metricsList = new ArrayList<>();
		try {
			Map<Integer, String> metricListForCategory = getMetricListForCategory("Team");
			Map<Integer, String> primaryMetricMap = new HashMap<>();
			if (initiativeTypeId > 0) {
				primaryMetricMap = getPrimaryMetricMap(initiativeTypeId);
			}

			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("R Path for eval " + s);
			dch.rCon.eval(s);
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Filling up parameters for rscript function");
			List<Integer> funcList = new ArrayList<>();
			List<Integer> posList = new ArrayList<>();
			List<Integer> zoneList = new ArrayList<>();
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
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
	 * Retrieves the metrics for the filters chosen while creating the initiative for individual
	 * @param partOfEmployeeList - list of employees to be part of the initiative
	 * @param initiativeTypeId - ID of the kind of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetricsForIndividual(int initiativeTypeId, List<Employee> partOfEmployeeList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, Integer> currentScoreMap = new HashMap<>();
		Map<Integer, Integer> previousScoreMap = new HashMap<>();
		Map<Integer, Date> dateOfCalcMap = new HashMap<>();

		try {
			Map<Integer, String> metricListForCategory = getMetricListForCategory("Individual");
			Map<Integer, String> primaryMetricMap = getPrimaryMetricMap(initiativeTypeId);

			List<Integer> empIdList = new ArrayList<>();
			for (Employee e : partOfEmployeeList) {
				empIdList.add(e.getEmployeeId());
			}

			try {
				CallableStatement cs = dch.mysqlCon.prepareCall("{call getMetricValueListForIndividualInitiative(?)}");
				int empId = empIdList.get(0);
				cs.setInt(1, empId);
				ResultSet rs = cs.executeQuery();
				while (rs.next()) {
					previousScoreMap.put(rs.getInt("metric_id"), rs.getInt("previous_score"));
					currentScoreMap.put(rs.getInt("metric_id"), rs.getInt("current_score"));

				}
			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(MetricsList.class).error(
						"Exception while trying to metrics list for category individual and type ID " + initiativeTypeId, e);
			}
			metricsList = getMetricsList("Individual", metricListForCategory, primaryMetricMap, previousScoreMap, currentScoreMap, dateOfCalcMap);
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Successfully calculated metrics for the team");
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category individual and type ID " + initiativeTypeId, e);
		}

		return metricsList;

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

	/**
	 * Retrieves the primary metric for the Initiative
	 * @param initiativeTypeId - Initiative type ID of the Initiative
	 * @return - The primary metric map containing the ID and name of the primary metric
	 */
	public Map<Integer, String> getPrimaryMetricMap(int initiativeTypeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> primaryMetricMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call GetMetricForInitiative(?)}");
			cstmt.setInt(1, initiativeTypeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				primaryMetricMap.put(rs.getInt(1), rs.getString(2));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error("Exception while getting the metrics for initiative", e);
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
				metricListForCategory.put(rs.getInt(1), rs.getString(2));
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while getting the metrics list for initiative of category" + category, e);
		}
		return metricListForCategory;
	}

}
