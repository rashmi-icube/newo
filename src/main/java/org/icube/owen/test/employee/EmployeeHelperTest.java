package org.icube.owen.test.employee;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.BasicEmployeeDetails;
import org.icube.owen.employee.EducationDetails;
import org.icube.owen.employee.EmployeeHelper;
import org.icube.owen.employee.LanguageDetails;
import org.icube.owen.employee.WorkExperience;
import org.junit.Test;

public class EmployeeHelperTest {
	EmployeeHelper eh = (EmployeeHelper) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeHelper");
	DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);

	@Test
	public void testGetBasicEmployeeDetails() {
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
	public void testGetWorkExperienceDetails() {
		List<WorkExperience> wel = eh.getWorkExperienceDetails(1, 1);
		for (WorkExperience we : wel) {
			assertNotNull(we.getCompanyName());
			assertNotNull(we.getDesignation());
			assertNotNull(we.getDuration());
			assertNotNull(we.getEmployeeId());
			assertNotNull(we.getLocation());
			assertNotNull(we.getStartDate());
		}
	}

	@Test
	public void testGetEducationDetails() {
		List<EducationDetails> edl = eh.getEducationDetails(1, 1);
		for (EducationDetails ed : edl) {
			assertNotNull(ed.getEmployeeId());
			assertNotNull(ed.getEducationDetailsId());
			assertNotNull(ed.getInstitution());
			assertNotNull(ed.getCertification());
			assertNotNull(ed.getStartDate());
			assertNotNull(ed.getEndDate());
			assertNotNull(ed.getLocation());
		}
	}

	@Test
	public void testGetEmployeeLanguageDetails() {
		List<LanguageDetails> ldl = eh.getEmployeeLanguageDetails(1, 1);
		for (LanguageDetails ld : ldl) {
			assertNotNull(ld.getEmployeeId());
			assertNotNull(ld.getLanguageDetailsId());
			assertNotNull(ld.getLanguageId());
			assertNotNull(ld.getLanguageName());
		}
	}

	@Test
	public void testRemoveWorkExperience() {
		boolean status = eh.removeWorkExperience(1, 2);
		assertTrue(status);
	}

	@Test
	public void testRemoveEducation() {
		boolean status = eh.removeEducation(1, 2);
		assertTrue(status);
	}

	@Test
	public void testRemoveLanguage() {
		boolean status = eh.removeLanguage(1, 2);
		assertTrue(status);
	}

	@Test
	public void testUpdateBasicDetails() {
		BasicEmployeeDetails bed = eh.getBasicEmployeeDetails(1, 1);
		bed.setPhone("12345678");
		boolean status = eh.updateBasicDetails(1, bed);
		assertTrue(status);
	}

	@Test
	public void testAddWorkExperience() throws ParseException {
		WorkExperience wex = new WorkExperience();
		wex.setEmployeeId(1);
		wex.setCompanyName("Avandeo");
		wex.setDesignation("Senior Java Developer");
		wex.setLocation("Shanghai");
		wex.setStartDate(dateFormat.parse("June 15, 2012"));
		assertTrue(eh.addWorkExperience(1, wex));

	}

	@Test
	public void testAddEducation() throws ParseException {
		EducationDetails ed = new EducationDetails();
		ed.setEmployeeId(1);
		ed.setInstitution("KC College");
		ed.setCertification("BScIT");
		ed.setStartDate(dateFormat.parse("June 15, 2005"));
		ed.setEndDate(dateFormat.parse("April 15, 2008"));
		ed.setLocation("Mumbai");
		assertTrue(eh.addEducation(1, ed));
	}

	@Test
	public void testAddLanguage() {
		LanguageDetails ld = new LanguageDetails();
		ld.setEmployeeId(1);
		ld.setLanguageId(3);
		ld.setLanguageName("Chinese");
		assertTrue(eh.addLanguage(1, ld));
	}

	@Test
	public void testGetLanguageMasterMap() {
		Map<Integer, String> languageMasterMap = eh.getLanguageMasterMap(1);
		assertNotNull(languageMasterMap);
	}
}
