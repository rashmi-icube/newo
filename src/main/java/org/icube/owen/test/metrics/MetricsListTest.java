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
import org.junit.Test;

public class MetricsListTest {
	MetricsList ml = (MetricsList) ObjectFactory.getInstance("org.icube.owen.metrics.MetricsList");

	@Test
	public void testGetInitiativeMetricsForTeam() {
		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
		List<Filter> filterMasterList = fl.getFilterValues();
		List<Metrics> metricsList = ml.getInitiativeMetricsForTeam(1, filterMasterList);
		for (Metrics m : metricsList) {
			assertTrue(!m.getName().isEmpty());
			assertTrue(!m.getDirection().isEmpty());
		}

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
