package org.icube.owen.individual;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class Login extends TheBorg {

	/**
	 * Validates user name and password for login page
	 * @param emailId - email id of the user
	 * @param password - password of the user
	 * @param ipAddress - ip address of the machine from where the user logs in
	 * @param roleId - 1/2 depending on either Individual or HR (1:Individual 2:HR)
	 * @return Employee object
	 * @throws Exception - thrown when provided with invalid credentials
	 */
	public Employee login(String emailId, String password, String ipAddress, int roleId) throws Exception {

		Employee e = new Employee();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Connection companySqlCon = null;

		int index = emailId.indexOf('@');
		String companyDomain = emailId.substring(index + 1);
		int companyId = 0;
		String companyName = "";
		try {
			CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyDb(?)}");
			cstmt.setString(1, companyDomain);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				companyId = rs.getInt("comp_id");
				dch.getCompanyConnection(companyId);
				companySqlCon = dch.companyConnectionMap.get(companyId).getSqlConnection();
				companyName = rs.getString("comp_name");
				org.apache.log4j.Logger.getLogger(Login.class).debug("Company Name : " + companyName);
			}
			org.apache.log4j.Logger.getLogger(Login.class).debug("Role ID for user : " + emailId + " is : " + roleId);
			CallableStatement cstmt1 = companySqlCon.prepareCall("{call verifyLogin(?,?,?,?,?)}");
			cstmt1.setString("loginid", emailId);
			cstmt1.setString("pass", password);
			cstmt1.setTimestamp("curr_time", UtilHelper.convertJavaDateToSqlTimestamp(Date.from(Instant.now())));
			cstmt1.setString("ip", ipAddress);
			cstmt1.setInt("roleid", roleId);
			ResultSet res = cstmt1.executeQuery();
			while (res.next()) {
				if (res.getInt("emp_id") == 0) {
					org.apache.log4j.Logger.getLogger(Login.class).error("Invalid username/password");
					throw new Exception("Invalid credentials!!!");
				} else {
					e = e.get(companyId, res.getInt("emp_id"));
					e.setCompanyId(companyId);
					e.setFirstTimeLogin(res.getBoolean("first_time_login"));
					e.setCompanyName(companyName);
					org.apache.log4j.Logger.getLogger(Login.class).debug("Successfully validated user with userID : " + emailId);

				}
			}

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(Login.class).error("Exception while retrieving the company database", e1);
		}
		return e;
	}

	/**
	 * Retrieves the role list
	 * @param companyId - ID of the company
	 * @return Map of role ID and role
	 */
	/*public Map<Integer, String> getUserRoleMap(int companyId) {
		Map<Integer, String> userRoleMap = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			CallableStatement cstmt1 = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getRoleList()}");
			ResultSet res = cstmt1.executeQuery();
			while (res.next()) {
				userRoleMap.put(res.getInt("role_id"), res.getString("role"));
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Login.class).error("Exception while retrieving the user role map", e);
		}
		return userRoleMap;
	}*/

	/**
	 * Validates the employee ID for login page
	 * @param companyId - Company ID
	 * @param employeeId - Employee ID to be validated
	 * @return true if employee is validated and false if employee is not validated
	 */
	public boolean loginIhcl(int companyId, int employeeId) {
		boolean status = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			CallableStatement cstmt2 = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call verifyLoginForIhcl(?)}");
			cstmt2.setInt("emp_id", employeeId);
			ResultSet res1 = cstmt2.executeQuery();
			res1.next();
			if (res1.getBoolean("status")) {
				status = true;
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Login.class).error("Exception while retrieving the company database", e);
		}
		return status;
	}

}
