package org.icube.owen.test.survey;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.survey.Question;
import org.junit.Ignore;
import org.junit.Test;

public class QuestionTest {
	Question q = (Question) ObjectFactory.getInstance("org.icube.owen.survey.Question");
	int companyId = 3;

	@Ignore
	public void testGetQuestion() {
		Question q1 = q.getQuestion(companyId, 1);
		assertTrue(q1.getQuestionId() > 0);
		assertTrue(!q1.getQuestionText().isEmpty());
		assertTrue(q1.getSurveyBatchId() > 0);
	}

	@Test
	public void testGetResponse() {
		Map<Date, Integer> responseMap = q.getResponse(companyId, q.getQuestion(companyId, 17));
		assertTrue(!responseMap.isEmpty());
		responseMap = q.getResponse(companyId, q.getQuestion(companyId, 2));
		assertTrue(!responseMap.isEmpty());
		responseMap = q.getResponse(companyId, q.getCurrentQuestion(companyId, 1));
		assertTrue(!responseMap.isEmpty());
	}

	@Ignore
	public void testGetCurrentQuestion() {
		Question q1 = q.getCurrentQuestion(companyId, 1);
		assertTrue(q1.getQuestionId() > 0);
		assertTrue(!q1.getQuestionText().isEmpty());
		assertTrue(q1.getSurveyBatchId() > 0);
	}

	@Ignore
	public void testGetQuestionStatus() {
		String status = q.getQuestionStatus(q.getQuestion(companyId, 1).getStartDate(), q.getQuestion(companyId, 1).getEndDate());
		assertTrue(!status.isEmpty());
	}

	@Ignore
	public void testGetEmployeeQuestionList() {
		List<Question> ql = new ArrayList<Question>();
		ql = q.getEmployeeQuestionList(1, 30);
		assertNotNull(ql);
	}

	@Ignore
	public void testGetSmartListForQuestion() {
		// smart list test
		List<Employee> result = q.getSmartListForQuestion(companyId, 1, 1);
		assertNotNull(result);

		// cube list test
		result = q.getSmartListForQuestion(4, 1, 1);
		assertNotNull(result);
	}

}
