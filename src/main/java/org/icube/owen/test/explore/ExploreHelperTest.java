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
import org.icube.owen.explore.MeResponse;
import org.icube.owen.explore.MeResponseAnalysis;
import org.icube.owen.explore.Node;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.metrics.Metrics;
import org.icube.owen.survey.Question;
import org.icube.owen.test.TestHelper;
import org.junit.Test;

public class ExploreHelperTest {

	ExploreHelper eh = (ExploreHelper) ObjectFactory.getInstance("org.icube.owen.explore.ExploreHelper");
	int companyId = 2;
	
	@Test
	public void testGetMeResponseAnalysisForOrg(){
		List<MeResponseAnalysis> mer = eh.getMeResponseAnalysisForOrg(companyId, 1);
		assertNotNull(mer);
		
	}

	@Test
	public void testGetCompletedMeQuestionList() {
		Map<Integer, Map<Question, MeResponse>> result = new HashMap<>();
		result = eh.getCompletedMeQuestionList(companyId, 2);
		for (int i : result.keySet()) {
			for (Question q : result.get(i).keySet()) {
				System.out.println(q.getQuestionId());
				System.out.println(q.getQuestionText());
			}
		}

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
	public void testGetMeResponseDetailsForTeam() {
		Map<String, List<Filter>> teamListMap = new HashMap<>();

		List<Filter> filterList = TestHelper.getOneForEachFilter(companyId);

		teamListMap.put("team1", filterList);

		List<Filter> filterList2 = TestHelper.getOneForEachFilter(companyId);

		Filter filter = filterList2.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(1, "HR");
			filterList2.add(filter);
		}
		teamListMap.put("team2", filterList2);
		List<Filter> filterList3 = TestHelper.getAllForTwoFilters(companyId);
		Filter filter1 = filterList3.get(0);
		if (filter1.getFilterName().equalsIgnoreCase("Position")) {
			filter1.getFilterValues().clear();
			filter1.getFilterValues().put(5, "State");
			filterList3.add(filter);
		}
		teamListMap.put("team3", filterList3);
		Map<Integer, Map<String, MeResponse>> result = eh.getMeResponseDetailsForTeam(companyId, 6, teamListMap);
		for (Map<String, MeResponse> meResMap : result.values()) {
			for (String team : meResMap.keySet()) {
				System.out.println("Team----" + team);
				System.out.println("strongly disagree---" + meResMap.get(team).getStronglyDisagree());
				System.out.println("disagree----" + meResMap.get(team).getDisagree());
				System.out.println("neutral----" + meResMap.get(team).getNeutral());
				System.out.println("agree----" + meResMap.get(team).getAgree());
				System.out.println("strongly agree---" + meResMap.get(team).getStronglyAgree());
			}

		}

	}
	
	@Test
	public void testGetCompletedMeQuestionListForTeam(){
		Map<String, List<Filter>> teamListMap = new HashMap<>();

		List<Filter> filterList = TestHelper.getOneForEachFilter(companyId);

		teamListMap.put("team1", filterList);

		List<Filter> filterList2 = TestHelper.getOneForEachFilter(companyId);

		Filter filter = filterList2.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(1, "HR");
			filterList2.add(filter);
		}
		teamListMap.put("team2", filterList2);
		//List<Filter> filterList3 = TestHelper.getAllForTwoFilters(companyId);
		List<Filter> filterList3 = TestHelper.getOneForEachFilter(companyId);
		Filter filter1 = filterList3.get(1);
		if (filter1.getFilterName().equalsIgnoreCase("Position")) {
			filter1.getFilterValues().clear();
			filter1.getFilterValues().put(5, "State");
			filterList3.add(filter1);
		}
		teamListMap.put("team3", filterList3);
		Map<Integer, Map<Question, MeResponse>> result = eh.getCompletedMeQuestionListForTeam(companyId, 1, teamListMap);
		for (Map<Question, MeResponse> meResMap : result.values()) {
			for (Question q : meResMap.keySet()) {
				System.out.println("question----" + q.getQuestionId());
				System.out.println("strongly disagree---" + meResMap.get(q).getStronglyDisagree());
				System.out.println("disagree----" + meResMap.get(q).getDisagree());
				System.out.println("neutral----" + meResMap.get(q).getNeutral());
				System.out.println("agree----" + meResMap.get(q).getAgree());
				System.out.println("strongly agree---" + meResMap.get(q).getStronglyAgree());
			}

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
