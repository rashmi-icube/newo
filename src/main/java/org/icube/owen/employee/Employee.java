package org.icube.owen.employee;

import java.sql.ResultSet;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class Employee extends TheBorg {

	private int employeeId;
	private String companyEmployeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private long score;
	private boolean active = true; //TODO hpatel to figure out where this field will be filled in from 

	//TODO add employeeImage
	
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

	/**
	 * Returns an employee object based on the employee ID given
	 * 
	 * @param employeeId - ID of the employee that needs to be retrieved
	 * @return employee object
	 */
	@SuppressWarnings("unchecked")
	public Employee get(int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Employee e = new Employee();
		try {
			String query = "match (e:Employee{emp_id:" + employeeId + "}) return e";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(query);

			while (res.next()) {
				Map<String, Object> resultMap = (Map<String, Object>) res.getObject("e");

				e.setEmployeeId((int) resultMap.get("emp_id"));
				e.setCompanyEmployeeId((String) resultMap.get("emp_int_id"));
				e.setFirstName(resultMap.get("FirstName").toString());
				e.setLastName(resultMap.get("LastName").toString());
				e.setReportingManagerId(resultMap.get("Reporting_emp_id").toString());
				org.apache.log4j.Logger.getLogger(Employee.class).debug(
						"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getLastName());

			}
		} catch (Exception e1) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while retrieving employee object with employeeId : " + employeeId,
					e1);

		}
		return e;
	}
}