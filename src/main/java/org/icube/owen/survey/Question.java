package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class Question extends TheBorg {

	private Date startDate;
	private Date endDate;
	private String questionText;
	private QuestionType questionType;
	private double responsePercentage;
	private int questionId;
	private int surveyBatchId;

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

	/**
	 * @return the questionId
	 */
	public int getQuestionId() {
		return questionId;
	}

	/**
	 * @param questionId the questionId to set
	 */
	public void setQuestionId(int questionId) {
		this.questionId = questionId;
	}

	/**
	 * @return the surveyBatchId
	 */
	public int getSurveyBatchId() {
		return surveyBatchId;
	}

	/**
	 * @param surveyBatchId the surveyBatchId to set
	 */
	public void setSurveyBatchId(int surveyBatchId) {
		this.surveyBatchId = surveyBatchId;
	}

	public Question getQuestion(int questionId) {
		// TODO Swarna fill the functionality
		return new Question();
	}

	public String getQuestionStatus(Date startDate, Date endDate) {
		String status = "";

		if (endDate.before(Date.from(Instant.now()))) {
			status = "completed";
		} else if (startDate.after(Date.from(Instant.now()))) {
			status = "upcoming";
		} else {
			status = "current";
		}

		return status;
	}
	
	public Map<Date,Integer> getResponse(Question q){
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Date,Integer> responseMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getResponseData(?)}");
			cstmt.setInt(1, q.getQuestionId());
			ResultSet rs = cstmt.executeQuery();
			while(rs.next()){
				responseMap.put(rs.getDate("date"), rs.getInt("responses"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return responseMap;
		
	}
}
