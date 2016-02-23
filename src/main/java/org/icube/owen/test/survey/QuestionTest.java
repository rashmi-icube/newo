package org.icube.owen.test.survey;

import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.survey.Question;
import org.junit.Test;

public class QuestionTest {
	Question q = (Question) ObjectFactory.getInstance("org.icube.owen.survey.Question");
	
	@Test
	public void testGetQuestion(){
		Question q1 = q.getQuestion(1);
		assertTrue(q1.getQuestionId() > 0);
		assertTrue(!q1.getQuestionText().isEmpty());
		assertTrue(q1.getSurveyBatchId() > 0);
	}
	
	@Test
	public void testgetResponse(){
		Map<Date, Integer> responseMap = q.getResponse(q.getQuestion(17));
		assertTrue(!responseMap.isEmpty());
		Map<Date, Integer> responseMap1 = q.getResponse(q.getQuestion(1));
		assertTrue(!responseMap1.isEmpty());
	}
	
	@Test
	public void testGetCurrentQuestion(){
		Question q1 = q.getCurrentQuestion(1);
		assertTrue(q1.getQuestionId() > 0);
		assertTrue(!q1.getQuestionText().isEmpty());
		assertTrue(q1.getSurveyBatchId() > 0);
	}
	
	@Test
	public void testGetQuestionStatus(){
		String status = q.getQuestionStatus(q.getQuestion(1).getStartDate(),q.getQuestion(1).getEndDate());
		assertTrue(!status.isEmpty());
	}

}