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
	 */
	public Employee login(String userId, String password) {
		Employee e = new Employee();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Connection companySqlCon;
		// userId == email; split the email into id + company name
		// send company to sql ... receive company ID + db connection details
		// make sql connection to company db
		// return employee object(with companyId) to UI
		int index = userId.indexOf('@');
		String companyDomain = userId.substring(index + 1);
		try {
			CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyDb(?)}");
			cstmt.setString(1, companyDomain);
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			String companySqlUrl = "jdbc:mysql://" + rs.getString("sql_server") + ":3306/" + rs.getString("comp_sql_dbname") + "";
			String companyUser = rs.getString("sql_user_id");
			String companyPassword = rs.getString("sql_password");
			companySqlCon = dch.getCompanyConnection(rs.getInt("comp_id"), companySqlUrl, companyUser, companyPassword);
			CallableStatement cstmt1 = companySqlCon.prepareCall("{call verifyLogin(?,?)}");
			cstmt1.setString(1, userId);
			cstmt1.setString(2, password);
			ResultSet res = cstmt1.executeQuery();
			if (res.next()){
				e = e.get(res.getInt("emp_id"));
				e.setCompanyId(rs.getInt("comp_id"));
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Successfully validated user with userID : " + userId);
			}else {
				//TODO swarna: figure out how to send error message
				//send error message to UI
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Invalid username/password");
			}

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(Question.class).error("Exception while retrieving the company database", e1);
		}
		return e;
	}
}
