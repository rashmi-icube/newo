package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class Question extends TheBorg {

	private Date startDate;
	private Date endDate;
	private String questionText;
	private QuestionType questionType;
	private double responsePercentage;
	private int questionId;
	private int surveyBatchId;
	private int relationshipTypeId;

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public QuestionType getQuestionType() {
		return questionType;
	}

	public void setQuestionType(QuestionType questionType) {
		this.questionType = questionType;
	}

	public double getResponsePercentage() {
		return responsePercentage;
	}

	public void setResponsePercentage(double responsePercentage) {
		this.responsePercentage = responsePercentage;
	}

	public int getQuestionId() {
		return questionId;
	}

	public void setQuestionId(int questionId) {
		this.questionId = questionId;
	}

	public int getSurveyBatchId() {
		return surveyBatchId;
	}

	public void setSurveyBatchId(int surveyBatchId) {
		this.surveyBatchId = surveyBatchId;
	}

	public int getRelationshipTypeId() {
		return relationshipTypeId;
	}

	public void setRelationshipTypeId(int relationshipTypeId) {
		this.relationshipTypeId = relationshipTypeId;
	}

	/**
	 * Retrieves the question based on the question ID passed
	 * @param questionId - ID of the question to be retrieved
	 * @return a Question object
	 */
	public Question getQuestion(int questionId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Question q = new Question();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getQuestion(?)}");
			cstmt.setInt(1, questionId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				q.setEndDate(rs.getDate("end_date"));
				q.setStartDate(rs.getDate("start_date"));
				q.setQuestionText(rs.getString("question"));
				q.setQuestionId(rs.getInt("que_id"));
				q.setResponsePercentage(rs.getDouble("resp"));
				q.setQuestionType(QuestionType.values()[rs.getInt("que_type")]);
				q.setSurveyBatchId(rs.getInt("survey_batch_id"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving Question with ID" + questionId, e);
		}
		return q;
	}

	/**
	 * Retrieves the status of the question
	 * @param startDate - Start date of the question
	 * @param endDate - End date of the question
	 * @return - the status of the question based on startDate and endDate
	 */
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

	/**
	 * Retrieves the response data for a question
	 * @param q - a Question object for which the response data is required
	 * @return - A map containing the responses and the date
	 */
	public Map<Date, Integer> getResponse(Question q) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Date, Integer> responseMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getResponseData(?)}");
			cstmt.setInt(1, q.getQuestionId());
			ResultSet rs = cstmt.executeQuery();
			if (rs.next()) {
				while (rs.next()) {
					responseMap.put(rs.getDate("date"), rs.getInt("responses"));
				}
			} else {
				for (Date d = q.getStartDate(); d.before(Date.from(Instant.now())); d = UtilHelper.convertJavaDateToSqlDate(DateUtils.addDays(d, 1))) {
					responseMap.put(d, 0);
				}
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving response data", e);
		}
		return responseMap;

	}

	/**
	 * Retrieves the current question
	 * @param batchId - batch ID of the question
	 * @return - the current Question object
	 */
	public Question getCurrentQuestion(int batchId) {
		Question q = new Question();
		QuestionList ql = new QuestionList();
		for (Question q1 : ql.getQuestionListForBatch(batchId)) {
			if ((q1.getStartDate().compareTo(Date.from(Instant.now())) <= 0) && (q1.getEndDate().after(Date.from(Instant.now())))) {
				q.setQuestionId(q1.getQuestionId());
				q.setQuestionText(q1.getQuestionText());
				q.setStartDate(q1.getStartDate());
				q.setEndDate(q1.getEndDate());
				q.setResponsePercentage(q1.getResponsePercentage());
				q.setQuestionType(q1.getQuestionType());
				q.setSurveyBatchId(q1.getSurveyBatchId());
			}
		}
		return q;
	}

	public List<Question> getEmployeeQuestionList(int employeeId, int companyId) {
		List<Question> questionList = new ArrayList<>();

		// check whether connection to company db exists
		// send employeeId, requestedDate(now) to SQL ... receive a single questionId
		// return question object from the questionId

		return questionList;
	}

	public Map<Integer, Employee> getSmartListForQuestion(int employeeId, int companyId, Question q) {

		Map<Integer, Employee> employeeScoreMap = new HashMap<>();

		return employeeScoreMap;
	}

	public Map<Integer, Employee> getSmartListForQuestionByFilters(int employeeId, int companyId, Question q, List<Filter> filterList) {
		Map<Integer, Employee> employeeScoreMap = new HashMap<>();
		// order doesn't matter for employee list
		return employeeScoreMap;

	}
}
