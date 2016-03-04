package org.icube.owen.test.employee;

import static org.junit.Assert.*;

import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.BasicEmployeeDetails;
import org.icube.owen.employee.EmployeeHelper;
import org.icube.owen.employee.WorkExperience;
import org.junit.Test;

public class EmployeeHelperTest {
	EmployeeHelper eh = (EmployeeHelper) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeHelper");
	
	@Test
	public void testGetBasicEmployeeDetails(){
		BasicEmployeeDetails bed = eh.getBasicEmployeeDetails(1, 1);
		assertNotNull(bed.getCompanyEmployeeId());
		assertNotNull(bed.getDesignation());
		assertNotNull(bed.getDob());
		assertNotNull(bed.getEmailId());
		assertNotNull(bed.getFirstName());
		assertNotNull(bed.getLastName());
		assertNotNull(bed.getLocation());
		assertNotNull(bed.getFunction());
		assertNotNull(bed.getPhone());
		assertNotNull(bed.getSalutation());
	}
	
	@Test
	public void testGetWorkExperienceDetails(){
		List<WorkExperience> wel = eh.getWorkExperienceDetails(1, 1);
		for(WorkExperience we : wel){
			assertNotNull(we.getCompanyName());
			assertNotNull(we.getDesignation());
			assertNotNull(we.getDuration());
			assertNotNull(we.getEmployeeId());
			assertNotNull(we.getEndDate());
			assertNotNull(we.getLocation());
			assertNotNull(we.getStartDate());
		}
	}
	
	
	
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
