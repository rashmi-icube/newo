package org.icube.owen.dashboard;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.employee.Employee;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.metrics.Metrics;

public class IndividualDashboardHelper {

	public List<Metrics> getIndividualMetrics(int employeeId) {
		List<Metrics> metricsList = new ArrayList<>();
		// getIndividualMetricValue - filter out for only 3 metrics {remove retention / sentiment - 3/5}

		return metricsList;
	}

	public Map<Integer, List<Map<Date, Integer>>> getIndividualMetricsTimeSeries(int employeeId) {
		Map<Integer, List<Map<Date, Integer>>> result = new HashMap<>();
		// getIndividualMetricTimeSeries  - filter out for only 3 metrics {remove retention / sentiment - 3/5}
		return result;
	}
	
	public List<Initiative> individualInitiativeList(int employeeId){
		List<Initiative> initiativeList = new ArrayList<>();
		
		return initiativeList;
	}
	
	public List<ActivityFeed> getActivityFeedList(int employeeId){
	List<ActivityFeed> result = new ArrayList<>();
	
	return result;
	}
	
	public Map<Integer, Integer> getMetricRelationshipTypeMapping(){
		Map<Integer, Integer> result = new HashMap<>();
		
		return result;
	}
	
	public List<Employee> getSmartList(int employeeId, int metricId){
		//get relationship type id from metric id
		List<Employee> employeeList = new ArrayList();
		// Question getSmartListForQuestion
		
		return employeeList;
	}
	
	public boolean saveAppreciation(List<Employee> employeeList, int responseValue){
		return false;
	}
	
	/**
	 * Password Guidelines : Minimum 8 characters and should contain atleast 1 numeric digit
	 * @param currentPassword
	 * @param newPassword
	 * @return
	 */
	public boolean changePassword(String currentPassword, String newPassword){
		//UI will make the check to see if both times the current password match
		
		return false;
	}
}
