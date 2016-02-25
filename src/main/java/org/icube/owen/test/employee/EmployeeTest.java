package org.icube.owen.test.employee;

import static org.junit.Assert.*;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.junit.Test;

public class EmployeeTest {
	Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
	
	@Test
	public void testGet(){
		Employee emp = new Employee();
		emp = e.get(1);
		assertNotNull(emp.getEmployeeId());
		assertNotNull(emp.getFirstName());
		assertNotNull(emp.getLastName());
		assertNotNull(emp.getCompanyEmployeeId());
		assertNotNull(emp.getReportingManagerId());
		assertNotNull(emp.getScore());
		
	}

}
