package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.RList;

public class MetricsList extends TheBorg {

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative for teams 
	 * 
	 * @param filterList - list of filters applicable to the initiative 
	 * @param initiativeTypeId - ID of the type of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetricsForTeam(int initiativeTypeId, List<Filter> filterList) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = new HashMap<>();

		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, "Team");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				metricsTypeMap.put(rs.getInt(1), rs.getString(2));
			}

			Map<Integer, String> primaryMetricMap = new HashMap<>();
			cstmt = dch.mysqlCon.prepareCall("{call GetMetricForInitiative(?)}");
			cstmt.setInt(1, initiativeTypeId);
			rs = cstmt.executeQuery();
			while (rs.next()) {
				primaryMetricMap.put(rs.getInt(1), rs.getString(2));
			}

			dch.rCon.eval("source(\"/Users/apple/Documents/workspace/owen/scripts/metric.r\")");

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
			dch.rCon.assign("funcList", this.getIntArrayFromIntegerList(funcList));
			dch.rCon.assign("posList", this.getIntArrayFromIntegerList(posList));
			dch.rCon.assign("zoneList", this.getIntArrayFromIntegerList(zoneList));
			REXP teamMetricScore = dch.rCon.parseAndEval("try(eval(TeamMetric(funcList, posList, zoneList)))");
			if (teamMetricScore.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(MetricsList.class).error("Error: " + teamMetricScore.asString());
			} else {
				org.apache.log4j.Logger.getLogger(MetricsList.class).debug(teamMetricScore.asList());
			}

			RList result = teamMetricScore.asList();
			REXPDouble metricIdResult = (REXPDouble) result.get("metric_id");
			int[] metricIdArray = metricIdResult.asIntegers();
			REXPDouble scoreResult = (REXPDouble) result.get("score");
			double[] scoreArray = scoreResult.asDoubles();
			Map<Integer, Integer> metricScoreMap = new HashMap<>();

			// TODO hpatel change to integer directly without rounding once rscript is updated
			for (int i = 0; i < metricIdArray.length; i++) {
				metricScoreMap.put(metricIdArray[i], (int) (Math.round((Double) scoreArray[i])));
			}

			for (int id : metricsTypeMap.keySet()) {
				Metrics m = new Metrics();
				m.setCategory("Team");
				m.setName(metricsTypeMap.get(id));
				m.setScore(metricScoreMap.get(id));
				if (primaryMetricMap.containsKey(id)) {
					m.setPrimary(true);
				} else {
					m.setPrimary(false);
				}
				metricsList.add(m);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category team and type ID " + initiativeTypeId, e);
		}

		return metricsList;

	}

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative for individuals
	 * 
	 * @param partOfEmployeeList - list of employees to be part of the initiative
	 * @param initiativeTypeId - ID of the kind of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetricsForIndividual(int initiativeTypeId, List<Employee> partOfEmployeeList) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = new HashMap<>();

		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, "Individual");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				metricsTypeMap.put(rs.getInt(1), rs.getString(2));
			}

			Map<Integer, String> primaryMetricMap = new HashMap<>();
			cstmt = dch.mysqlCon.prepareCall("{call GetMetricForInitiative(?)}");
			cstmt.setInt(1, initiativeTypeId);
			rs = cstmt.executeQuery();
			while (rs.next()) {
				primaryMetricMap.put(rs.getInt(1), rs.getString(2));
			}

			dch.rCon.eval("source(\"/Users/apple/Documents/workspace/owen/scripts/metric.r\")");
			List<Integer> empIdList = new ArrayList<>();
			for (Employee e : partOfEmployeeList) {
				empIdList.add(e.getEmployeeId());
			}

			// TODO hpatel figure out how to pass multiple employee IDs
			int temp = empIdList.get(0);
			empIdList.clear();
			empIdList.add(temp);

			dch.rCon.assign("empIdList", this.getIntArrayFromIntegerList(empIdList));

			REXP individualMetricScore = dch.rCon.parseAndEval("try(eval(IndividualMetric(empIdList)))");
			if (individualMetricScore.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(MetricsList.class).error("Error: " + individualMetricScore.asString());
			} else {
				org.apache.log4j.Logger.getLogger(MetricsList.class).debug(individualMetricScore.asList());
			}

			RList result = individualMetricScore.asList();
			REXPDouble metricIdResult = (REXPDouble) result.get("metric_id");
			int[] metricIdArray = metricIdResult.asIntegers();
			REXPDouble scoreResult = (REXPDouble) result.get("score");
			double[] scoreArray = scoreResult.asDoubles();
			Map<Integer, Integer> metricScoreMap = new HashMap<>();

			// TODO hpatel change to integer directly without rounding once rscript is updated
			for (int i = 0; i < metricIdArray.length; i++) {
				metricScoreMap.put(metricIdArray[i], (int) (Math.round((Double) scoreArray[i])));
			}

			for (int id : metricsTypeMap.keySet()) {
				Metrics m = new Metrics();
				m.setCategory("Individual");
				m.setName(metricsTypeMap.get(id));
				m.setScore(metricScoreMap.get(id));
				if (primaryMetricMap.containsKey(id)) {
					m.setPrimary(true);
				} else {
					m.setPrimary(false);
				}
				metricsList.add(m);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category individual and type ID " + initiativeTypeId, e);
		}

		return metricsList;

	}

	private static int[] getIntArrayFromIntegerList(List<Integer> integerList) {
		int[] result = new int[integerList.size()];
		for (int i = 0; i <= integerList.size() - 1; i++) {
			result[i] = integerList.get(i);
		}
		return result;
	}

}
