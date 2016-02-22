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
import org.icube.owen.survey.BatchList;

public class ExploreHelper extends TheBorg {
	// TODO Ravi : What is the point of creating class objects from ObjectFactory

	public List<List<Metrics>> getTeamMetricsData(List<List<Filter>> teamList) {
		List<List<Metrics>> result = new ArrayList<>();
		// use MetricsList.getInitiativeMetricsForTeam for every team

		// if all selections are ALL then it is a organizational team metric
		// call procedure getOrganizationMetricValue

		// if two of the filters are ALL then it is a dimension metric
		// call procedure getDimensionMetricValue

		// if none of the filters is ALL then it is a cube metric
		// call procedure getTeamMetricValue

		// else call metric.R

		return result;
	}

	public List<Map<String, Object>> getTeamTimeSeriesGraph(List<List<Filter>> teamList) {
		List<Map<String, Object>> timeSeriesGraphMapList = new ArrayList<>();
		// if all selections are ALL then it is a organizational team metric
		// call procedure getOrganizationMetricTimeSeries

		// if two of the filters are ALL then it is a dimension metric
		// call procedure getDimensionMetricTimeSeries

		// if none of the filters is ALL then it is a cube metric
		// call procedure getTeamMetricTimeseries

		// else nothing ... decide what to send

		return timeSeriesGraphMapList;
	}

	/**
	 * Retrieves the individual metrics data
	 * @param employeeList - list of employees selected
	 * @return map of employee linked to a list of metrics
	 */
	public Map<Employee, List<Metrics>> getIndividualMetricsData(List<Employee> employeeList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Employee, List<Metrics>> result = new HashMap<>();
		try {
			for (Employee e : employeeList) {
				List<Metrics> metricsList = new ArrayList<>();
				CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getIndividualMetricValue(?)}");
				cstmt.setInt(1, e.getEmployeeId());
				ResultSet rs = cstmt.executeQuery();
				while (rs.next()) {
					Metrics m = new Metrics();
					m.setId(rs.getInt("metric_id"));
					m.setName(rs.getString("metric_name"));
					m.setScore(rs.getInt("score"));
					m.setDateOfCalculation(rs.getDate("calc_time"));
					metricsList.add(m);
				}
				result.put(e, metricsList);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while retrieving individual metrics data", e);
		}
		return result;
	}

	public Map<Employee, List<Metrics>> getIndividualTimeSeriesGraph(List<Employee> employeeList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Employee, List<Metrics>> result = new HashMap<>();
		try {
			for (Employee e : employeeList) {

				List<Metrics> metricsList = new ArrayList<>();
				CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getIndividualMetricTimeSeries(?)}");
				cstmt.setInt(1, e.getEmployeeId());
				ResultSet rs = cstmt.executeQuery();
				while (rs.next()) {
					Metrics m = new Metrics();
					m.setId(rs.getInt("metric_id"));
					m.setName(rs.getString("metric_name"));
					m.setScore(rs.getInt("score"));
					m.setDateOfCalculation(rs.getDate("calc_time"));
					metricsList.add(m);
				}
				result.put(e, metricsList);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while retrieving individual metrics data", e);
		}
		return result;
	}
}
