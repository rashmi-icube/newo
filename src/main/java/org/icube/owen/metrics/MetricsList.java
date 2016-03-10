package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
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

public class MetricsList extends TheBorg {

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative for teams 
	 * @param filterList - list of filters applicable to the initiative 
	 * @param initiativeTypeId - ID of the type of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetricsForTeam(int initiativeTypeId, List<Filter> filterList) {
		List<Metrics> metricsList = new ArrayList<>();
		MetricsHelper mh = new MetricsHelper();
		Map<String, Object> parsedFilterListResult = UtilHelper.parseFilterList(filterList);
		try {
			if ((int) parsedFilterListResult.get("funcListSize") == 1 && (int) parsedFilterListResult.get("posListSize") == 1
					&& (int) parsedFilterListResult.get("zoneListSize") == 1) {
				if ((int) parsedFilterListResult.get("countAll") == 1) {
					metricsList = mh.getDynamicTeamMetrics(initiativeTypeId, parsedFilterListResult);
				} else {
					metricsList = mh.getTeamMetricsList(filterList);
				}
			} else {
				metricsList = mh.getDynamicTeamMetrics(initiativeTypeId, parsedFilterListResult);
			}
		} catch (Exception e) {

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
		MetricsHelper mh = new MetricsHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, Integer> currentScoreMap = new HashMap<>();
		Map<Integer, Integer> previousScoreMap = new HashMap<>();
		Map<Integer, Date> dateOfCalcMap = new HashMap<>();

		try {
			Map<Integer, String> metricListForCategory = mh.getMetricListForCategory("Individual");
			Map<Integer, String> primaryMetricMap = mh.getPrimaryMetricMap(initiativeTypeId);

			List<Integer> empIdList = new ArrayList<>();
			for (Employee e : partOfEmployeeList) {
				empIdList.add(e.getEmployeeId());
			}

			try {
				CallableStatement cs = dch.mysqlCon.prepareCall("{call getIndividualInitiativeMetricValueAggregate(?)}");
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
			metricsList = mh.getMetricsList("Individual", metricListForCategory, primaryMetricMap, previousScoreMap, currentScoreMap, dateOfCalcMap);
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Successfully calculated metrics for the team");
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category individual and type ID " + initiativeTypeId, e);
		}

		return metricsList;

	}

}
