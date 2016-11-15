package org.icube.owen.test.survey;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.survey.ResponseHelper;
import org.junit.Test;

public class ResponseHelperTest {

	ResponseHelper r = (ResponseHelper) ObjectFactory.getInstance("org.icube.owen.survey.Response");
	int companyId = 5;

	@Test
	public void testSaveMeResponse() {

		assertTrue(r.saveMeResponse(companyId, 64, 1, 3));
	}

	@Test
	public void testSaveWeResponse() {

		Employee e = new Employee();

		Map<Employee, Integer> employeeRating = new HashMap<>();
		employeeRating.put(e.get(companyId, 1), 5);
		employeeRating.put(e.get(companyId, 2), 6);
		employeeRating.put(e.get(companyId, 3), 7);
		employeeRating.put(e.get(companyId, 4), 8);
		employeeRating.put(e.get(companyId, 5), 9);

		assertTrue(r.saveWeResponse(companyId, 64, 1, employeeRating));
	}
}
