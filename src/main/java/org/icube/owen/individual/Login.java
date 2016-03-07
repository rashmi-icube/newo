package org.icube.owen.individual;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class Login extends TheBorg {

	/**
	 * Validates username and password for login page
	 * @param emailId - email ID of the user
	 * @param password - password of the user
	 * @return Employee object
	 * @throws Exception - thrown when provided with invalid credentials
	 */
	public Employee login(String emailId, String password) throws Exception {
		Employee e = new Employee();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Connection companySqlCon = null;

		int index = emailId.indexOf('@');
		String companyDomain = emailId.substring(index + 1);
		int companyId = 0;
		try {
			CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyDb(?)}");
			cstmt.setString(1, companyDomain);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				companyId = rs.getInt("comp_id");
				companySqlCon = dch.getCompanyConnection(companyId);
			}

			CallableStatement cstmt1 = companySqlCon.prepareCall("{call verifyLogin(?,?)}");
			cstmt1.setString(1, emailId);
			cstmt1.setString(2, password);
			ResultSet res = cstmt1.executeQuery();
			if (res.next()) {
				e = e.get(res.getInt("emp_id"));
				e.setCompanyId(companyId);
				org.apache.log4j.Logger.getLogger(Login.class).debug("Successfully validated user with userID : " + emailId);
			} else {
				org.apache.log4j.Logger.getLogger(Login.class).error("Invalid username/password");
				throw new Exception("Invalid credentials!!!");
			}

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(Login.class).error("Exception while retrieving the company database", e1);
		}
		return e;
	}
}
