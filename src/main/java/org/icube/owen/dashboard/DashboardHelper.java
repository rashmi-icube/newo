package org.icube.owen.dashboard;

import java.sql.CallableStatement;
import java.sql.Date;
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
	 * @param filter - filter selection
	 * @return Map of metric Id and List of map of calculation date and metric score for the time series graph
	 */
	public Map<Integer, List<Map<Date, Integer>>> getTimeSeriesGraph(Filter filter) {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {

			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getDimensionMetricTimeSeries(?)}");
			cstmt.setInt(1, filter.getFilterValues().keySet().iterator().next().intValue());
			ResultSet rs = cstmt.executeQuery();
			result = getTimeSeriesMap(rs);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DashboardHelper.class).error("Exception while retrieving metrics", e);
		}

		return result;
	}

	private Map<Integer, List<Map<Date, Integer>>> getTimeSeriesMap(ResultSet rs) throws SQLException {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		
		while (rs.next()) {
			if (result.containsKey(rs.getInt("metric_id"))) {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				Map<Date, Integer> metricScoreMap = new HashMap<>();
				metricScoreMapList = result.get(rs.getInt("metric_id"));
				metricScoreMap.put(rs.getDate("calc_time"), rs.getInt("Score"));
				metricScoreMapList.add(metricScoreMap);
				result.put(rs.getInt("metric_id"), metricScoreMapList);
			} else {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				Map<Date, Integer> metricScoreMap = new HashMap<>();
				metricScoreMap.put(rs.getDate("calc_time"), rs.getInt("Score"));
				metricScoreMapList.add(metricScoreMap);
				result.put(rs.getInt("metric_id"), metricScoreMapList);
			}
		}
		return result;
	}

	/**
	 * Returns all the details for the time series graph for Organization to be displayed on the HR dashboard
	 * @return Map of metric Id and List of map of calculation date and metric score for the time series graph
	 */

	public Map<Integer, List<Map<Date, Integer>>> getOrganizationTimeSeriesGraph() {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getOrganizationMetricTimeSeries()}");
			ResultSet rs = cstmt.executeQuery();
			result = getTimeSeriesMap(rs);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}
		return result;

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
