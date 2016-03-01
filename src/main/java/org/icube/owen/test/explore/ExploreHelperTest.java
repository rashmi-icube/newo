package org.icube.owen.test.explore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.explore.Edge;
import org.icube.owen.explore.ExploreHelper;
import org.icube.owen.explore.Node;
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
		Map<Employee, Map<Integer, List<Map<Date, Integer>>>> result = eh.getIndividualTimeSeriesGraph(Arrays.asList(empObj.get(1), empObj.get(2)));
		for (Employee e : result.keySet()) {
			checkTimeSeriesData(result.get(e));
		}

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

	private void checkTimeSeriesData(Map<Integer, List<Map<Date, Integer>>> timeSeriesMap) {
		for (int metricId : timeSeriesMap.keySet()) {
			assertNotNull(timeSeriesMap.get(metricId));
			List<Map<Date, Integer>> timeSeriesDataList = timeSeriesMap.get(metricId);
			assertNotNull(timeSeriesDataList);
			for (int i = 0; i < timeSeriesDataList.size(); i++) {
				assertNotNull(timeSeriesDataList.get(i));
			}
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

		Map<String, Map<Integer, List<Map<Date, Integer>>>> teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // no filter is selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // 1 filter has selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}
		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // 2 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

		filterList.get(2).getFilterValues().clear();
		filterList.get(2).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamTimeSeriesGraph(teamListMap); // 3 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

	}

	@Test
	public void testGetTeamNetworkDiagram() {

		Map<String, List<Filter>> teamListMap = new HashMap<>();
		List<Filter> filterList = new ArrayList<>();

		Map<Integer, String> filterValuesMap = new HashMap<>();
		Filter f = new Filter();
		f.setFilterId(1);
		f.setFilterName("Function");
		filterValuesMap.put(0, "All");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		f = new Filter();
		f.setFilterId(2);
		f.setFilterName("Position");
		filterValuesMap = new HashMap<>();
		filterValuesMap.put(4, "Region");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		f = new Filter();
		f.setFilterId(3);
		f.setFilterName("Zone");
		filterValuesMap = new HashMap<>();
		filterValuesMap.put(8, "INTG1");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		teamListMap.put("team1", filterList);

		filterList = new ArrayList<>();
		f = new Filter();
		f.setFilterId(1);
		f.setFilterName("Function");
		filterValuesMap = new HashMap<>();
		filterValuesMap.put(0, "All");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		f = new Filter();
		f.setFilterId(2);
		f.setFilterName("Position");
		filterValuesMap = new HashMap<>();
		filterValuesMap.put(4, "Region");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		f = new Filter();
		f.setFilterId(3);
		f.setFilterName("Zone");
		filterValuesMap = new HashMap<>();
		filterValuesMap.put(9, "INTG2");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		teamListMap.put("team2", filterList);

		Map<String, List<?>> result = eh.getTeamNetworkDiagram(teamListMap, eh.getRelationshipTypeMap());

		List<Node> nodeList = (List<Node>) result.get("nodeList");
		for (Node n : nodeList) {
			assertNotNull(n.getEmployee_id());
			assertNotNull(n.getFirstName());
			assertNotNull(n.getLastName());
			assertNotNull(n.getFunction());
			assertNotNull(n.getPosition());
			assertNotNull(n.getZone());
			assertNotNull(n.getTeamName());
		}

		List<Edge> edgeList = (List<Edge>) result.get("edgeList");
		for (Edge e : edgeList) {
			assertNotNull(e.getFromEmployeId());
			assertNotNull(e.getToEmployeeId());
			assertNotNull(e.getRelationshipType());
			assertNotNull(e.getWeight());
		}

	}

	@Test
	public void testGetIndividualNetworkDiagram() {
		List<Employee> employeeList = new ArrayList<>();
		for (int i = 1; i < 6; i++) {
			Employee e = new Employee();
			employeeList.add(e.get(i));
		}

		Map<String, List<?>> result = eh.getIndividualNetworkDiagram(employeeList, eh.getRelationshipTypeMap());
		List<Node> nodeList = (List<Node>) result.get("nodeList");
		for (Node n : nodeList) {
			assertNotNull(n.getEmployee_id());
			assertNotNull(n.getFirstName());
			assertNotNull(n.getLastName());
			assertNotNull(n.getFunction());
			assertNotNull(n.getPosition());
			assertNotNull(n.getZone());
			assertNotNull(n.getConnectedness());
		}

		List<Edge> edgeList = (List<Edge>) result.get("edgeList");
		for (Edge e : edgeList) {
			assertNotNull(e.getFromEmployeId());
			assertNotNull(e.getToEmployeeId());
			assertNotNull(e.getRelationshipType());
			assertNotNull(e.getWeight());
		}

	}
}
