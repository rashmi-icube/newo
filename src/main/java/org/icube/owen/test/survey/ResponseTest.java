package org.icube.owen.test.survey;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.survey.Question;
import org.icube.owen.survey.Response;
import org.junit.Test;

public class ResponseTest {

	Response r = (Response) ObjectFactory.getInstance("org.icube.owen.survey.Response");
	int companyId = 1;

	@Test
	public void testSaveMeResponse() {
		Question q = new Question();
		assertTrue(r.saveMeResponse(1, 1, q.getQuestion(companyId, 1), 3, "test feedback"));
	}

	@Test
	public void testSaveWeResponse() {
		Question q = new Question();
		Employee e = new Employee();

		Map<Employee, Integer> employeeRating = new HashMap<>();
		employeeRating.put(e.get(companyId, 1), 5);
		employeeRating.put(e.get(companyId, 2), 6);
		employeeRating.put(e.get(companyId, 3), 7);
		employeeRating.put(e.get(companyId, 4), 8);
		employeeRating.put(e.get(companyId, 5), 9);

		assertTrue(r.saveWeResponse(1, 1, q.getQuestion(companyId, 1), employeeRating));
	}
}
