package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class MetricsList extends TheBorg {

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative
	 * Metrics are retrieved based on the combination of the category and filters selected
	 * @param initiativeCategory - team or individual 
	 * @param initiativeTypeId - ID of the type of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetrics(String initiativeCategory, int initiativeTypeId) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = new HashMap<>();

		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, initiativeCategory);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				metricsTypeMap.put(rs.getInt(1), rs.getString(2));
			}

			Map<Integer, String> primaryMetricMap = new HashMap<>();
			cstmt = dch.mysqlCon.prepareCall("{call GetMetricForInitiative(?)}");
			cstmt.setInt(1, initiativeTypeId);
			rs = cstmt.executeQuery();
			while (rs.next()) {
				primaryMetricMap.put(rs.getInt(1), rs.getString(2));
			}

			for (int id : metricsTypeMap.keySet()) {
				Metrics m = new Metrics();
				m.setCategory(initiativeCategory);
				m.setName(metricsTypeMap.get(id));
				// TODO passing dummy values for score... waiting for R connection
				m.setScore(Math.round(Math.random() * 100));
				if (primaryMetricMap.containsKey(id)) {
					m.setPrimary(true);
				} else {
					m.setPrimary(false);
				}
				metricsList.add(m);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category " + initiativeCategory + " and type ID " + initiativeTypeId);
		}

		return metricsList;

	}

}
