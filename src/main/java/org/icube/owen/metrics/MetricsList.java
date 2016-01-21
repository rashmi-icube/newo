package org.icube.owen.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.initiative.Initiative;

public class MetricsList {

	public List<Metrics> getInitiativeMetrics(Initiative i){
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = i.getInitiativeTypeMap(i.getCategory());
		
		
		
		return metricsList;
		
	}
	
}
