package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class ResponseHelper extends TheBorg {

	/**
	 * Save all responses for ME/WE/MOOD question using the same function
	 * @param responseList - answer objects
	 * @return true/false - if the response is saved or not
	 */
	public boolean saveAllResponses(List<Response> responseList) {
		boolean allResponsesSaved = true;

		if (!responseList.isEmpty()) {
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("Entering saveAllResponses");

			for (int i = 0; i < responseList.size(); i++) {
				Response respObj = responseList.get(i);
				if (respObj.getQuestionType() == QuestionType.ME || respObj.getQuestionType() == QuestionType.MOOD) {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
							"Entering saveAllResponses (ME/MOOD) for question ID" + respObj.getQuestionId() + " for employee ID : "
									+ respObj.getEmployeeId());
					boolean flag = saveMeResponse(respObj.getCompanyId(), respObj.getEmployeeId(), respObj.getQuestionId(), respObj
							.getResponseValue());
					allResponsesSaved = (allResponsesSaved || flag);

				} else {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
							"Entering saveAllResponses (WE) for question ID" + respObj.getQuestionId() + " for employee ID : "
									+ respObj.getEmployeeId());
					boolean flag = saveWeResponse(respObj.getCompanyId(), respObj.getEmployeeId(), respObj.getQuestionId(), respObj
							.getTargetEmployee(), respObj.getResponseValue());
					allResponsesSaved = (allResponsesSaved || flag);
				}
			}
		}

		return allResponsesSaved;
	}

	/**
	 * Saves the response for the ME question
	 * @param companyId - Company ID
	 * @param employeeId - ID of the employee who is logged in
	 * @param questionId 
	 * @param responseValue - Value of the response
	 * @param feedback - The comments for the question
	 * @return true/false - if the response is saved or not
	 */
	public boolean saveMeResponse(int companyId, int employeeId, int questionId, int responseValue) {
		boolean responseSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call insertMeResponse(?,?,?,?,?)}");
			cstmt.setInt("empid", employeeId);
			cstmt.setInt("queid", questionId);
			cstmt.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt("score", responseValue);
			cstmt.setString("feedbck", "");
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("SQL statement for question : " + questionId + " : " + cstmt.toString());
			ResultSet rs = cstmt.executeQuery();
			if (rs.next()) {
				org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("RS statement for question : " + questionId + " : " + rs.toString());
				responseSaved = true;
				org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("Successfully saved the response for : " + questionId);
			}
			rs.close();
			cstmt.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).error("Exception while saving the response for question : " + questionId, e);
		}

		return responseSaved;
	}

	/**
	 * Save we response from the all questions function
	 * @param companyId
	 * @param employeeId - logged in employee
	 * @param questionId
	 * @param targetEmployee
	 * @param responseValue
	 * @return true/false - if the response is saved or not
	 */
	public boolean saveWeResponse(int companyId, int employeeId, int questionId, int targetEmployee, int responseValue) {
		boolean responseSaved = false;
		org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
				"Entering the saveWeResponse for companyId " + companyId + " employeeId " + employeeId);
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			dch.getCompanyConnection(companyId);
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
					"Saving the response in the db for questionId " + questionId + "target employee " + targetEmployee + " with the response "
							+ responseValue);

			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call insertWeResponse(?,?,?,?,?)}");
			cstmt.setInt("empid", employeeId);
			cstmt.setInt("queid", questionId);
			cstmt.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt("targetid", targetEmployee);
			cstmt.setInt("wt", responseValue);
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("SQL statement for question : " + questionId + " : " + cstmt.toString());
			ResultSet rs = cstmt.executeQuery();
			if (rs.next()) {
				org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("RS statement for question : " + questionId + " : " + rs.toString());
				responseSaved = true;
			}
			rs.close();
			cstmt.close();
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
					"Successfully saved the response for questionId " + questionId + "target employee " + targetEmployee + " with the response "
							+ responseValue);

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).error(
					"Exception while saving the response for questionId " + questionId + "target employee " + targetEmployee + " with the response "
							+ responseValue, e);
		}
		return responseSaved;
	}

	/**
	 * Saves the response for the WE question
	 * @param companyId - ID of the company to which the employee belongs
	 * @param employeeId - ID of the employee who is logged in
	 * @param q - A question object
	 * @param employeeRating - Ratings given in the answer
	 * @return true/false - if the response is saved successfully or not
	 */
	public boolean saveWeResponse(int companyId, int employeeId, int questionId, Map<Employee, Integer> employeeRating) {
		org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
				"Entering the saveWeResponse for companyId " + companyId + " employeeId " + employeeId);
		boolean responseSaved = false;
		int count = 0;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		try {
			CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call isWeQuestionAnswered(?,?)}");
			cstmt1.setInt("empid", employeeId);
			cstmt1.setInt("queid", questionId);
			ResultSet res = cstmt1.executeQuery();
			res.next();
			cstmt1.close();
			if (!res.getBoolean("op")) {
				dch.getCompanyConnection(companyId);

				for (Employee e : employeeRating.keySet()) {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
							"Saving the response in the db for questionId " + questionId + "target employee " + e.getEmployeeId()
									+ " with the response " + employeeRating.get(e));

					cstmt1 = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call insertWeResponse(?,?,?,?,?)}");
					cstmt1.setInt("empid", employeeId);
					cstmt1.setInt("queid", questionId);
					cstmt1.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
					cstmt1.setInt("targetid", e.getEmployeeId());
					cstmt1.setInt("wt", employeeRating.get(e));
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
							"SQL statement for question : " + questionId + " : " + cstmt1.toString());
					ResultSet rs = cstmt1.executeQuery();
					if (rs.next()) {
						org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
								"RS statement for question : " + questionId + " : " + rs.toString());
						responseSaved = true;
						count++;
					}
					rs.close();
					cstmt1.close();
				}
				if (employeeRating.size() == count) {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("Successfully saved the response for : " + questionId);
				}

			} else {
				org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
						"Response is already stored for question ID :" + questionId + " for employee ID : " + employeeId);
			}
			res.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).error("Exception while saving the response for question : " + questionId, e);
		}

		return responseSaved;

	}
}
