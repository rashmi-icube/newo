package org.icube.owen.test.metrics;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.metrics.MetricsHelper;
import org.junit.Test;

public class MetricsHelperTest {
	MetricsHelper mh = (MetricsHelper) ObjectFactory.getInstance("org.icube.owen.metrics.MetricsHelper");
	int companyId = 2;

	@Test
	public void testGetMetricListForCategory() {
		Map<Integer, String> metricsMap = mh.getMetricListForCategory(companyId, "Team");
		assertNotNull(metricsMap);
	}
}
