package org.icube.owen.explore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.metrics.Metrics;

public class ExploreHelper extends TheBorg{

	public List<String> getVisualizationMasterList(){
		List<String> visualizationMasterList = new ArrayList<>();
		
		//how do we retrieve this list
		// hardcoded list
		return visualizationMasterList;
	}
	
	public List<List<Metrics>> getTeamMetricsData(List<List<Filter>> teamList){
		List<List<Metrics>> result = new ArrayList<>();
		// use MetricsList.getInitiativeMetricsForTeam for every team
		
		
		return result;
	}
	
	
	//TODO hpatel, ravi : figure out what to do when ALL is selected for dimensions 
	public List<Map<String, Object>> getTeamTimeSeriesGraph(List<List<Filter>> teamList){
		List<Map<String, Object>> timeSeriesGraphMapList = new ArrayList<>();
		// sql procedure to be called for every team individually - I/P filter IDs 
		// if only one of the filter is ALL do not call the team series function cuz there won't be any O/P
		
		return timeSeriesGraphMapList;
	}
	
	public Map<Employee, List<Metrics>> getIndividualMetricsData(List<Employee> employeeList){
		Map<Employee, List<Metrics>> result = new HashMap<>();
		
		// sql call to get individual metrics , I/P list of employeeId 
		// o/p employeeId metric data
		
		
		return result;
	}
	
	public List<Map<String, Object>> getIndividualTimeSeriesGraph(List<Employee> employeeList){
		List<Map<String, Object>> timeSeriesGraphMapList = new ArrayList<>();
		// sql procedure to be called for every team individually - I/P employeeId list 
		// i/p o/p same as individual metrics data
		
		return timeSeriesGraphMapList;
	}
}
