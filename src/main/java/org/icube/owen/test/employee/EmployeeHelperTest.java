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
import org.icube.owen.helper.UtilHelper;
import org.junit.Ignore;
import org.junit.Test;

public class EmployeeHelperTest {
	EmployeeHelper eh = (EmployeeHelper) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeHelper");
	DateFormat dateFormat = new SimpleDateFormat(UtilHelper.dateFormat, Locale.ENGLISH);

	@Ignore
	public void testGetBasicEmployeeDetails() {
		BasicEmployeeDetails bed = eh.getBasicEmployeeDetails(2, 5);
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

	@Ignore
	public void testGetWorkExperienceDetails() {
		List<WorkExperience> wel = eh.getWorkExperienceDetails(2, 5);
		for (WorkExperience we : wel) {
			assertNotNull(we.getCompanyName());
			assertNotNull(we.getDesignation());
			assertNotNull(we.getDuration());
			assertNotNull(we.getEmployeeId());
			assertNotNull(we.getLocation());
			assertNotNull(we.getStartDate());
		}
	}

	@Ignore
	public void testGetEducationDetails() {
		List<EducationDetails> edl = eh.getEducationDetails(2, 5);
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

	@Ignore
	public void testGetEmployeeLanguageDetails() {
		List<LanguageDetails> ldl = eh.getEmployeeLanguageDetails(2, 5);
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

	@Ignore
	public void testUpdateBasicDetails() {
		BasicEmployeeDetails bed = eh.getBasicEmployeeDetails(1, 1);
		bed.setPhone("12345678");
		boolean status = eh.updateBasicDetails(1, bed);
		assertTrue(status);
	}

	@Ignore
	public void testAddWorkExperience() throws ParseException {
		WorkExperience wex = new WorkExperience();
		wex.setEmployeeId(5);
		wex.setCompanyName("Avandeo");
		wex.setDesignation("Senior Java Developer");
		wex.setLocation("Shanghai");
		wex.setStartDate(dateFormat.parse("2012-06-15"));
		assertTrue(eh.addWorkExperience(2, wex));

	}

	@Ignore
	public void testAddEducation() throws ParseException {
		EducationDetails ed = new EducationDetails();
		ed.setEmployeeId(5);
		ed.setInstitution("KC College");
		ed.setCertification("BScIT");
		ed.setStartDate(dateFormat.parse("2005-06-15"));
		ed.setEndDate(dateFormat.parse("2008-04-15"));
		ed.setLocation("Mumbai");
		assertTrue(eh.addEducation(2, ed));
	}

	@Ignore
	public void testAddLanguage() {
		LanguageDetails ld = new LanguageDetails();
		ld.setEmployeeId(5);
		ld.setLanguageId(3);
		ld.setLanguageName("Chinese");
		assertTrue(eh.addLanguage(2, ld));
	}

	@Ignore
	public void testGetLanguageMasterMap() {
		Map<Integer, String> languageMasterMap = eh.getLanguageMasterMap(1);
		assertNotNull(languageMasterMap);
	}
}
