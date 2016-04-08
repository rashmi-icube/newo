package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
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
	 * @return boolean value if the response was stored successfully
	 */
	public boolean saveMeResponse(int companyId, int employeeId, Question q, int responseValue, String feedback) {
		boolean responseSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			Connection conn = dch.getCompanyConnection(companyId);
			CallableStatement cstmt = conn.prepareCall("{call insertMeResponse(?,?,?,?,?,?)}");
			cstmt.setInt(1, employeeId);
			cstmt.setInt(2, q.getQuestionId());
			cstmt.setTimestamp(3, UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt.setInt(4, responseValue);
			cstmt.setInt(5, q.getRelationshipTypeId());
			cstmt.setString(6, feedback);
			ResultSet rs = cstmt.executeQuery();
			if (rs.next()) {
				responseSaved = true;
				org.apache.log4j.Logger.getLogger(Response.class).debug("Successfully saved the response for : " + q.getQuestionText());
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Response.class).error("Exception while saving the response for question : " + q.getQuestionText(), e);
		}

		return responseSaved;
	}

	/**
	 * Saves the response for the WE question
	 * @return boolean value if the response was stored successfully
	 */
	public boolean saveWeResponse(int companyId, int employeeId, Question q, Map<Employee, Integer> employeeRating) {

		boolean responseSaved = false;
		int count = 0;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			Connection conn = dch.getCompanyConnection(companyId);
			for (Employee e : employeeRating.keySet()) {
				CallableStatement cstmt = conn.prepareCall("{call insertWeResponse(?,?,?,?,?,?)}");
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
				org.apache.log4j.Logger.getLogger(Response.class).debug("Successfully saved the response for : " + q.getQuestionText());
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Response.class).error("Exception while saving the response for question : " + q.getQuestionText(), e);
		}

		return responseSaved;

	}
}
