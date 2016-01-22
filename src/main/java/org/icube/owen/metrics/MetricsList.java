package org.icube.owen.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.initiative.Initiative;

public class MetricsList extends TheBorg {

	/**
	 * Retrieves the metrics for the filters chosen while creating the initiative
	 * Metrics are retrieved based on the combination of the category and filters selected
	 * @param initiativeCategory - team or individual 
	 * @param filterList - list of filters selected on the UI
	 * @return list of metrics objects
	 */
	public List<Metrics> getInitiativeMetrics(String initiativeCategory, List<Filter> filterList) {
		List<Metrics> metricsList = new ArrayList<>();
		Initiative initObj = new Initiative();
		Map<Integer, String> metricsTypeMap = initObj.getInitiativeTypeMap(initiativeCategory);

		// TODO retrieve actual calculation from db @hpatel
		for (int id : metricsTypeMap.keySet()) {
			Metrics m = new Metrics();
			m.setAverage(Math.round(Math.random() * 100));
			m.setCategory(initiativeCategory);
			m.setName(metricsTypeMap.get(id));
			m.setScore(Math.round(Math.random() * 100));
			metricsList.add(m);
		}

		return metricsList;

	}

}
