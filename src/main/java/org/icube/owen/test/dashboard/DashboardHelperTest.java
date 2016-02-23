package org.icube.owen.test.dashboard;

import static org.junit.Assert.assertTrue;

import java.util.List;

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
		List<Metrics> metricsList = dh.getTimeSeriesGraph(6, 7, functionFilter);
		for (Metrics m : metricsList){
			assertTrue(m.getId() > 0);
		}
	}
	
	@Test
	public void testGetOrganizationTimeSeriesGraph(){
		List<Metrics> metricsList = dh.getOrganizationTimeSeriesGraph();
		for (Metrics m : metricsList){
			assertTrue(m.getId() > 0);
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
