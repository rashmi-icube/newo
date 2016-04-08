package org.icube.owen.test.survey;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.survey.Question;
import org.icube.owen.survey.QuestionList;
import org.junit.Test;

public class QuestionListTest {
	QuestionList ql = (QuestionList) ObjectFactory.getInstance("org.icube.owen.survey.QuestionList");

	@Test
	public void testGetQuestionList() {

		List<Question> questionList = ql.getQuestionList();
		assertTrue(!questionList.isEmpty());
		for (Question q : questionList) {
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
	}

	@Test
	public void testGetQuestionListForBatch() {
		List<Question> questionList = ql.getQuestionListForBatch(1);
		assertTrue(!questionList.isEmpty());
		for (Question q : questionList) {
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
	}

	@Test
	public void testGetQuestionListByStatus() {
		List<Question> questionList = ql.getQuestionListByStatus(1, "Upcoming");
		assertTrue(!questionList.isEmpty());
		for (Question q : questionList) {
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
		List<Question> questionList1 = ql.getQuestionListByStatus(1, "Completed");
		assertTrue(!questionList1.isEmpty());
		for (Question q : questionList1) {
			assertTrue(q.getQuestionId() > 0);
			assertTrue(!q.getQuestionText().isEmpty());
		}
	}

}
