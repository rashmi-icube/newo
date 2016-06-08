package org.icube.owen.test.dashboard;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.dashboard.Alert;
import org.icube.owen.dashboard.HrDashboardHelper;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.metrics.Metrics;
import org.junit.Test;

public class HrDashboardHelperTest {
	HrDashboardHelper dh = (HrDashboardHelper) ObjectFactory.getInstance("org.icube.owen.dashboard.HrDashboardHelper");
	FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
	int companyId = 2;
	List<Filter> filterMasterList = fl.getFilterValues(companyId);
	Filter functionFilter = fl.getFilterValues(companyId, "Function");

	@Test
	public void testGetFilterMetrics() {
		Map<Integer, String> filterValuesMap = new HashMap<>();
		filterValuesMap.put(8, "INGT1");
		Filter f = new Filter();
		f.setFilterName("Zone");
		f.setFilterId(3);
		f.setFilterValues(filterValuesMap);
		List<Metrics> metricsList = dh.getFilterMetrics(companyId, f);
		for (Metrics m : metricsList) {
			assertNotNull(m.getId());
			assertNotNull(m.getDirection());
			assertNotNull(m.getCategory());
			assertNotNull(m.getName());
			assertNotNull(m.getDateOfCalculation());
			assertNotNull(m.getScore());
			assertNotNull(m.getAverage());
		}
	}

	@Test
	public void testGetOrganizationalMetrics() {
		List<Metrics> metricsList = dh.getOrganizationalMetrics(companyId);
		for (Metrics m : metricsList) {
			assertTrue(m.getId() > 0);
			assertTrue(!m.getDirection().isEmpty());
		}
	}

	@Test
	public void testGetTimeSeriesGraph() {
		Map<Integer, String> filterValuesMap = new HashMap<>();
		filterValuesMap.put(8, "INTG1");
		Filter f = new Filter();
		f.setFilterName("Zone");
		f.setFilterId(3);
		f.setFilterValues(filterValuesMap);
		Map<Integer, List<Map<Date, Integer>>> result = dh.getTimeSeriesGraph(companyId, f);
		for (int metricId : result.keySet()) {
			assertNotNull(result.get(metricId));
			List<Map<Date, Integer>> timeSeriesDataList = result.get(metricId);
			assertNotNull(timeSeriesDataList);
			for (int i = 0; i < timeSeriesDataList.size(); i++) {
				assertNotNull(timeSeriesDataList.get(i));
			}
		}

	}

	@Test
	public void testGetOrganizationTimeSeriesGraph() {
		Map<Integer, List<Map<Date, Integer>>> result = dh.getOrganizationTimeSeriesGraph(companyId);
		for (int metricId : result.keySet()) {
			assertNotNull(result.get(metricId));
			List<Map<Date, Integer>> timeSeriesDataList = result.get(metricId);
			assertNotNull(timeSeriesDataList);
			for (int i = 0; i < timeSeriesDataList.size(); i++) {
				assertNotNull(timeSeriesDataList.get(i));
			}
		}
	}

	@Test
	public void testGetAlertList() {
		List<Alert> alertList = dh.getAlertList(companyId);
		for (Alert a : alertList) {
			assertTrue(a.getAlertId() > 0);

		}
	}

}
