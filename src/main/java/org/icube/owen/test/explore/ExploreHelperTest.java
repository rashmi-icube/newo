package org.icube.owen.test.explore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.explore.Edge;
import org.icube.owen.explore.ExploreHelper;
import org.icube.owen.explore.MeResponseAnalysis;
import org.icube.owen.explore.Node;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.test.TestHelper;
import org.junit.Test;

public class ExploreHelperTest {

	ExploreHelper eh = (ExploreHelper) ObjectFactory.getInstance("org.icube.owen.explore.ExploreHelper");
	int companyId = 2;

	@Test
	public void testGetMeResponseAnalysisForOrg() {
		List<MeResponseAnalysis> mer = eh.getMeResponseAnalysisForOrg(companyId, 1);
		assertNotNull(mer);

	}

	@Test
	public void testGetMeResponseAnalysisForTeam() {
		Map<String, List<Filter>> teamListMap = new HashMap<>();

		// 1st team

		List<Filter> filterList1 = new ArrayList<>();
		Filter filter1 = new Filter();
		filter1.setFilterId(1);
		filter1.setFilterName("Function");
		Map<Integer, String> filterValuesMap1 = new HashMap<>();
		filterValuesMap1.put(2, "HR");
		filter1.setFilterValues(filterValuesMap1);
		filterList1.add(filter1);
		Filter filter2 = new Filter();
		Map<Integer, String> filterValuesMap2 = new HashMap<>();
		filter2.setFilterId(2);
		filter2.setFilterName("Position");
		filterValuesMap2.put(6, "Zone");
		filter2.setFilterValues(filterValuesMap2);
		filterList1.add(filter2);
		Filter filter3 = new Filter();
		Map<Integer, String> filterValuesMap3 = new HashMap<>();
		filter3.setFilterId(3);
		filter3.setFilterName("Zone");
		filterValuesMap3.put(12, "INTG5");
		filter3.setFilterValues(filterValuesMap3);
		filterList1.add(filter3);
		teamListMap.put("team1", filterList1);

		// 2nd team

		List<Filter> filterList2 = new ArrayList<>();
		Filter filter4 = new Filter();
		filter4.setFilterId(1);
		filter4.setFilterName("Function");
		Map<Integer, String> filterValuesMap4 = new HashMap<>();
		filterValuesMap4.put(2, "HR");
		filter4.setFilterValues(filterValuesMap4);
		filterList2.add(filter4);
		Filter filter5 = new Filter();
		Map<Integer, String> filterValuesMap5 = new HashMap<>();
		filter5.setFilterId(2);
		filter5.setFilterName("Position");
		filterValuesMap5.put(5, "State");
		filter5.setFilterValues(filterValuesMap5);
		filterList2.add(filter5);
		Filter filter6 = new Filter();
		Map<Integer, String> filterValuesMap6 = new HashMap<>();
		filter6.setFilterId(3);
		filter6.setFilterName("Zone");
		filterValuesMap6.put(12, "INTG5");
		filter6.setFilterValues(filterValuesMap6);
		filterList2.add(filter6);
		teamListMap.put("team2", filterList2);

		// 3rd team

		List<Filter> filterList3 = new ArrayList<>();
		Filter filter7 = new Filter();
		filter7.setFilterId(1);
		filter7.setFilterName("Function");
		Map<Integer, String> filterValuesMap7 = new HashMap<>();
		filterValuesMap7.put(2, "HR");
		filter7.setFilterValues(filterValuesMap1);
		filterList3.add(filter7);
		Filter filter8 = new Filter();
		Map<Integer, String> filterValuesMap8 = new HashMap<>();
		filter8.setFilterId(2);
		filter8.setFilterName("Position");
		filterValuesMap8.put(4, "Region");
		filter8.setFilterValues(filterValuesMap8);
		filterList3.add(filter8);
		Filter filter9 = new Filter();
		Map<Integer, String> filterValuesMap9 = new HashMap<>();
		filter9.setFilterId(3);
		filter9.setFilterName("Zone");
		filterValuesMap9.put(12, "INTG5");
		filter9.setFilterValues(filterValuesMap9);
		filterList3.add(filter9);
		teamListMap.put("team3", filterList3);

		List<MeResponseAnalysis> mer = eh.getMeResponseAnalysisForTeam(companyId, 1, teamListMap);
		assertNotNull(mer);
	}

