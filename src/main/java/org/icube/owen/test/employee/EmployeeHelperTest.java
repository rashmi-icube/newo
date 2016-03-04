package org.icube.owen.test.employee;

import static org.junit.Assert.*;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.EmployeeHelper;
import org.junit.Test;

public class EmployeeHelperTest {
	EmployeeHelper eh = (EmployeeHelper) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeHelper");
	
	@Test
	public void testRemoveWorkExperience(){
		boolean status = eh.removeWorkExperience(1, 2);
		assertTrue(status);
	}
	
	@Test
	public void testRemoveEducation(){
		boolean status = eh.removeEducation(1, 2);
		assertTrue(status);
	}
	
	@Test
	public void testRemoveLanguage(){
		boolean status = eh.removeLanguage(1, 1);
		assertTrue(status);
	}

}
