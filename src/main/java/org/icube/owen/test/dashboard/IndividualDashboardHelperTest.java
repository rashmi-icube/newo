package org.icube.owen.test.dashboard;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.dashboard.ActivityFeed;
import org.icube.owen.dashboard.IndividualDashboardHelper;
import org.icube.owen.employee.Employee;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.metrics.Metrics;
import org.junit.Test;

public class IndividualDashboardHelperTest {
	IndividualDashboardHelper idh = (IndividualDashboardHelper) ObjectFactory.getInstance("org.icube.owen.dashboard.IndividualDashboardHelper");

	@Test
	public void testGetActivityFeedList() {
		Map<Date, List<ActivityFeed>> result = idh.getActivityFeedList(1, 208, 2);
		for (Date d : result.keySet()) {
			List<ActivityFeed> afl = result.get(d);
			for (ActivityFeed af : afl) {
				assertNotNull(af.getActivityType());
				assertNotNull(af.getBodyText());
				assertNotNull(af.getDate());
				assertNotNull(af.getHeaderText());
				System.out.println(af.getActivityType() + " - " + af.getHeaderText() + " - " + af.getBodyText() + " - " + af.getDate());
			}
		}
	}

	@Test
	public void testGetIndividualMetrics() {
		List<Metrics> metricsList = idh.getIndividualMetrics(1, 1);
		for (Metrics m : metricsList) {
			assertNotNull(m.getName());
			assertNotNull(m.getScore());
		}

	}

	@Test
	public void testGetIndividualMetricsTimeSeries() {
		Map<Integer, List<Map<Date, Integer>>> metricsListMap = idh.getIndividualMetricsTimeSeries(1, 1);
		for (int i = 1; i <= metricsListMap.keySet().size(); i++) {
			assertTrue(!metricsListMap.keySet().isEmpty());
		}
	}

	@Test
	public void testIndividualInitiativeList() {
		List<Initiative> initiativeList = new ArrayList<>();
		initiativeList = idh.getIndividualInitiativeList(208);
		for (Initiative i : initiativeList) {
			assertNotNull(i.get(i.getInitiativeId()));
		}
	}

	@Test
	public void testGetSmartList() {
		List<Employee> employeeList = idh.getSmartList(1, 1);
		assertNotNull(employeeList);

	}

	@Test
	public void testSaveAppreciation() {
		Map<Employee, Integer> appreciationResponseMap = new HashMap<>();
		Employee e = new Employee();
		appreciationResponseMap.put(e.get(1), 5);
		appreciationResponseMap.put(e.get(2), 6);
		appreciationResponseMap.put(e.get(3), 7);
		appreciationResponseMap.put(e.get(4), 8);
		appreciationResponseMap.put(e.get(5), 9);
		boolean status = idh.saveAppreciation(1, 1, 1, appreciationResponseMap);
		assertTrue(status);

	}

	@Test
	public void testChangePassword() {
		boolean status = idh.changePassword(1, 100, "abc123", "efg456");
		assertTrue(status);
	}

	@Test
	public void testUpdateNotificationTimestamp() {
		boolean status = idh.updateNotificationTimestamp(1, 1);
		assertTrue(status);
	}

	@Test
	public void testGetNotificationsCount() {
		int count = idh.getNotificationsCount(1, 1);
		assertTrue(count > 0);
	}
}
