package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.helper.CompanyConfig;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	 * @param companyId - Company ID
	 * @param questionId - ID of the question to be retrieved
	 * @return a Question object
	 */

	public Question getQuestion(int companyId, int questionId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Question q = new Question();
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall("{call getQuestion(?)}")) {
			cstmt.setInt(1, questionId);
			try (ResultSet rs = cstmt.executeQuery()) {
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
	 * @param companyId - ID of the company for which response is retrieved
	 * @param q - a Question object for which the response data is required
	 * @return - A map containing the responses and the date
	 */

	public Map<Date, Integer> getResponse(int companyId, Question q) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		org.apache.log4j.Logger.getLogger(Question.class).info("HashMap created!!!");
		Map<Date, Integer> responseMap = new HashMap<>();
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getResponseData(?)}")) {
			cstmt.setInt(1, q.getQuestionId());
			try (ResultSet rs = cstmt.executeQuery()) {

				if (rs.next()) {
					org.apache.log4j.Logger.getLogger(Question.class).debug("Response available for question : " + q.getQuestionId());
					do {
						Date utilDate = new Date(rs.getDate("date").getTime());
						responseMap.put(utilDate, rs.getInt("responses"));
					} while (rs.next());
				} else {
					// if no response is available for the question we return an empty map with the response count 0 from the date the question was
					// started
					org.apache.log4j.Logger.getLogger(Question.class).debug("No response available for question : " + q.getQuestionId());
					for (Date d = q.getStartDate(); d.before(Date.from(Instant.now())); d = UtilHelper.convertJavaDateToSqlDate(DateUtils.addDays(d,
							1))) {
						responseMap.put(d, 0);
					}
				}
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving response data", e);
		}
		org.apache.log4j.Logger.getLogger(Question.class).debug("Response map for question : " + q.getQuestionId() + " : " + responseMap.toString());
		return responseMap;

	}

	/**
	 * Retrieves the current question
	 * @param companyId - Company ID 
	 * @param batchId - batch ID of the question
	 * @return - the current Question object
	 */

	public Question getCurrentQuestion(int companyId, int batchId) {
		Question q = null;
		QuestionList ql = new QuestionList();
		for (Question q1 : ql.getQuestionListForBatch(companyId, batchId)) {
			if (UtilHelper.getStartOfDay(q1.getStartDate()).compareTo(Date.from(Instant.now())) <= 0
					&& UtilHelper.getEndOfDay(q1.getEndDate()).after(Date.from(Instant.now()))) {
				q = new Question();
				q.setQuestionId(q1.getQuestionId());
				q.setQuestionText(q1.getQuestionText());
				q.setStartDate(q1.getStartDate());
				q.setEndDate(q1.getEndDate());
				q.setResponsePercentage(q1.getResponsePercentage());
				q.setQuestionType(q1.getQuestionType());
				q.setSurveyBatchId(q1.getSurveyBatchId());
				q.setRelationshipTypeId(q1.getRelationshipTypeId());
				org.apache.log4j.Logger.getLogger(Question.class).debug("Retrieved the current question " + q.getQuestionId());
			}
		}

		// if there isn't any current question return null so that the UI doesn't break
		if (q == null) {
			org.apache.log4j.Logger.getLogger(Question.class).debug("No current question to display");
			return null;
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
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getEmpQuestionList(?,?)}")) {
			cstmt.setInt(1, employeeId);
			Date date = Date.from(Instant.now());
			cstmt.setDate(2, UtilHelper.convertJavaDateToSqlDate(date));
			try (ResultSet rs = cstmt.executeQuery()) {
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
					org.apache.log4j.Logger.getLogger(Question.class).debug(
							"Question for employee : " + q.getQuestionId() + " - " + q.getQuestionText() + " - " + q.getRelationshipTypeId());

					questionList.add(q);
				}
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving the questionList", e);
		}

		return questionList;
	}

	public String getJsonEmployeeQuestionList(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		JSONArray arr = new JSONArray();
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getEmpQuestionList(?,?)}")) {

			cstmt.setInt(1, employeeId);
			Date date = Date.from(Instant.now());
			cstmt.setDate(2, UtilHelper.convertJavaDateToSqlDate(date));
			try (ResultSet rs = cstmt.executeQuery()) {
				while (rs.next()) {
					JSONObject json = new JSONObject();
					json.put("questionId", rs.getInt("que_id"));
					json.put("questionText", rs.getString("question"));
					json.put("questionType", QuestionType.get(rs.getInt("que_type")));
					arr.put(json);
				}
			}

		} catch (SQLException | JSONException e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving the questionList", e);
		}
		return arr.toString();
	}

	/**
	 * Returns the default smart list for the employee on the we question page
	 * @param companyId - ID of the company to which the logged in user belongs
	 * @param employeeId - Employee ID of the individual who is logged in
	 * @param q - A question object
	 * @return List of employee objects - view of the employee list should be sorted by the rank
	 */
	public List<Employee> getSmartListForQuestion(int companyId, int employeeId, int questionId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeList = new ArrayList<>();
		EmployeeList el = new EmployeeList();
		dch.refreshCompanyConnection(companyId);
		try {

			CompanyConfig ccObj = dch.companyConfigMap.get(companyId);
			if (ccObj.getSmartList().equals("all_employee")) {
				org.apache.log4j.Logger.getLogger(Question.class).debug("Calling getEmployeeMasterList");
				employeeList.addAll(el.getEmployeeMasterList(companyId));
			} else if (ccObj.getSmartList().equals("cube")) {
				try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
						"{call getListColleague(?)}")) {
					cstmt.setString("array", String.valueOf(employeeId));
					try (ResultSet rs = cstmt.executeQuery()) {
						List<Integer> empIdList = new ArrayList<>();
						while (rs.next()) {
							empIdList.add(rs.getInt("emp_id"));
						}
						Employee e = new Employee();
						employeeList = e.get(companyId, empIdList);
					}
				}

			} else {
				Employee e = new Employee();
				employeeList = e.get(companyId, getDynamicSmartList(companyId, employeeId, questionId));
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Error while trying to retrieve the smart list for employee from question", e);
		} finally {
			dch.releaseRcon();
		}
		return employeeList;
	}

	public List<Integer> getDynamicSmartList(int companyId, int employeeId, int questionId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.refreshCompanyConnection(companyId);
		Question q = getQuestion(companyId, questionId);
		List<Integer> empIdList = new ArrayList<>();

		// get relationship name
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call getRelationNameFromId(?)}")) {
			cstmt.setInt("relid", q.getRelationshipTypeId());
			String relationName = "";
			try (ResultSet rs = cstmt.executeQuery()) {
				while (rs.next()) {
					relationName = rs.getString("rel_name");
				}
			}

			// get first connections
			String firstConnectionQuery = "match (a:Employee {emp_id:" + employeeId + "})-[r:" + relationName
					+ "]->(b:Employee) return b.emp_id as emp_id,r.weight as weight";
			Map<Integer, Double> connectionsMap = new HashMap<>();
			try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement();
					ResultSet res = stmt.executeQuery(firstConnectionQuery)) {
				while (res.next()) {
					connectionsMap.put(res.getInt("emp_id"), res.getDouble("weight"));
				}
			}

			// if first connections aren't empty fetch the second connections
			if (!connectionsMap.isEmpty()) {
				String secondConnectionQuery = "match (a:Employee {emp_id:" + employeeId + "})-[r:" + relationName + "]->(b:Employee)-[:"
						+ relationName + "]->(c:Employee) return b.emp_id,c.emp_id as emp_id,r.weight as weight";

				try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement();
						ResultSet res = stmt.executeQuery(secondConnectionQuery)) {
					while (res.next()) {
						int empId = res.getInt("emp_id");
						double weight = res.getDouble("weight");
						if (!connectionsMap.containsKey(empId)) {
							connectionsMap.put(empId, weight);
						} else {
							if (connectionsMap.get(empId) < weight) {
								connectionsMap.put(empId, weight);
							}
						}
					}
				}
			}

			// get inactive employee list
			List<Integer> inactiveEmpList = new ArrayList<>();
			try (CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
					"{call getInactiveEmp()}");
					ResultSet rs1 = cstmt1.executeQuery()) {
				while (rs1.next()) {
					inactiveEmpList.add(rs1.getInt("empid"));
				}
			}

			// remove inactive employee & signed in employee ID from connections
			connectionsMap.keySet().removeAll(inactiveEmpList);
			connectionsMap.keySet().remove(employeeId);

			// get cube employees
			List<Integer> cubeEmpIdList = new ArrayList<>();
			try (CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
					"{call getListColleague(?)}")) {
				cstmt1.setString("array", String.valueOf(employeeId));
				try (ResultSet rs = cstmt1.executeQuery()) {

					while (rs.next()) {
						cubeEmpIdList.add(rs.getInt("emp_id"));
					}
					cubeEmpIdList.removeAll(connectionsMap.keySet());
					cubeEmpIdList.remove(Integer.valueOf(employeeId));
				}
			}

			// sort map by weight
			Map<Integer, Double> sortedConnectionsMap = sortByValue(connectionsMap);

			// build final empId list
			empIdList.addAll(sortedConnectionsMap.keySet());
			empIdList.addAll(cubeEmpIdList);

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Error while trying to retrieve the smart list for employee", e);
		}
		return empIdList;
	}

	private Map<Integer, Double> sortByValue(Map<Integer, Double> map) {
		List<Map.Entry<Integer, Double>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
			@Override
			public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<Integer, Double> result = new LinkedHashMap<>();
		for (Map.Entry<Integer, Double> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

}
