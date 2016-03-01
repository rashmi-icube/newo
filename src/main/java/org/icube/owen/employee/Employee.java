package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class Employee extends TheBorg {

	// TODO: retrieve all employee details from SQL
	private int employeeId;
	private String companyEmployeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private long score;
	private boolean active;
	private int companyId;

	// TODO add method to give employee image

	public int getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(int employeeId) {
		this.employeeId = employeeId;
	}

	public String getCompanyEmployeeId() {
		return companyEmployeeId;
	}

	public void setCompanyEmployeeId(String companyEmployeeId) {
		this.companyEmployeeId = companyEmployeeId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getReportingManagerId() {
		return reportingManagerId;
	}

	public void setReportingManagerId(String reportingManagerId) {
		this.reportingManagerId = reportingManagerId;
	}

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getCompanyId() {
		return companyId;
	}

	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}

	/**
	 * Returns an employee object based on the employee ID given
	 * 
	 * @param employeeId - ID of the employee that needs to be retrieved
	 * @return employee object
	 */
	public Employee get(int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		EmployeeList el = new EmployeeList();
		Employee e = new Employee();
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("get method started");
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getEmployeeDetails(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet res = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + cstmt);
			res.next();
			e = el.setEmployeeDetails(res, false);
			org.apache.log4j.Logger.getLogger(Employee.class).debug(
					"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getLastName());

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while retrieving employee object with employeeId : " + employeeId,
					e1);

		}
		return e;
	}
}