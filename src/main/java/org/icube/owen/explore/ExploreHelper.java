package org.icube.owen.explore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.metrics.Metrics;

public class ExploreHelper extends TheBorg{

	public List<String> getVisualizationMasterList(){
		List<String> visualizationMasterList = new ArrayList<>();
		
		//how do we retrieve this list
		
		return visualizationMasterList;
	}
	
	public List<List<Metrics>> getTeamMetricsData(List<List<Filter>> teamList, List<Metrics> selectedMetrics, String visualizationType){
		List<List<Metrics>> result = new ArrayList<>();
		
		//figure out how to calculate
		
		return result;
	}
	
	public List<Map<String, Object>> getTimeSeriesGraph(List<List<Filter>> teamList, List<Metrics> metricsList){
		List<Map<String, Object>> timeSeriesGraphMapList = new ArrayList<>();
		
		//figure out how to calculate
		
		return timeSeriesGraphMapList;
	}
	
	
}
