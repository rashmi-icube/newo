package org.icube.owen.test.individual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.individual.Login;
import org.junit.Test;

public class LoginTest {
	Login l = (Login) ObjectFactory.getInstance("org.icube.owen.individual.Login");

	@Test
	public void testLogin() {
		try {
			Employee e = l.login("emp5@i-cube.in", "abc123", "114.9.1.2", 1);
			assertNotNull(e.getEmployeeId());
			assertNotNull(e.getCompanyEmployeeId());
			assertNotNull(e.getCompanyEmployeeId());
			assertNotNull(e.getFirstName());
			assertNotNull(e.getLastName());
			assertNotNull(e.getReportingManagerId());
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Invalid credentials!!!");
		}
	}
}
