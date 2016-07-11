package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class Response extends TheBorg {

	/**
	 * Saves the response for the ME question
	 * @param companyId - Company ID
	 * @param employeeId - ID of the employee who is logged in
	 * @param q - the question object
	 * @param responseValue - Value of the response
	 * @param feedback - The comments for the question
	 * @return true/false - if the response is saved or not
	 */
	public boolean saveMeResponse(int companyId, int employeeId, Question q, int responseValue, String feedback) {
		boolean responseSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call insertMeResponse(?,?,?,?,?,?)}");
			cstmt.setInt("empid", employeeId);
			cstmt.setInt("queid", q.getQuestionId());
			cstmt.setTimestamp("responsetime", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt("score", responseValue);
			if (q.getRelationshipTypeId() == 0) {
				cstmt.setNull("relid", Types.INTEGER);
			} else {
				cstmt.setInt("relid", q.getRelationshipTypeId());
			}
			cstmt.setString("feedbck", feedback);
			ResultSet rs = cstmt.executeQuery();
			if (rs.next()) {
				responseSaved = true;
				org.apache.log4j.Logger.getLogger(Response.class).debug(
						"Successfully saved the response for : " + q.getQuestionText() + " with the relationship ID " + q.getRelationshipTypeId());
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Response.class).error("Exception while saving the response for question : " + q.getQuestionText(), e);
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
	public boolean saveWeResponse(int companyId, int employeeId, Question q, Map<Employee, Integer> employeeRating) {
		org.apache.log4j.Logger.getLogger(Response.class).debug(
				"Entering the saveWeResponse for companyId " + companyId + " employeeId " + employeeId);
		boolean responseSaved = false;
		int count = 0;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		try {
			CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call isWeQuestionAnswered(?,?)}");
			cstmt1.setInt(1, employeeId);
			cstmt1.setInt(2, q.getQuestionId());
			ResultSet res = cstmt1.executeQuery();
			res.next();
			if (!res.getBoolean("op")) {
				dch.getCompanyConnection(companyId);

				for (Employee e : employeeRating.keySet()) {
					org.apache.log4j.Logger.getLogger(Response.class).debug(
							"Saving the response in the db for questionId " + q.getQuestionId() + "target employee " + e.getEmployeeId()
									+ " with the response " + employeeRating.get(e));

					CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall(
							"{call insertWeResponse(?,?,?,?,?,?)}");
					cstmt.setInt(1, employeeId);
					cstmt.setInt(2, q.getQuestionId());
					cstmt.setTimestamp(3, UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
					cstmt.setInt(4, e.getEmployeeId());
					cstmt.setInt(5, q.getRelationshipTypeId());
					cstmt.setInt(6, employeeRating.get(e));
					ResultSet rs = cstmt.executeQuery();
					if (rs.next()) {
						responseSaved = true;
						count++;
					}
				}
				if (employeeRating.size() == count) {
					org.apache.log4j.Logger.getLogger(Response.class)
							.debug("Successfully saved the response for : " + q.getQuestionText() + " with the relationship ID "
									+ q.getRelationshipTypeId());
				}
			} else {
				org.apache.log4j.Logger.getLogger(Response.class).debug(
						"Response is already stored for question ID :" + q.getQuestionId() + " for employee ID : " + employeeId);
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Response.class).error("Exception while saving the response for question : " + q.getQuestionText(), e);
		}

		return responseSaved;

	}
}
