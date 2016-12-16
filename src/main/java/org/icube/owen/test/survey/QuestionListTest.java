package org.icube.owen.test.survey;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.survey.Question;
import org.icube.owen.survey.QuestionList;
import org.junit.Test;

public class QuestionListTest {
	QuestionList ql = (QuestionList) ObjectFactory.getInstance("org.icube.owen.survey.QuestionList");
	int companyId = 3;

	@Test
	public void testGetQuestionList() {

		List<Question> questionList = ql.getQuestionList(companyId);
		assertTrue(!questionList.isEmpty());
		for (Question q : questionList) {
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
	}

	@Test
	public void testGetQuestionListForBatch() {
		List<Question> questionList = ql.getQuestionListForBatch(companyId, 6);
		assertTrue(!questionList.isEmpty());
		for (Question q : questionList) {
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
	}

	@Test
	public void testGetQuestionListByStatus() {
		List<Question> questionList = ql.getQuestionListByStatus(companyId, 6, "Current");
		//assertTrue(!questionList.isEmpty());
		for (Question q : questionList) {
			System.out.println(q.getEndDate());
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
		List<Question> questionList1 = ql.getQuestionListByStatus(companyId, 1, "Completed");
		//assertTrue(!questionList1.isEmpty());
		for (Question q : questionList1) {
			System.out.println(q.getEndDate());
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
	}

}
