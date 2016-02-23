package org.icube.owen.test.dashboard;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.dashboard.Alert;
import org.icube.owen.dashboard.DashboardHelper;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.metrics.Metrics;
import org.junit.Test;

public class DashboardHelperTest {
	DashboardHelper dh = (DashboardHelper) ObjectFactory.getInstance("org.icube.owen.dashboard.DashboardHelper");
	FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
	List<Filter> filterMasterList = fl.getFilterValues();
	Filter functionFilter = fl.getFilterValues("Function");
	
	
	@Test
	public void testGetFilterMetrics(){
		List<Metrics> metricsList = dh.getFilterMetrics(functionFilter);
		for (Metrics m : metricsList){
			assertTrue(m.getId() > 0);
			assertTrue(!m.getDirection().isEmpty());
			assertTrue(!m.getCategory().isEmpty());
		}
	}
	
	@Test
	public void testGetOrganizationalMetrics(){
		List<Metrics> metricsList = dh.getOrganizationalMetrics();
		for (Metrics m : metricsList){
			assertTrue(m.getId() > 0);
			assertTrue(!m.getDirection().isEmpty());
		}
	}
	
	@Test
	public void testGetTimeSeriesGraph(){
		Map<Integer, String> filterValuesMap = new HashMap<>();
		filterValuesMap.put(11, "INTG4");
        Filter f = new Filter();
        f.setFilterName("Position");
        f.setFilterId(2);
        f.setFilterValues(filterValuesMap);
        Map<Integer, List<Map<Date, Integer>>> result = dh.getTimeSeriesGraph(f);
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
	public void testGetOrganizationTimeSeriesGraph(){
		Map<Integer, List<Map<Date, Integer>>> result = dh.getOrganizationTimeSeriesGraph();
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
	public void testGetAlertList(){
		List<Alert> alertList = dh.getAlertList();
		for (Alert a : alertList){
			assertTrue(a.getAlertId() > 0);
			
		}
	}

}
