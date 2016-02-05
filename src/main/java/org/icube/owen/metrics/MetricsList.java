package org.icube.owen.metrics;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.RList;

public class MetricsList extends TheBorg {

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative
	 * Metrics are retrieved based on the combination of the category and filters selected
	 * @param initiativeCategory - team or individual 
	 * @param initiativeTypeId - ID of the type of initiative
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetricsForTeam(List<Filter> filterList, int initiativeTypeId) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = new HashMap<>();

		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, "Team");
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
				m.setCategory("Team");
				m.setName(metricsTypeMap.get(id));

				dch.rCon.eval("source(\"/Users/apple/Documents/workspace/owen/scripts/metric.r\")");
				List<Integer> funcList = new ArrayList<>();
				List<Integer> posList = new ArrayList<>();
				List<Integer> zoneList = new ArrayList<>();
				for (Filter f : filterList) {
					if (f.getFilterName().equalsIgnoreCase("Function")) {
						funcList.addAll(f.getFilterValues().keySet());
					} else if (f.getFilterName().equalsIgnoreCase("Position")) {
						posList.addAll(f.getFilterValues().keySet());
					} else if (f.getFilterName().equalsIgnoreCase("Zone")) {
						zoneList.addAll(f.getFilterValues().keySet());
					}
				}
				dch.rCon.assign("funcList", funcList.toString());
				dch.rCon.assign("posList", posList.toString());
				dch.rCon.assign("zoneList", zoneList.toString());
				REXP teamMetricScore = dch.rCon.parseAndEval("try(eval(TeamMetric(funcList, posList, zoneList)))");
				if (teamMetricScore.inherits("try-error")) {
					org.apache.log4j.Logger.getLogger(MetricsList.class).error("Error: " + teamMetricScore.asDouble());
				} else {
					org.apache.log4j.Logger.getLogger(MetricsList.class).debug(teamMetricScore.asDouble());
				}
				m.setScore(teamMetricScore.asDouble());

				if (primaryMetricMap.containsKey(id)) {
					m.setPrimary(true);
				} else {
					m.setPrimary(false);
				}
				metricsList.add(m);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category team and type ID " + initiativeTypeId);
		}

		return metricsList;

	}

	public List<Metrics> getInitiativeMetricsForIndividual(List<Employee> partOfEmployeeList, int initiativeTypeId) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = new HashMap<>();

		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getMetricListForCategory(?)}");
			cstmt.setString(1, "Individual");
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

			dch.rCon.eval("source(\"/Users/apple/Documents/workspace/owen/scripts/metric.r\")");
			List<Integer> empIdList = new ArrayList<>();
			for (Employee e : partOfEmployeeList) {
				empIdList.add(e.getEmployeeId());
			}
			dch.rCon.assign("empIdList", empIdList.toString());
			REXP individualMetricScore = dch.rCon.parseAndEval("try(eval(IndividualMetric(empIdList)))");
			if (individualMetricScore.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(MetricsList.class).error("Error: " + individualMetricScore.asDouble());
			} else {
				org.apache.log4j.Logger.getLogger(MetricsList.class).debug(individualMetricScore.asList());
			}
			//TODO Rashmi read the result from R
			RList result = individualMetricScore.asList();
			Map<Integer, Double> metricScoreMap = new HashMap<>();
			for(String s : result.keys()){
				List<Integer> metricIdList = (List<Integer>)result.get(s);
				
			}
			
			
			for(int i = 0; i<5; i++){
				metricScoreMap.put((Integer)result.get(i), (Double)result.get(i));
			}
			List<Integer> metricIdList = (List<Integer>) result.get(0);
			List<Double> scoreList = (List<Double>) result.get(1);
			
			
			
			for (int id : metricsTypeMap.keySet()) {
				Metrics m = new Metrics();
				m.setCategory("Individual");
				m.setName(metricsTypeMap.get(id));

				
				
				m.setScore((Double)result.get(id));

				if (primaryMetricMap.containsKey(id)) {
					m.setPrimary(true);
				} else {
					m.setPrimary(false);
				}
				metricsList.add(m);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(MetricsList.class).error(
					"Exception while trying to retrieve metrics for category individual and type ID " + initiativeTypeId);
		}

		return metricsList;

	}

}
