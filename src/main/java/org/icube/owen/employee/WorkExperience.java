package org.icube.owen.employee;

import java.util.Date;

public class WorkExperience {

	private int employeeId;
	private int workExperienceDetailsId;
	private String companyName;
	private String designation;
	private Date startDate;
	private Date endDate;
	private String duration;
	private String location;

	public int getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(int employeeId) {
		this.employeeId = employeeId;
	}

	public int getWorkExperienceDetailsId() {
		return workExperienceDetailsId;
	}

	public void setWorkExperienceDetailsId(int workExperienceDetailsId) {
		this.workExperienceDetailsId = workExperienceDetailsId;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
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

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
