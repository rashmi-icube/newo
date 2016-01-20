package org.icube.owen.employee;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class Employee extends TheBorg {

	static Logger logger = ObjectFactory.getLogger("org.icube.owen.employee.Employee");
	private String internalId; // actual neoId //TODO if the node is deleted from neo4j the neoId too will be reused
	private String employeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private long score;
	private boolean active;

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
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
	 * @param employeeId
	 * @return employee object
	 */
	public Employee get(String employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Employee e = new Employee();
		try (Transaction tx = dch.graphDb.beginTx()) {
			Map<String, Object> params = new HashMap<>();
			params.put("employeeId", employeeId);
			String query = "match (a:Employee) where a.EmpID = {employeeId} return id(a) as neoId, a.EmpID as employeeId , a.Name as firstName";
			logger.debug("query : " + query);
			Result res = dch.graphDb.execute(query, params);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				// TODO replace the employee setters once db change is made
				/*
				 * e.setInternalId(resultMap.get("neoId").toString());
				 * e.setEmployeeId(empId);
				 * e.setFirstName(resultMap.get("firstName").toString());
				 * e.setLastName("");
				 * e.setReportingManagerId(resultMap.get("reportingManagerId"
				 * ).toString());
				 */

				e.setInternalId(resultMap.get("neoId").toString());
				e.setEmployeeId(resultMap.get("employeeId").toString());
				e.setFirstName(resultMap.get("firstName").toString());
				org.apache.log4j.Logger.getLogger(Employee.class).debug(
						"Employee  : " + e.getInternalId() + "-" + e.getEmployeeId() + "-" + e.getFirstName());
				tx.success();
			}
		} catch (Exception e1) {
			logger.error("Exception while retrieving employee object with employeeId : " + employeeId, e1);

		}
		return e;
	}
}