package org.icube.owen.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.filter.Filter;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.metrics.Metrics;

public class DashboardHelper {

	//Make a function to retrieve the score/average from R script
	public List<Metrics> getOrganizationalMetrics(List<Filter> filterList){
		List<Metrics> orgMetricsList = new ArrayList<>();
		// call R script to get organizational metrics...
		
		return orgMetricsList;
	}
	
	// Time series function - I/P : 2 metric names , initiative category, filter selection
	public List<Map<String, Object>> getTimeSeriesGraph(String metricName1, String metricName2, List<Filter> filterList){
		List<Map<String, Object>> timeSeriesGraphMap = new ArrayList<>();
		
		//call SQL procedure for getting all the graph details
		
		//fill map with values : date, score, metric name
		
		
		return timeSeriesGraphMap;
	}
	
	// Function to switch between ALL/Filters -- What will exactly change?
	
	
	
	
	
	// Focus Area - Top 3 active initiatives w/descending order of end date
	public List<Initiative> getInitiativesUnderFocus(){
		List<Initiative> initiativeList = new ArrayList<>();
		// write neo4j query to get these 3 initiatives
		return initiativeList;
	}
	
	
}
