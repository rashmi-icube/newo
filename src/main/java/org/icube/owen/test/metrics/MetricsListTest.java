package org.icube.owen.test.metrics;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
	int companyId = 2;

	@Test
	public void testGetInitiativeMetricsForTeam() {
		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");

		List<Filter> filterList = fl.getFilterValues(companyId);
		for (Filter f : filterList) {
			f.getFilterValues().remove(0);
		}
		List<Metrics> metricsList = ml.getInitiativeMetricsForTeam(companyId, 6, filterList);
		ExploreHelperTest.checkMetricsList(metricsList, true);
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}

		metricsList = ml.getInitiativeMetricsForTeam(companyId, 6, filterList);
		ExploreHelperTest.checkMetricsList(metricsList, true);

		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		metricsList = ml.getInitiativeMetricsForTeam(companyId, 6, filterList); // 1 filter has selected as ALL
		ExploreHelperTest.checkMetricsList(metricsList, true);

		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		metricsList = ml.getInitiativeMetricsForTeam(companyId, 6, filterList);// 2 filters have selected as ALL
		ExploreHelperTest.checkMetricsList(metricsList, true);

		filterList.get(2).getFilterValues().clear();
		filterList.get(2).getFilterValues().put(0, "All");
		metricsList = ml.getInitiativeMetricsForTeam(companyId, 6, filterList);// 3 filters have selected as ALL
		ExploreHelperTest.checkMetricsList(metricsList, true);

	}

	@Test
	public void testGetInitiativeMetricsForIndividual() {
		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		partOfEmployeeList.add(e.get(companyId, 1));
		List<Metrics> metricsList = ml.getInitiativeMetricsForIndividual(companyId, 1, partOfEmployeeList);
		for (Metrics m : metricsList) {
			assertTrue(!m.getName().isEmpty());
			assertTrue(!m.getDirection().isEmpty());
		}
	}

}
