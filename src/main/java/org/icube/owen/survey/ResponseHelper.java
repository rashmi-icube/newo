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
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call insertMeResponse(?,?,?,?,?)}")) {
			cstmt.setInt("empid", employeeId);
			cstmt.setInt("queid", questionId);
			cstmt.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt("score", responseValue);
			cstmt.setString("feedbck", "");
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("SQL statement for question : " + questionId + " : " + cstmt.toString());
			try (ResultSet rs = cstmt.executeQuery()) {
				if (rs.next()) {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class)
							.debug("RS statement for question : " + questionId + " : " + rs.toString());
					responseSaved = true;
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("Successfully saved the response for : " + questionId);
				}
			}

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
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call insertWeResponse(?,?,?,?,?)}")) {

			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
					"Saving the response in the db for questionId " + questionId + "target employee " + targetEmployee + " with the response "
							+ responseValue);

			cstmt.setInt("empid", employeeId);
			cstmt.setInt("queid", questionId);
			cstmt.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt("targetid", targetEmployee);
			cstmt.setInt("wt", responseValue);
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("SQL statement for question : " + questionId + " : " + cstmt.toString());
			try (ResultSet rs = cstmt.executeQuery()) {
				if (rs.next()) {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class)
							.debug("RS statement for question : " + questionId + " : " + rs.toString());
					responseSaved = true;
				}
			}

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
		dch.refreshCompanyConnection(companyId);
		try (CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
				"{call isWeQuestionAnswered(?,?)}")) {
			cstmt1.setInt("empid", employeeId);
			cstmt1.setInt("queid", questionId);
			try (ResultSet res = cstmt1.executeQuery()) {
				res.next();
				if (!res.getBoolean("op")) {

					for (Employee e : employeeRating.keySet()) {
						try (CallableStatement cstmt2 = dch.companyConnectionMap.get(companyId).getDataSource().getConnection().prepareCall(
								"{call insertWeResponse(?,?,?,?,?)}")) {
							org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
									"Saving the response in the db for questionId " + questionId + "target employee " + e.getEmployeeId()
											+ " with the response " + employeeRating.get(e));

							cstmt2.setInt("empid", employeeId);
							cstmt2.setInt("queid", questionId);
							cstmt2.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
							cstmt2.setInt("targetid", e.getEmployeeId());
							cstmt2.setInt("wt", employeeRating.get(e));
							org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
									"SQL statement for question : " + questionId + " : " + cstmt2.toString());
							try (ResultSet rs = cstmt2.executeQuery()) {
								if (rs.next()) {
									org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
											"RS statement for question : " + questionId + " : " + rs.toString());
									responseSaved = true;
									count++;
								}
							}
						}
					}
					if (employeeRating.size() == count) {
						org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug("Successfully saved the response for : " + questionId);
					}

				} else {
					org.apache.log4j.Logger.getLogger(ResponseHelper.class).debug(
							"Response is already stored for question ID :" + questionId + " for employee ID : " + employeeId);
				}
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(ResponseHelper.class).error("Exception while saving the response for question : " + questionId, e);
		}

		return responseSaved;

	}
}
