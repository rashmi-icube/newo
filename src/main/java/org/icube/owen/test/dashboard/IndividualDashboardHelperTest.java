package org.icube.owen.test.dashboard;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.dashboard.IndividualDashboardHelper;
import org.icube.owen.employee.Employee;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.metrics.Metrics;
import org.junit.Test;

public class IndividualDashboardHelperTest {
	IndividualDashboardHelper idh = (IndividualDashboardHelper) ObjectFactory.getInstance("org.icube.owen.dashboard.IndividualDashboardHelper");

	@Test
	public void testGetIndividualMetrics() {
		List<Metrics> metricsList = idh.getIndividualMetrics(1);
		for (Metrics m : metricsList) {
			assertNotNull(m.getName());
			assertNotNull(m.getScore());
		}

	}

	@Test
	public void testGetIndividualMetricsTimeSeries() {
		Map<Integer, List<Map<java.sql.Date, Integer>>> metricsListMap = idh.getIndividualMetricsTimeSeries(1);
		for (int i = 1; i<= metricsListMap.keySet().size();i++) {
			assertTrue(!metricsListMap.keySet().isEmpty());
		}
	}
	
	@Test
	public void testIndividualInitiativeList(){
		List<Initiative> initiativeList = new ArrayList<>();
		initiativeList = idh.individualInitiativeList(208);
	}
	
	@Test
	public void testGetSmartList(){
		Map<Integer, Employee> employeeScoreMap = idh.getSmartList(1, 1);
		
	}
	
	@Test
	public void testChangePassword(){
		boolean status = idh.changePassword("abc123", "myNewPassword123", 1, 2);
		assertTrue(status);
	}
}
