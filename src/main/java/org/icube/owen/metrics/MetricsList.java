package org.icube.owen.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;
import org.icube.owen.initiative.Initiative;

public class MetricsList extends TheBorg {

	public List<Metrics> getInitiativeMetrics(Initiative i) {
		List<Metrics> metricsList = new ArrayList<>();
		Map<Integer, String> metricsTypeMap = i.getInitiativeTypeMap(i.getCategory());

		// TODO retrieve actual calculation from db @hpatel
		for (int id : metricsTypeMap.keySet()) {
			Metrics m = new Metrics();
			m.setAverage(Math.round(Math.random() * 100));
			m.setCategory(i.getCategory());
			m.setName(metricsTypeMap.get(id));
			m.setScore(Math.round(Math.random() * 100));
			metricsList.add(m);
		}

		return metricsList;

	}

}
