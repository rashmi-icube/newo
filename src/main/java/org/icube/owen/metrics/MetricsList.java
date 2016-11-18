package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
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
	 * @param companyId - Company ID
	 * @param filterList - list of filters applicable to the initiative 
	 * @param initiativeTypeId - ID of the type of initiative
	 * @return list of metrics objects
	 */

	public List<Metrics> getInitiativeMetricsForTeam(int companyId, int initiativeTypeId, List<Filter> filterList) {
		org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Entering getInitiativeMetricsForTeam");
	
		List<Metrics> metricsList = new ArrayList<>();
		MetricsHelper mh = new MetricsHelper();
		Map<String, Object> parsedFilterListResult = UtilHelper.parseFilterList(filterList);
		try {
			if ((int) parsedFilterListResult.get("funcListSize") == 1 && (int) parsedFilterListResult.get("posListSize") == 1
					&& (int) parsedFilterListResult.get("zoneListSize") == 1) {
				if ((int) parsedFilterListResult.get("countAll") == 1) {
					metricsList = mh.getDynamicTeamMetrics(companyId, initiativeTypeId, parsedFilterListResult);
				} else {
					metricsList = mh.getTeamMetricsList(companyId, initiativeTypeId, parsedFilterListResult, true);
				}
			} else {
				metricsList = mh.getDynamicTeamMetrics(companyId, initiativeTypeId, parsedFilterListResult);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category team and type ID " + initiativeTypeId, e);
		}
		Collections.sort(metricsList, (o1, o2) -> Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId())));
		return metricsList;
	}

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative for individual
	 * @param companyId - Company ID
	 * @param partOfEmployeeList - list of employees to be part of the initiative
	 * @param initiativeTypeId - ID of the kind of initiative
	 * @return list of metrics objects
	 */
	
	public List<Metrics> getInitiativeMetricsForIndividual(int companyId, int initiativeTypeId, List<Employee> partOfEmployeeList) {
		org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Entering getInitiativeMetricsForIndividual");
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		MetricsHelper mh = new MetricsHelper();
		List<Metrics> metricsList = new ArrayList<>();
		org.apache.log4j.Logger.getLogger(MetricsList.class).info("HashMap created!!!");
		Map<Integer, Integer> currentScoreMap = new HashMap<>();
		Map<Integer, Integer> previousScoreMap = new HashMap<>();
		Map<Integer, Date> dateOfCalcMap = new HashMap<>();

		try {
			Map<Integer, String> metricListForCategory = mh.getMetricListForCategory(companyId, "Individual");
			Map<Integer, String> primaryMetricMap = mh.getPrimaryMetricMap(companyId, initiativeTypeId);

			List<Integer> empIdList = new ArrayList<>();
			for (Employee e : partOfEmployeeList) {
				empIdList.add(e.getEmployeeId());
			}

			try {
				org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Calling getIndividualInitiativeMetricValueAggregate");
				CallableStatement cs = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getIndividualInitiativeMetricValueAggregate(?)}");
				int empId = empIdList.get(0);
				cs.setInt(1, empId);
				ResultSet rs = cs.executeQuery();
				while (rs.next()) {
					previousScoreMap.put(rs.getInt("metric_id"), rs.getInt("previous_score"));
					currentScoreMap.put(rs.getInt("metric_id"), rs.getInt("current_score"));
				}
				cs.close();
				rs.close();
			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(MetricsList.class).error(
						"Exception while trying to metrics list for category individual and type ID " + initiativeTypeId, e);
			}
			metricsList = mh.getMetricsList("Individual", metricListForCategory, primaryMetricMap, previousScoreMap, currentScoreMap, dateOfCalcMap);
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Successfully calculated metrics for the individual");
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category individual and type ID " + initiativeTypeId, e);
		}
		
		Collections.sort(metricsList, (o1, o2) -> Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId())));
		return metricsList;

	}

}
