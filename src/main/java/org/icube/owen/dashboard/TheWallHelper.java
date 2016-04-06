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
import org.icube.owen.helper.UtilHelper;

public class TheWallHelper extends TheBorg {

	/**
	 * Get list of individuals for the wall 
	 */
	public List<Map<String, Object>> getIndividualWallFeed(int metricId, String direction, int percentage, int pageNumber, int pageSize,
			List<Filter> filterList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, Object> parsedFilterMap = new HashMap<>();
		if (filterList == null || filterList.isEmpty()) {
			parsedFilterMap.put("funcId", 0);
			parsedFilterMap.put("posId", 0);
			parsedFilterMap.put("zoneId", 0);
		} else {
			parsedFilterMap = UtilHelper.parseFilterList(filterList);
		}
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getWallFeedIndividual(?,?,?,?,?,?,?,?)}");
			cstmt.setInt("fun", (int) parsedFilterMap.get("funcId"));
			cstmt.setInt("pos", (int) parsedFilterMap.get("posId"));
			cstmt.setInt("zon", (int) parsedFilterMap.get("zoneId"));
			cstmt.setInt("page_no", pageNumber);
			cstmt.setInt("page_size", pageSize);
			cstmt.setString("top_bottom", direction.toLowerCase());
			cstmt.setInt("perc", percentage);
			cstmt.setInt("metricid", metricId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> employeeDetailsMap = new HashMap<>();
				//TODO hard coding the company ID
				employeeDetailsMap.put("companyId", 1);
				employeeDetailsMap.put("employeeId", rs.getInt("emp_id"));
				employeeDetailsMap.put("metricScore", rs.getInt("metric_value"));
				employeeDetailsMap.put("firstName", rs.getString("first_name"));
				employeeDetailsMap.put("lastName", rs.getString("last_name"));
				employeeDetailsMap.put("function", rs.getString("Function"));
				employeeDetailsMap.put("position", rs.getString("Position"));
				employeeDetailsMap.put("zone", rs.getString("Zone"));
				result.add(employeeDetailsMap);
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(TheWallHelper.class).error("Exception while retrieving individual wall feed", e);
		}
		return result;
	}

	/**
	 * Get the list of teams for the wall
	 */
	public List<Map<String, Object>> getTeamWallFeed(int metricId, String direction, int percentage, int pageNumber, int pageSize,
			List<Filter> filterList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Map<String, Object>> result = new ArrayList<>();
		Map<String, Object> parsedFilterMap = new HashMap<>();
		if (filterList == null || filterList.isEmpty()) {
			parsedFilterMap.put("funcId", 0);
			parsedFilterMap.put("posId", 0);
			parsedFilterMap.put("zoneId", 0);
		} else {
			parsedFilterMap = UtilHelper.parseFilterList(filterList);
		}
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getWallFeedTeam(?,?,?,?,?,?,?,?)}");
			cstmt.setInt("fun", (int) parsedFilterMap.get("funcId"));
			cstmt.setInt("pos", (int) parsedFilterMap.get("posId"));
			cstmt.setInt("zon", (int) parsedFilterMap.get("zoneId"));
			cstmt.setInt("page_no", pageNumber);
			cstmt.setInt("page_size", pageSize);
			cstmt.setString("top_bottom", direction.toLowerCase());
			cstmt.setInt("perc", percentage);
			cstmt.setInt("metricid", metricId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> teamDetailsMap = new HashMap<>();
				teamDetailsMap.put("cubeId", rs.getInt("cube_id"));
				teamDetailsMap.put("metricScore", rs.getInt("metric_value"));
				teamDetailsMap.put("function", rs.getString("Function"));
				teamDetailsMap.put("position", rs.getString("Position"));
				teamDetailsMap.put("zone", rs.getString("Zone"));
				result.add(teamDetailsMap);
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(TheWallHelper.class).error("Exception while retrieving team wall feed", e);
		}
		return result;
	}

}
