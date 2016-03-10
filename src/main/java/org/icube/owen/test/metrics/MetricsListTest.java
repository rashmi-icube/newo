package org.icube.owen.test.metrics;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.metrics.MetricsList;
import org.icube.owen.test.explore.ExploreHelperTest;
import org.junit.Test;

public class MetricsListTest {
	MetricsList ml = (MetricsList) ObjectFactory.getInstance("org.icube.owen.metrics.MetricsList");

	@Test
	public void testGetInitiativeMetricsForTeam() {
		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
		/*List<Filter> filterMasterList = fl.getFilterValues();
		List<Metrics> metricsList = ml.getInitiativeMetricsForTeam(1, filterMasterList);
		for (Metrics m : metricsList) {
			assertTrue(!m.getName().isEmpty());
			assertTrue(!m.getDirection().isEmpty());
		}*/

		Map<String, List<Filter>> teamListMap = new HashMap<>();

		List<Filter> filterList = fl.getFilterValues();
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}

		List<Metrics> metricsList = ml.getInitiativeMetricsForTeam(6, filterList);
		ExploreHelperTest.checkMetricsList(metricsList);

		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		metricsList = ml.getInitiativeMetricsForTeam(6, filterList); // 1 filter has selected as ALL
		ExploreHelperTest.checkMetricsList(metricsList);

		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		metricsList = ml.getInitiativeMetricsForTeam(6, filterList);// 2 filters have selected as ALL
		ExploreHelperTest.checkMetricsList(metricsList);

		filterList.get(2).getFilterValues().clear();
		filterList.get(2).getFilterValues().put(0, "All");
		metricsList = ml.getInitiativeMetricsForTeam(6, filterList);// 3 filters have selected as ALL
		ExploreHelperTest.checkMetricsList(metricsList);

	}

	@Test
	public void testGetInitiativeMetricsForIndividual() {
		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		partOfEmployeeList.add(e.get(16));
		List<Metrics> metricsList = ml.getInitiativeMetricsForIndividual(1, partOfEmployeeList);
		for (Metrics m : metricsList) {
			assertTrue(!m.getName().isEmpty());
			assertTrue(!m.getDirection().isEmpty());
		}
	}

}
