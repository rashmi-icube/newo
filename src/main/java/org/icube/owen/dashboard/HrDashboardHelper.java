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
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getDimensionMetricValueAggregate(?, ?)}")) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Entering getFilterMetrics using procedure getDimensionMetricValueAggregate");
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Filter Value ID : " + filter.getFilterValues().keySet().iterator().next().intValue());
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Filter ID : " + filter.getFilterId());
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug("Filter Name : " + filter.getFilterName());
			cstmt.setInt("dimvalid", filter.getFilterValues().keySet().iterator().next().intValue());
			cstmt.setInt("dimid", filter.getFilterId());
			try (ResultSet rs = cstmt.executeQuery()) {
				MetricsHelper mh = new MetricsHelper();
				dimensionMetricsList = mh.fillMetricsData(companyId, rs, null, "Team");
			}
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
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getOrganizationMetricValueAggregate()}");
				ResultSet rs = cstmt.executeQuery()) {
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
		org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).info("HashMap created!!!");
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getDimensionMetricTimeSeries(?)}")) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Entering getTimeSeriesGraph using procedure getDimensionMetricTimeSeries");

			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).debug(
					"Filter Value ID : " + filter.getFilterValues().keySet().iterator().next().intValue());
			cstmt.setInt(1, filter.getFilterValues().keySet().iterator().next().intValue());
			try (ResultSet rs = cstmt.executeQuery();) {
				result = getTimeSeriesMap(rs);
			}
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
		org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).info("HashMap created!!!");
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();

		while (rs.next()) {
			if (result.containsKey(rs.getInt("metric_id"))) {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).info("HashMap created!!!");
				Map<Date, Integer> metricScoreMap = new HashMap<>();
				metricScoreMapList = result.get(rs.getInt("metric_id"));
				metricScoreMap.put(rs.getDate("calc_time"), rs.getInt("Score"));
				metricScoreMapList.add(metricScoreMap);
				result.put(rs.getInt("metric_id"), metricScoreMapList);
			} else {
				List<Map<Date, Integer>> metricScoreMapList = new ArrayList<>();
				org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).info("HashMap created!!!");
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
		org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).info("HashMap created!!!");
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getOrganizationMetricTimeSeries()}");
				ResultSet rs = cstmt.executeQuery()) {

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
		dch.refreshCompanyConnection(companyId);
		List<Alert> alertList = new ArrayList<>();
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall("{call getAlertList()}");
				ResultSet rs = cstmt.executeQuery()) {
			while (rs.next()) {
				Alert a = new Alert();
				alertList.add(a.fillAlertDetails(companyId, rs));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(HrDashboardHelper.class).error("Exception while retrieving organization level metrics", e);
		}
		return alertList;
	}

	// TODO : RASHMI ::: DUMMY FUNCTIONS FOR TENTATIVE DASHBOARD
	/*public Map<String, ReportObject> getReportData1(String group, String subGroup) {
		Map<String, ReportObject> subResult = new HashMap<>();
		ReportObject ro = new ReportObject();
		ro.setStronglyAgree(20);
		ro.setAgree(20);
		ro.setNeutral(20);
		ro.setDisagree(20);
		ro.setStronglyDisagree(20);
		subResult.put("All Day Dining", ro);
		ro.setStronglyAgree(40);
		ro.setAgree(30);
		ro.setNeutral(20);
		ro.setDisagree(10);
		ro.setStronglyDisagree(0);
		subResult.put("Bakery", ro);
		ro.setStronglyAgree(50);
		ro.setAgree(10);
		ro.setNeutral(10);
		ro.setDisagree(10);
		ro.setStronglyDisagree(20);
		subResult.put("Banquets Team", ro);
		return subResult;
	}

	public Map<String, ReportObject> getReportData2(String group, String subGroup) {
		Map<String, ReportObject> subResult = new HashMap<>();
		ReportObject ro = new ReportObject();
		ro.setStronglyAgree(50);
		ro.setAgree(10);
		ro.setNeutral(10);
		ro.setDisagree(10);
		ro.setStronglyDisagree(20);
		subResult.put("All Day Dining", ro);
		ro.setStronglyAgree(20);
		ro.setAgree(20);
		ro.setNeutral(20);
		ro.setDisagree(20);
		ro.setStronglyDisagree(20);
		subResult.put("Bakery", ro);
		ro.setStronglyAgree(40);
		ro.setAgree(30);
		ro.setNeutral(20);
		ro.setDisagree(10);
		ro.setStronglyDisagree(0);
		subResult.put("Banquets Team", ro);
		return subResult;
	}*/

	public Map<String, ReportObject> getReportData1(String group, String subGroup) {
		Map<String, ReportObject> subResult = new HashMap<>();
		ReportObject ro = new ReportObject();
		ro.setStronglyAgree(20);
		ro.setAgree(20);
		ro.setNeutral(20);
		ro.setDisagree(20);
		ro.setStronglyDisagree(20);
		subResult.put("Dining", ro);
		ReportObject ro1 = new ReportObject();
		ro1.setStronglyAgree(40);
		ro1.setAgree(30);
		ro1.setNeutral(20);
		ro1.setDisagree(10);
		ro1.setStronglyDisagree(0);
		subResult.put("Bakery", ro1);
		ReportObject ro2 = new ReportObject();
		ro2.setStronglyAgree(50);
		ro2.setAgree(10);
		ro2.setNeutral(10);
		ro2.setDisagree(10);
		ro2.setStronglyDisagree(20);
		subResult.put("Banquets", ro2);

		return subResult;
	}

	public Map<String, ReportObject> getReportData2(String group, String subGroup) {
		Map<String, ReportObject> subResult = new HashMap<>();
		ReportObject ro = new ReportObject();
		ro.setStronglyAgree(50);
		ro.setAgree(10);
		ro.setNeutral(10);
		ro.setDisagree(10);
		ro.setStronglyDisagree(20);
		subResult.put("Dining", ro);
		ReportObject ro1 = new ReportObject();
		ro1.setStronglyAgree(20);
		ro1.setAgree(20);
		ro1.setNeutral(20);
		ro1.setDisagree(20);
		ro1.setStronglyDisagree(20);
		subResult.put("Bakery", ro1);
		ReportObject ro2 = new ReportObject();
		ro2.setStronglyAgree(40);
		ro2.setAgree(30);
		ro2.setNeutral(20);
		ro2.setDisagree(10);
		ro2.setStronglyDisagree(0);
		subResult.put("Banquets", ro2);
		return subResult;
	}
}
