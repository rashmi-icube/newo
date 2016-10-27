package org.icube.owen.survey;

import org.icube.owen.TheBorg;

public class Response extends TheBorg {

	private int questionId;
	private QuestionType questionType;
	private int responseValue;
	private String feedback;
	private int targetEmployee;

	public int getQuestionId() {
		return questionId;
	}

	public void setQuestionId(int questionId) {
		this.questionId = questionId;
	}

	public QuestionType getQuestionType() {
		return questionType;
	}

	public void setQuestionType(QuestionType questionType) {
		this.questionType = questionType;
	}

	public int getResponseValue() {
		return responseValue;
	}

	public void setResponseValue(int responseValue) {
		this.responseValue = responseValue;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public int getTargetEmployee() {
		return targetEmployee;
	}

	public void setTargetEmployee(int targetEmployee) {
		this.targetEmployee = targetEmployee;
	}

}
