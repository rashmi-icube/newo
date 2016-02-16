package org.icube.owen.dashboard;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;

public class DashboardHelper extends TheBorg {

	/**
	 * Calculates the organizational metrics - for specific filter selection
	 * @param filter - filter object for which the metric is to be calculated
	 * @return list of metric objects 
	 */
	public List<Metrics> getFilterMetrics(Filter filter) {
		List<Metrics> orgMetricsList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {

			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricValueListForDimension(?, ?)}");
			cstmt.setInt(1, filter.getFilterValues().keySet().iterator().next().intValue());
			cstmt.setInt(2, filter.getFilterId());
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Metrics m = new Metrics();
				m.setId(rs.getInt("metric_id"));
				m.setName(rs.getString("metric_name"));
				m.setCategory("Team");
				m.setScore(rs.getInt("Current_Score"));
				m.setDirection(m.calculateMetricDirection(rs.getInt("Current_Score"), rs.getInt("Previous_Score")));
				m.setAverage(rs.getInt("Average_Score"));
				m.setDateOfCalculation(rs.getDate("calc_time"));
				orgMetricsList.add(m);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}

		return orgMetricsList;
	}

	/**
	 * Calculates the organizational metrics - for ALL selection
	 * @return list of metric objects 
	 */
	public List<Metrics> getOrganizationalMetrics() {
		List<Metrics> orgMetricsList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {

			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricValueListForOrganization()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Metrics m = new Metrics();
				m.setId(rs.getInt("metric_id"));
				m.setName(rs.getString("metric_name"));
				m.setCategory("Team");
				m.setScore(rs.getInt("Current_Score"));
				m.setDirection(m.calculateMetricDirection(rs.getInt("Current_Score"), rs.getInt("Previous_Score")));
				m.setAverage(rs.getInt("Average_Score"));
				m.setDateOfCalculation(rs.getDate("calc_time"));
				orgMetricsList.add(m);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}

		return orgMetricsList;
	}

	/**
	 * Returns all the details for the time series graph to be displayed on the HR dashboard
	 * 
	 * @param metricId1 - metric ID of the first metric type
	 * @param metricId2 - metric ID of the second metric type
	 * @param filter - filter selection 
	 * @return list of maps with data for the time series graph
	 */
	public List<Map<String, Object>> getTimeSeriesGraph(int metricId1, int metricId2, Filter filter) {
		List<Map<String, Object>> timeSeriesGraphMapList = new ArrayList<>();

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {

			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricValueForTimeSeries(?, ?, ?)}");
			cstmt.setInt(1, metricId1);
			cstmt.setInt(2, metricId2);
			cstmt.setInt(3, filter.getFilterValues().keySet().iterator().next().intValue());
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> timeSeriesGraph = new HashMap<>();
				timeSeriesGraph.put("metricId", rs.getInt("metric_id"));
				timeSeriesGraph.put("metricName", rs.getString("metric_name"));
				timeSeriesGraph.put("score", rs.getInt("Score"));
				timeSeriesGraph.put("dateOfCalculation", rs.getDate("calc_time"));

				timeSeriesGraphMapList.add(timeSeriesGraph);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}

		return timeSeriesGraphMapList;
	}

	/**
	 * Retrieves the list of alerts
	 * @return alert list
	 */
	public List<Alert> getAlertList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Alert> alertList = new ArrayList<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getAlertList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Alert a = new Alert();
				alertList.add(a.fillAlertDetails(rs));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}
		return alertList;
	}
}
