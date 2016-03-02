package org.icube.owen.employee;

import java.util.Date;

public class BasicEmployeeDetails {

	private int employeeId;
	private int basicEmployeeDetailId;
	private String salutation;
	private String firstName;
	private String designation;
	private String function;
	private String location;
	private String emailId;
	private String phone;
	private Date dob;
	private int companyEmployeeId;

	public int getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(int employeeId) {
		this.employeeId = employeeId;
	}

	public int getBasicEmployeeDetailId() {
		return basicEmployeeDetailId;
	}

	public void setBasicEmployeeDetailId(int basicEmployeeDetailId) {
		this.basicEmployeeDetailId = basicEmployeeDetailId;
	}

	public String getSalutation() {
		return salutation;
	}

	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public int getCompanyEmployeeId() {
		return companyEmployeeId;
	}

	public void setCompanyEmployeeId(int companyEmployeeId) {
		this.companyEmployeeId = companyEmployeeId;
	}
}
