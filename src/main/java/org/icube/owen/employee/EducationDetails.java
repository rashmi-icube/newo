package org.icube.owen.employee;

import java.util.Date;

public class EducationDetails {

	private int employeeId;
	private int educationDetailsId;
	private String institution;
	private String certification;
	private Date startDate;
	private Date endDate;
	private String location;

	public int getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(int employeeId) {
		this.employeeId = employeeId;
	}

	public int getEducationDetailsId() {
		return educationDetailsId;
	}

	public void setEducationDetailsId(int educationDetailsId) {
		this.educationDetailsId = educationDetailsId;
	}

	public String getInstitution() {
		return institution;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public String getCertification() {
		return certification;
	}

	public void setCertification(String certification) {
		this.certification = certification;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
