package org.icube.owen.test.dashboard;

import static org.junit.Assert.assertTrue;

import org.icube.owen.ObjectFactory;
import org.icube.owen.dashboard.Alert;
import org.junit.Test;

public class AlertTest {
	Alert a = (Alert) ObjectFactory.getInstance("org.icube.owen.dashboard.Alert");

	@Test
	public void testGet() {
		Alert alert = a.get(4);
		assertTrue(alert.getAlertId() > 0);
		assertTrue(!alert.getEmployeeList().isEmpty());
		assertTrue(!alert.getAlertTeam().isEmpty());
		assertTrue(!alert.getAlertStatement().isEmpty());
		assertTrue(!alert.getAlertMetric().toString().isEmpty());
		assertTrue(!alert.getFilterList().isEmpty());

	}

	@Test
	public void testDelete() {
		boolean status = a.delete(2);
		assertTrue(status == true);
	}

}
