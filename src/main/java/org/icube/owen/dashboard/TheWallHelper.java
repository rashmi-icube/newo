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
	 * @param companyId - Company ID
	 * @param metricId - ID of the metric
	 * @param direction - top/bottom
	 * @param percentage - percent of employees to be displayed 
	 * @param pageNumber - page number of the page to be displayed
	 * @param pageSize - size of the page
	 * @param filterList - List of the filter objects
	 * @return List of map of Employee details
	 */
	public List<Map<String, Object>> getIndividualWallFeed(int companyId, int metricId, String direction, int percentage, int pageNumber,
			int pageSize, List<Filter> filterList) {
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Entering getIndividualWallFeed");
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Map<String, Object>> result = new ArrayList<>();
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).info("HashMap created!!!");
		Map<String, Object> parsedFilterMap = new HashMap<>();
		if (filterList == null || filterList.isEmpty()) {
			parsedFilterMap.put("funcId", 0);
			parsedFilterMap.put("posId", 0);
			parsedFilterMap.put("zoneId", 0);
		} else {
			parsedFilterMap = UtilHelper.parseFilterList(filterList);
		}
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Function : " + parsedFilterMap.get("funcId"));
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Position : " + parsedFilterMap.get("posId"));
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Zone : " + parsedFilterMap.get("zoneId"));
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
				"{call getWallFeedIndividual(?,?,?,?,?,?,?,?)}")) {

			cstmt.setInt("fun", (int) parsedFilterMap.get("funcId"));
			cstmt.setInt("pos", (int) parsedFilterMap.get("posId"));
			cstmt.setInt("zon", (int) parsedFilterMap.get("zoneId"));
			cstmt.setInt("page_no", pageNumber);
			cstmt.setInt("page_size", pageSize);
			cstmt.setString("top_bottom", direction.toLowerCase());
			cstmt.setInt("perc", percentage);
			cstmt.setInt("metricid", metricId);
			try (ResultSet rs = cstmt.executeQuery();) {
				while (rs.next()) {
					org.apache.log4j.Logger.getLogger(TheWallHelper.class).info("HashMap created!!!");
					Map<String, Object> employeeDetailsMap = new HashMap<>();
					employeeDetailsMap.put("companyId", companyId);
					employeeDetailsMap.put("employeeId", rs.getInt("emp_id"));
					employeeDetailsMap.put("metricScore", rs.getInt("metric_value"));
					employeeDetailsMap.put("firstName", rs.getString("first_name"));
					employeeDetailsMap.put("lastName", rs.getString("last_name"));
					employeeDetailsMap.put("metricId", rs.getInt("metric_id"));
					employeeDetailsMap.put("initiativeTypeId", rs.getInt("init_type_id"));
					employeeDetailsMap.put("function", rs.getString("Function"));
					employeeDetailsMap.put("position", rs.getString("Position"));
					employeeDetailsMap.put("zone", rs.getString("Zone"));
					org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug(
							"Employee Details : companyId : " + employeeDetailsMap.get("companyId") + "; employeeId :  "
									+ employeeDetailsMap.get("employeeId") + "; metricScore :  " + employeeDetailsMap.get("metricScore")
									+ "; firstName :  " + employeeDetailsMap.get("firstName") + "; lastName :  " + employeeDetailsMap.get("lastName")
									+ "; metricId :  " + employeeDetailsMap.get("metricId") + "; initiativeTypeId :  "
									+ employeeDetailsMap.get("initiativeTypeId") + "; function :  " + employeeDetailsMap.get("function")
									+ "; position :  " + employeeDetailsMap.get("position") + "; zone :  " + employeeDetailsMap.get("zone"));

					result.add(employeeDetailsMap);
				}
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(TheWallHelper.class).error("Exception while retrieving individual wall feed", e);
		}
		return result;
	}

	/**
	 * Get the list of teams for the wall
	 * @param companyId - Company ID
	 * @param metricId - ID of the metric
	 * @param direction - top/bottom
	 * @param percentage - percent of team to be displayed 
	 * @param pageNumber - page number of the page 
	 * @param pageSize - size of the page to be displayed
	 * @param filterList - List of filter objects
	 * @return List of map of Employee details
	 */
	public List<Map<String, Object>> getTeamWallFeed(int companyId, int metricId, String direction, int percentage, int pageNumber, int pageSize,
			List<Filter> filterList) {
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Entering getTeamWallFeed");
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Map<String, Object>> result = new ArrayList<>();
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).info("HashMap created!!!");
		Map<String, Object> parsedFilterMap = new HashMap<>();
		if (filterList == null || filterList.isEmpty()) {
			parsedFilterMap.put("funcId", 0);
			parsedFilterMap.put("posId", 0);
			parsedFilterMap.put("zoneId", 0);
		} else {
			parsedFilterMap = UtilHelper.parseFilterList(filterList);
		}
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Function : " + parsedFilterMap.get("funcId"));
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Position : " + parsedFilterMap.get("posId"));
		org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug("Zone : " + parsedFilterMap.get("zoneId"));
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
				"{call getWallFeedTeam(?,?,?,?,?,?,?,?)}")) {
			cstmt.setInt("fun", (int) parsedFilterMap.get("funcId"));
			cstmt.setInt("pos", (int) parsedFilterMap.get("posId"));
			cstmt.setInt("zon", (int) parsedFilterMap.get("zoneId"));
			cstmt.setInt("page_no", pageNumber);
			cstmt.setInt("page_size", pageSize);
			cstmt.setString("top_bottom", direction.toLowerCase());
			cstmt.setInt("perc", percentage);
			cstmt.setInt("metricid", metricId);
			try (ResultSet rs = cstmt.executeQuery()) {
				while (rs.next()) {
					org.apache.log4j.Logger.getLogger(TheWallHelper.class).info("HashMap created!!!");
					Map<String, Object> teamDetailsMap = new HashMap<>();
					teamDetailsMap.put("cubeId", rs.getInt("cube_id"));
					teamDetailsMap.put("metricScore", rs.getInt("metric_value"));
					List<Filter> resultFilterList = new ArrayList<>();
					for (int i = 1; i <= 3; i++) {
						Filter f = new Filter();
						f.setFilterId(rs.getInt("dimension_id_" + i));
						f.setFilterName(rs.getString("dimension_name_" + i));
						org.apache.log4j.Logger.getLogger(TheWallHelper.class).info("HashMap created!!!");
						Map<Integer, String> filterValueMap = new HashMap<>();
						filterValueMap.put(rs.getInt("dimension_val_id_" + i), rs.getString("dimension_val_name_" + i));
						f.setFilterValues(filterValueMap);
						resultFilterList.add(f);
					}
					teamDetailsMap.put("filterList", resultFilterList);
					teamDetailsMap.put("metricId", rs.getInt("metric_id"));
					teamDetailsMap.put("initiativeTypeId", rs.getInt("init_type_id"));
					org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug(
							"Team Details : cubeId : " + teamDetailsMap.get("cubeId") + "; metricScore :  " + teamDetailsMap.get("metricScore")
									+ "; metricId :  " + teamDetailsMap.get("metricId") + "; initiativeTypeId :  "
									+ teamDetailsMap.get("initiativeTypeId"));
					for (Filter f : resultFilterList) {
						org.apache.log4j.Logger.getLogger(TheWallHelper.class).debug(
								"Result filter : filterId : " + f.getFilterId() + " filterName : " + f.getFilterName() + " filterValues : "
										+ f.getFilterValues().toString());
					}

					result.add(teamDetailsMap);
				}
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(TheWallHelper.class).error("Exception while retrieving team wall feed", e);
		}
		return result;
	}

}
