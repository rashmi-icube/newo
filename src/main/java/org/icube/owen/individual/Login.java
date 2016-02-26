package org.icube.owen.individual;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.survey.Question;

public class Login extends TheBorg {

	/**
	 * Validates username and password for login page
	 * @param userId - email ID of the user
	 * @param password - password of the user
	 * @return Employee object
	 * @throws Exception - thrown when provided with invalid credentials
	 */
	public Employee login(String userId, String password) throws Exception {
		Employee e = new Employee();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Connection companySqlCon = null;

		int index = userId.indexOf('@');
		String companyDomain = userId.substring(index + 1);
		try {
			CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyDb(?)}");
			cstmt.setString(1, companyDomain);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				companySqlCon = dch.getCompanyConnection(rs.getInt("comp_id"));
			}

			CallableStatement cstmt1 = companySqlCon.prepareCall("{call verifyLogin(?,?)}");
			cstmt1.setString(1, userId);
			cstmt1.setString(2, password);
			ResultSet res = cstmt1.executeQuery();
			if (res.next()) {
				e = e.get(res.getInt("emp_id"));
				e.setCompanyId(rs.getInt("comp_id"));
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully validated user with userID : " + userId);
			} else {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("Invalid username/password");
				throw new Exception("Invalid credentials!!!");
			}

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving the company database", e1);
		}
		return e;
	}
}
