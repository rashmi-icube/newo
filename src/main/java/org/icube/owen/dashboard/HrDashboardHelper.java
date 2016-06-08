package org.icube.owen.dashboard;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsHelper;

public class HrDashboardHelper extends TheBorg {
	/**
	 * Calculates the organizational metrics - for specific filter selection
	 * @param companyId - Company ID
	 * @param filter - filter object for which the metric is to be calculated
	 * @return list of metric objects 
	 */

	public List<Metrics> getFilterMetrics(int companyId, Filter filter) {
		List<Metrics> dimensionMetricsList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Entering getFilterMetrics using procedure getDimensionMetricValueAggregate");
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getDimensionMetricValueAggregate(?, ?)}");
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Filter Value ID : " + filter.getFilterValues().keySet().iterator().next().intValue());
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Filter ID : " + filter.getFilterId());
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Filter Name : " + filter.getFilterName());
			cstmt.setInt("dimvalid", filter.getFilterValues().keySet().iterator().next().intValue());
			cstmt.setInt("dimid", filter.getFilterId());
			ResultSet rs = cstmt.executeQuery();
			MetricsHelper mh = new MetricsHelper();
			dimensionMetricsList = mh.fillMetricsData(companyId, rs, null, "Team");
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}
		org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Successfully calculated the filter metrics " + dimensionMetricsList.size());
		return dimensionMetricsList;
	}

	/**
	 * Calculates the organizational metrics - for ALL selection
	 * @param companyId - Company ID
	 * @return list of metric objects 
	 */

	public List<Metrics> getOrganizationalMetrics(int companyId) {
		List<Metrics> orgMetricsList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {

			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getOrganizationMetricValueAggregate()}");
			ResultSet rs = cstmt.executeQuery();
			MetricsHelper mh = new MetricsHelper();
			orgMetricsList = mh.fillMetricsData(companyId, rs, null, "Team");
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}

		return orgMetricsList;
	}

	/**
	 * Returns all the details for the time series graph to be displayed on the HR dashboard
	 * @param companyId - Company ID
	 * @param filter - filter selection
	 * @return Map of metric Id and List of map of calculation date and metric score for the time series graph
	 */

	public Map<Integer, List<Map<Date, Integer>>> getTimeSeriesGraph(int companyId, Filter filter) {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Entering getTimeSeriesGraph using procedure getDimensionMetricTimeSeries");
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getDimensionMetricTimeSeries(?)}");
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Filter Value ID : " + filter.getFilterValues().keySet().iterator().next().intValue());
			cstmt.setInt(1, filter.getFilterValues().keySet().iterator().next().intValue());
			ResultSet rs = cstmt.executeQuery();
			result = getTimeSeriesMap(rs);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).error("Exception while retrieving metrics", e);
		}
		org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Successfully got time series graph for dimension" + result.size());
		return result;
	}

	/**
	 * @param rs - Resultset
	 * @return - Map of Metric ID and list of map of calculation date and score
	 * @throws SQLException
	 */
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
	 * @param companyId - Company ID
	 * @return Map of metric Id and List of map of calculation date and metric score for the time series graph
	 */

	public Map<Integer, List<Map<Date, Integer>>> getOrganizationTimeSeriesGraph(int companyId) {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getOrganizationMetricTimeSeries()}");
			ResultSet rs = cstmt.executeQuery();
			result = getTimeSeriesMap(rs);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}
		org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Result for organization time series graph : " + result.toString());
		return result;

	}

	/**
	 * Retrieves the list of alerts
	 * @param companyId - Company ID
	 * @return alert list
	 */

	public List<Alert> getAlertList(int companyId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Alert> alertList = new ArrayList<>();
		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getAlertList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Alert a = new Alert();
				alertList.add(a.fillAlertDetails(companyId, rs));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}
		return alertList;
	}
}
