package org.icube.owen.configure;

import java.util.Date;

import org.icube.owen.TheBorg;

public class Question extends TheBorg {

	private Date startDate;
	private Date endDate;
	private String questionText;
	private QuestionType questionType;
	private double responsePercentage;

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the questionText
	 */
	public String getQuestionText() {
		return questionText;
	}

	/**
	 * @param questionText the questionText to set
	 */
	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	/**
	 * @return the questionType
	 */
	public QuestionType getQuestionType() {
		return questionType;
	}

	/**
	 * @param questionType the questionType to set
	 */
	public void setQuestionType(QuestionType questionType) {
		this.questionType = questionType;
	}

	/**
	 * @return the responsePercentage
	 */
	public double getResponsePercentage() {
		return responsePercentage;
	}

	/**
	 * @param responsePercentage the responsePercentage to set
	 */
	public void setResponsePercentage(double responsePercentage) {
		this.responsePercentage = responsePercentage;
	}

}
