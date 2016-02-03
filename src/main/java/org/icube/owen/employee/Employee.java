package org.icube.owen.employee;

import java.util.HashMap;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class Employee extends TheBorg {

	private int employeeId;
	private String companyEmployeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private long score;
	private boolean active;

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
	public Employee get(int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Employee e = new Employee();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String query = "match (a:Employee{emp_id:" + employeeId
					+ "}) return a.emp_id as employeeId , a.FirstName as firstName, a.LastName as LastName,"
					+ "a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();

				e.setEmployeeId(Integer.valueOf(resultMap.get("employeeId").toString()));
				e.setCompanyEmployeeId(resultMap.get("companyEmployeeId").toString());
				e.setFirstName(resultMap.get("firstName").toString());
				e.setLastName(resultMap.get("LastName").toString());
				e.setReportingManagerId(resultMap.get("reportingManagerId").toString());
				org.apache.log4j.Logger.getLogger(Employee.class).debug(
						"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getLastName());
				tx.success();
			}
		} catch (Exception e1) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while retrieving employee object with employeeId : " + employeeId,
					e1);

		}
		return e;
	}

}