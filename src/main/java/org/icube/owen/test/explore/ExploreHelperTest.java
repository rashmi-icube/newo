package org.icube.owen.test.explore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.explore.ExploreHelper;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.metrics.Metrics;
import org.junit.Test;

public class ExploreHelperTest {

	ExploreHelper eh = (ExploreHelper) ObjectFactory.getInstance("org.icube.owen.explore.ExploreHelper");

	@Test
	public void testGetIndividualMetricsData() {
		Employee empObj = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		Map<Employee, List<Metrics>> result = eh.getIndividualMetricsData(Arrays.asList(empObj.get(1), empObj.get(2)));
		individualDataTest(result);
	}

	@Test
	public void testGetIndividualTimeSeriesGraph() {
		Employee empObj = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		Map<Employee, List<Metrics>> result = eh.getIndividualTimeSeriesGraph(Arrays.asList(empObj.get(1), empObj.get(2)));
		individualDataTest(result);
	}

	private void individualDataTest(Map<Employee, List<Metrics>> result) {
		assertTrue(!result.isEmpty());
		for (Employee e : result.keySet()) {
			List<Metrics> metricsList = result.get(e);
			checkMetricsList(metricsList);
		}
	}

	private void checkMetricsList(List<Metrics> metricsList) {
		assertNotNull(metricsList);
		for (Metrics m : metricsList) {
			assertTrue(m.getId() > 0);
			assertTrue(!m.getName().isEmpty());
			assertTrue(m.getScore() >= 0);
			assertTrue(m.getDateOfCalculation() != null);
		}
	}

	@Test
	public void testGetTeamMetricsData() {
		Map<String, List<Filter>> teamListMap = new HashMap<>();
		FilterList fl = new FilterList();
		List<Filter> filterList = fl.getFilterValues();
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}
		teamListMap.put("team1", filterList);

		Filter filter = filterList.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(1, "HR");
		}
		teamListMap.put("team2", filterList);

		Map<String, List<Metrics>> teamMetricsData = eh.getTeamMetricsData(teamListMap); // no filter is selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}

		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamMetricsData(teamListMap); // 1 filter has selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}

		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamMetricsData(teamListMap); // 2 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}

		filterList.get(2).getFilterValues().clear();
		filterList.get(2).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamMetricsData(teamListMap); // 3 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}
	}

	@Test
	public void testGetTeamTimeSeriesGraph() {

		Map<String, List<Filter>> teamListMap = new HashMap<>();
		FilterList fl = new FilterList();
		List<Filter> filterList = fl.getFilterValues();
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}
		teamListMap.put("team1", filterList);

		Filter filter = filterList.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(1, "HR");
		}
		teamListMap.put("team2", filterList);

		Map<String, List<Metrics>> teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // no filter is selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}

		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // 1 filter has selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}
		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // 2 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}

		filterList.get(2).getFilterValues().clear();
		filterList.get(2).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // 3 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name));
		}

	}
}