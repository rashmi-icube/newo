package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.time.DateUtils;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

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
				q.setRelationshipTypeId(rs.getInt("rel_id"));
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
		if (UtilHelper.getEndOfDay(endDate).before(Date.from(Instant.now()))) {
			status = "completed";
		} else if (UtilHelper.getStartOfDay(startDate).after(Date.from(Instant.now()))) {
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
				do {
					Date utilDate = new Date(rs.getDate("date").getTime());
					responseMap.put(utilDate, rs.getInt("responses"));
				} while (rs.next());
			} else {
				for (Date d = q.getStartDate(); d.before(Date.from(Instant.now())); d = UtilHelper.convertJavaDateToSqlDate(DateUtils.addDays(d, 1))) {
					responseMap.put(d, 0);
				}
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving response data", e);
		}
		org.apache.log4j.Logger.getLogger(Question.class).debug("Response : " + responseMap.toString());
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
			if (UtilHelper.getStartOfDay(q1.getStartDate()).compareTo(Date.from(Instant.now())) <= 0
					&& UtilHelper.getEndOfDay(q1.getEndDate()).after(Date.from(Instant.now()))) {
				q.setQuestionId(q1.getQuestionId());
				q.setQuestionText(q1.getQuestionText());
				q.setStartDate(q1.getStartDate());
				q.setEndDate(q1.getEndDate());
				q.setResponsePercentage(q1.getResponsePercentage());
				q.setQuestionType(q1.getQuestionType());
				q.setSurveyBatchId(q1.getSurveyBatchId());
				org.apache.log4j.Logger.getLogger(Question.class).debug("Retrieved the current question " + q.getQuestionId());
			}
		}
		return q;
	}

	/**
	 * Retrieves the list of questions for the employee
	 * @param companyId - Company ID of the employee
	 * @param employeeId - Employee ID
	 * @return a list of Question objects
	 */
	public List<Question> getEmployeeQuestionList(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Question> questionList = new ArrayList<>();
		Connection conn;
		try {
			conn = dch.getCompanyConnection(companyId);
			CallableStatement cstmt = conn.prepareCall("{call getEmpQuestionList(?,?)}");
			cstmt.setInt(1, employeeId);
			Date date = Date.from(Instant.now());
			cstmt.setDate(2, UtilHelper.convertJavaDateToSqlDate(date));
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Question q = new Question();
				q.setQuestionId(rs.getInt("que_id"));
				q.setQuestionText(rs.getString("question"));
				q.setStartDate(rs.getDate("start_date"));
				q.setEndDate(rs.getDate("end_date"));
				q.setRelationshipTypeId(rs.getInt("rel_id"));
				q.setSurveyBatchId(rs.getInt("survey_batch_id"));
				q.setQuestionType(QuestionType.get(rs.getInt("que_type")));
				q.setResponsePercentage(0);
				questionList.add(q);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving the questionList", e);
		}

		return questionList;
	}

	/**
	 * Returns the default smart list for the employee on the we question page
	 * 
	 * @return List of employee objects - view of the employee list should be sorted by the rank
	 */
	public List<Employee> getSmartListForQuestion(int companyId, int employeeId, Question q) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, Employee> employeeRankMap = new HashMap<>();
		List<Employee> employeeList = new ArrayList<>();

		try {
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(Question.class).debug("R Connection Available : " + rCon.isConnected());
			org.apache.log4j.Logger.getLogger(Question.class).debug("Filling up parameters for rscript function");
			rCon.assign("emp_id", new int[] { employeeId });
			rCon.assign("rel_id", new int[] { q.getRelationshipTypeId() });
			org.apache.log4j.Logger.getLogger(Question.class).debug("Calling the actual function in RScript SmartListResponse");
			REXP employeeSmartList = rCon.parseAndEval("try(eval(SmartListResponse(emp_id, rel_id)))");
			if (employeeSmartList.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(Question.class).error("Error: " + employeeSmartList.asString());
				throw new Exception("Error: " + employeeSmartList.asString());
			} else {
				org.apache.log4j.Logger.getLogger(Question.class).debug(
						"Retrieval of the employee smart list completed " + employeeSmartList.asList());
			}

			RList result = employeeSmartList.asList();
			REXPInteger empIdResult = (REXPInteger) result.get("emp_id");
			int[] empIdArray = empIdResult.asIntegers();
			REXPInteger rankResult = (REXPInteger) (result.get("Rank"));
			int[] rankArray = rankResult.asIntegers();

			for (int i = 0; i < empIdArray.length; i++) {
				Employee e = new Employee();
				e = e.get(empIdArray[i]);
				employeeRankMap.put(rankArray[i], e);
			}
			Map<Integer, Employee> sorted_map = new TreeMap<Integer, Employee>(employeeRankMap);
			employeeList = new ArrayList<Employee>(sorted_map.values());

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Error while trying to retrieve the smart list for employee from question", e);
		}

		finally {
			ObjectFactory.getDBHelper().releaseRcon();
		}

		return employeeList;
	}

}
