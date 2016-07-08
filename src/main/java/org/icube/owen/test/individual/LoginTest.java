package org.icube.owen.test.individual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.individual.Login;
import org.junit.Ignore;
import org.junit.Test;

public class LoginTest {
	Login l = (Login) ObjectFactory.getInstance("org.icube.owen.individual.Login");

	@Test
	public void testLoginEmployee() {
		try {
			Employee e = l.login("ssrivastava@i-cube.in", "abc123", "114.9.1.2", 1);
			assertNotNull(e.getEmployeeId());
			assertNotNull(e.getCompanyEmployeeId());
			assertNotNull(e.getCompanyEmployeeId());
			assertNotNull(e.getFirstName());
			assertNotNull(e.getLastName());
			assertNotNull(e.getReportingManagerId());
			assertNotNull(e.isFirstTimeLogin());
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Invalid credentials!!!");
		}
	}
	@Ignore
	public void testLoginHr() {
		try {
			Employee e = l.login("emp3@i-cube.in", "abc123", "114.9.1.2", 2);
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