	@Test
	public void testGetMeQuestionRelationshipTypeMap() {
		Map<Integer, String> relationshipTypeMap = eh.getMeQuestionRelationshipTypeMap(companyId);
		for (int i : relationshipTypeMap.keySet()) {
			assertNotNull(relationshipTypeMap.get(i));
			System.out.println(relationshipTypeMap.get(i));
		}
	}

	@Test
	public void testGetIndividualMetricsData() {
		Employee empObj = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		Map<Employee, List<Metrics>> result = eh.getIndividualMetricsData(companyId, Arrays
				.asList(empObj.get(companyId, 1), empObj.get(companyId, 2)));
		individualDataTest(result);
	}

	@Test
	public void testGetIndividualTimeSeriesGraph() {
		Employee empObj = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		Map<Employee, Map<Integer, List<Map<Date, Integer>>>> result = eh.getIndividualTimeSeriesGraph(companyId, Arrays.asList(empObj.get(companyId,
				1), empObj.get(companyId, 2)));
		for (Employee e : result.keySet()) {
			checkTimeSeriesData(result.get(e));
		}

	}

	private void individualDataTest(Map<Employee, List<Metrics>> result) {
		assertTrue(!result.isEmpty());
		for (Employee e : result.keySet()) {
			List<Metrics> metricsList = result.get(e);
			checkMetricsList(metricsList, false);
		}
	}

	public static void checkMetricsList(List<Metrics> metricsList, boolean checkDirection) {
		assertNotNull(metricsList);
		for (Metrics m : metricsList) {
			assertTrue(m.getId() > 0);
			assertTrue(!m.getName().isEmpty());
			assertTrue(m.getScore() >= -1);
			assertTrue(m.getDateOfCalculation() != null);
			if (checkDirection) {
				assertNotNull(m.getDirection());

			}
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
		List<Filter> filterList = fl.getFilterValues(companyId);
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

		Map<String, List<Metrics>> teamMetricsData = eh.getTeamMetricsData(companyId, teamListMap); // no filter is selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name), false);
		}

		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamMetricsData(companyId, teamListMap); // 1 filter has selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name), false);
		}

		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamMetricsData(companyId, teamListMap); // 2 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name), false);
		}

		filterList.get(2).getFilterValues().clear();
		filterList.get(2).getFilterValues().put(0, "All");
		teamMetricsData = eh.getTeamMetricsData(companyId, teamListMap); // 3 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkMetricsList(teamMetricsData.get(name), false);
		}
	}

	@Test
	public void testGetTeamTimeSeriesGraph() {

		Map<String, List<Filter>> teamListMap = new HashMap<>();

		List<Filter> filterList = TestHelper.getOneForEachFilter(companyId);

		teamListMap.put("team1", filterList);

		Filter filter = filterList.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(1, "HR");
		}
		teamListMap.put("team2", filterList);

		Map<String, Map<Integer, List<Map<Date, Integer>>>> teamMetricsData = eh.getTeamTimeSeriesGraph(companyId, teamListMap); // no filter is
																																	// selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

		filterList = TestHelper.getAllForOneFilter(companyId);
		teamMetricsData = eh.getTeamTimeSeriesGraph(companyId, teamListMap); // 1 filter has selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

		filterList = TestHelper.getAllForTwoFilters(companyId);
		teamMetricsData = eh.getTeamTimeSeriesGraph(companyId, teamListMap); // 2 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

		filterList = TestHelper.getAllForAllFilters(companyId);
		teamMetricsData = eh.getTeamTimeSeriesGraph(companyId, teamListMap); // 3 filters have selected as ALL
		for (String name : teamMetricsData.keySet()) {
			checkTimeSeriesData(teamMetricsData.get(name));
		}

	}

	@SuppressWarnings("unchecked")
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

		Map<String, List<?>> result = eh.getTeamNetworkDiagram(companyId, teamListMap, eh.getRelationshipTypeMap(companyId));

		List<Node> nodeList = (List<Node>) result.get("nodeList");
		for (Node n : nodeList) {
			assertNotNull(n.getEmployeeId());
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

	@SuppressWarnings("unchecked")
	@Test
	public void testGetIndividualNetworkDiagram() {
		List<Employee> employeeList = new ArrayList<>();
		for (int i = 3; i < 8; i++) {
			Employee e = new Employee();
			employeeList.add(e.get(companyId, i));
		}

		Map<String, List<?>> result = eh.getIndividualNetworkDiagram(companyId, employeeList, eh.getRelationshipTypeMap(companyId));
		List<Node> nodeList = (List<Node>) result.get("nodeList");
		for (Node n : nodeList) {
			assertNotNull(n.getEmployeeId());
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
