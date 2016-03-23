package org.icube.owen.test.dashboard;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.dashboard.TheWallHelper;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.junit.Test;

public class TheWallTest {

	TheWallHelper twh = (TheWallHelper) ObjectFactory.getInstance("org.icube.owen.dashboard.TheWallHelper");

	@Test
	public void testGetIndividualWallFeed() {
		FilterList fl = new FilterList();
		List<Filter> filterList = fl.getFilterValues();
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}

		Filter filter = filterList.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(2, "Business");
		}
		List<Map<String, Object>> result = twh.getIndividualWallFeed(2, "top", 10, 1, 50, filterList);
		for (Map<String, Object> employeeDetailsMap : result) {
			assertNotNull(employeeDetailsMap.get("employeeId"));
			assertNotNull(employeeDetailsMap.get("metricScore"));
			assertNotNull(employeeDetailsMap.get("firstName"));
			assertNotNull(employeeDetailsMap.get("lastName"));
			assertNotNull(employeeDetailsMap.get("function"));
			assertNotNull(employeeDetailsMap.get("position"));
			assertNotNull(employeeDetailsMap.get("zone"));
		}
	}

	@Test
	public void testGetTeamWallFeed() {
		List<Filter> filterList = new ArrayList<>();

		Filter f = new Filter();
		Map<Integer, String> filterValuesMap = new HashMap<>();
		f.setFilterId(1);
		f.setFilterName("Function");
		filterValuesMap.put(0, "All");
		f.setFilterValues(filterValuesMap);
		filterList.add(f);

		Filter f1 = new Filter();
		Map<Integer, String> filterValuesMap1 = new HashMap<>();
		f1.setFilterId(2);
		f1.setFilterName("Position");
		filterValuesMap1.put(0, "All");
		f1.setFilterValues(filterValuesMap);
		filterList.add(f1);

		Filter f2 = new Filter();
		Map<Integer, String> filterValuesMap2 = new HashMap<>();
		f2.setFilterId(3);
		f2.setFilterName("Zone");
		filterValuesMap2.put(0, "All");
		f2.setFilterValues(filterValuesMap);
		filterList.add(f2);

		List<Map<String, Object>> result = twh.getTeamWallFeed(6, "top", 10, 1, 50, filterList);
		for (Map<String, Object> teamDetailsMap : result) {
			assertNotNull(teamDetailsMap.get("cubeId"));
			assertNotNull(teamDetailsMap.get("metricScore"));
			assertNotNull(teamDetailsMap.get("function"));
			assertNotNull(teamDetailsMap.get("position"));
			assertNotNull(teamDetailsMap.get("zone"));
		}
	}

}
