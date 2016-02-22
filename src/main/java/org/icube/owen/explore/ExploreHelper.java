package org.icube.owen.explore;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsList;
import org.icube.owen.survey.BatchList;

public class ExploreHelper extends TheBorg {
	// TODO Ravi : What is the point of creating class objects from ObjectFactory

	private int countAll = 0, dimensionId = 0, funcId = 0, posId = 0, zoneId = 0;

	public Map<String, List<Metrics>> getTeamMetricsData(Map<String, List<Filter>> teamMap) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<String, List<Metrics>> result = new HashMap<>();

		for (String teamName : teamMap.keySet()) {
			List<Metrics> metricList = new ArrayList<>();
			List<Filter> filterList = teamMap.get(teamName);
			parseTeamMap(filterList);
			try {
				if (countAll == 3) {
					// if all selections are ALL then it is a organizational team metric
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getOrganizationMetricValue()}");
					ResultSet rs = cstmt.executeQuery();
					metricList = fillMetricsData(rs);
				} else if (countAll == 2) {
					// if two of the filters are ALL then it is a dimension metric
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getDimensionMetricValue(?)}");
					cstmt.setInt(1, dimensionId);
					ResultSet rs = cstmt.executeQuery();
					metricList = fillMetricsData(rs);
				} else if (countAll == 0) {
					// if none of the filters is ALL then it is a cube metric
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getTeamMetricValue(?, ?, ?)}");
					cstmt.setInt(1, funcId);
					cstmt.setInt(2, posId);
					cstmt.setInt(3, zoneId);
					ResultSet rs = cstmt.executeQuery();
					metricList = fillMetricsData(rs);

				} else {
					// else call metric.R
					MetricsList ml = new MetricsList();
					metricList = ml.getInitiativeMetricsForTeam(0, filterList);
				}
			} catch (SQLException e) {
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while getting team metrics data : " + teamMap.toString(), e);
			}
			result.put(teamName, metricList);
		}
		return result;
	}

	public Map<String, List<Metrics>> getTeamTimeSeriesGraph(Map<String, List<Filter>> teamMap) {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<String, List<Metrics>> result = new HashMap<>();

		for (String teamName : teamMap.keySet()) {
			List<Metrics> metricList = new ArrayList<>();
			List<Filter> filterList = teamMap.get(teamName);
			parseTeamMap(filterList);
			try {
				if (countAll == 3) {
					// if all selections are ALL then it is a organizational team metric
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getOrganizationMetricTimeSeries()}");
					ResultSet rs = cstmt.executeQuery();
					metricList = fillMetricsData(rs);
				} else if (countAll == 2) {
					// if two of the filters are ALL then it is a dimension metric
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getDimensionMetricTimeSeries(?)}");
					cstmt.setInt(1, dimensionId);
					ResultSet rs = cstmt.executeQuery();
					metricList = fillMetricsData(rs);
				} else if (countAll == 0) {
					// if none of the filters is ALL then it is a cube metric
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getTeamMetricTimeseries(?)}");
					cstmt.setInt(1, funcId);
					cstmt.setInt(2, posId);
					cstmt.setInt(3, zoneId);
					ResultSet rs = cstmt.executeQuery();
					metricList = fillMetricsData(rs);

				} else {
					// TODO else nothing ... decide what to send
				}
			} catch (SQLException e) {
				org.apache.log4j.Logger.getLogger(ExploreHelper.class).error("Exception while getting team metrics data : " + teamMap.toString(), e);
			}
			result.put(teamName, metricList);
		}

		return result;
	}

	private List<Metrics> fillMetricsData(ResultSet rs) throws SQLException {
		List<Metrics> metricsList = new ArrayList<>();
		while (rs.next()) {
			Metrics m = new Metrics();
			m.setId(rs.getInt("metric_id"));
			m.setName(rs.getString("metric_name"));
			m.setScore(rs.getInt("score"));
			m.setDateOfCalculation(rs.getDate("calc_time"));
			m.setCategory("Team");
			metricsList.add(m);
		}
		return metricsList;
	}

	private void parseTeamMap(List<Filter> filterList) {
		for (Filter filter : filterList) {
			if (filter.getFilterValues().containsKey(0)) {
				countAll++;
			} else {
				if (filter.getFilterName().equalsIgnoreCase("Function")) {
					funcId = filter.getFilterValues().keySet().iterator().next();
				} else if (filter.getFilterName().equalsIgnoreCase("Position")) {
					posId = filter.getFilterValues().keySet().iterator().next();
				} else if (filter.getFilterName().equalsIgnoreCase("Zone")) {
					zoneId = filter.getFilterValues().keySet().iterator().next();
				}
			}

			// check for if only two filter values are 0
			for (int filterValueId : filter.getFilterValues().keySet()) {
				if (filterValueId > 0) {
					dimensionId = filterValueId;
				}
			}
		}
	}

	/**
	 * Retrieves the individual metrics data
	 * @param employeeList - list of employees selected
	 * @return map of employee linked to a list of metrics
	 */
	public Map<Employee, List<Metrics>> getIndividualMetricsData(List<Employee> employeeList) {
		Map<Employee, List<Metrics>> result = getIndividualsData(employeeList, "getIndividualMetricValue");
		return result;
	}

	public Map<Employee, List<Metrics>> getIndividualTimeSeriesGraph(List<Employee> employeeList) {
		Map<Employee, List<Metrics>> result = getIndividualsData(employeeList, "getIndividualMetricTimeSeries");
		return result;
	}

	private Map<Employee, List<Metrics>> getIndividualsData(List<Employee> employeeList, String sqlProcedureName) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Employee, List<Metrics>> result = new HashMap<>();
		try {
			for (Employee e : employeeList) {
				List<Metrics> metricsList = new ArrayList<>();
				CallableStatement cstmt = dch.mysqlCon.prepareCall("{call " + sqlProcedureName + "(?)}");
				cstmt.setInt(1, e.getEmployeeId());
				ResultSet rs = cstmt.executeQuery();
				metricsList = fillMetricsData(rs);
				result.put(e, metricsList);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while retrieving individual metrics data", e);
		}
		return result;
	}

}
