package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.survey.Response;
import org.restlet.data.Language;

public class EmployeeHelper {

	public BasicEmployeeDetails getBasicEmployeeDetails(int companyId, int employeeId) {
		BasicEmployeeDetails bed = new BasicEmployeeDetails();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);

		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getEmployeeBasicDetails(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				bed.setEmployeeId(employeeId);
				bed.setCompanyEmployeeId(rs.getInt("emp_int_id"));
				bed.setDob(rs.getDate("date_of_birth"));
				bed.setEmailId(rs.getString("emailId"));
				bed.setSalutation(rs.getString("salutation"));
				bed.setFirstName(rs.getString("firstName"));
				bed.setLastName(rs.getString("lastName"));
				bed.setPhone(rs.getString("phone"));
				bed.setFunction(rs.getString("function"));
				bed.setLocation(rs.getString("location"));
				bed.setDesignation(rs.getString("designation"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while getting the employee basic details", e);
		}

		return bed;

	}

	public List<WorkExperience> getWorkExperienceDetails(int companyId, int employeeId) {

		List<WorkExperience> workExList = new ArrayList<>();

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);

		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getEmployeeWorkExperience(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				WorkExperience workEx = new WorkExperience();
				workEx.setEmployeeId(employeeId);
				workEx.setWorkExperienceDetailsId(rs.getInt("id"));
				workEx.setCompanyName(rs.getString("company_name"));
				workEx.setDesignation(rs.getString("designation"));
				workEx.setStartDate(rs.getDate("start_date"));
				workEx.setLocation(rs.getString("location"));
				workEx.setEndDate(rs.getDate("end_date"));

				if (workEx.getEndDate() == null) {
					Date startDate = workEx.getStartDate();
					String duration = "";
					LocalDate today = LocalDate.now();
					Calendar cal = Calendar.getInstance();
					cal.setTime(startDate);
					LocalDate startingDate = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
					Period p = Period.between(startingDate, today);
					duration = p.getYears() + "years " + p.getMonths() + "months";
					workEx.setDuration(duration);
				} else {
					workEx.setDuration("");
				}
				workExList.add(workEx);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while getting the employee basic details", e);
		}

		return workExList;

	}

	public List<EducationDetails> getEducationDetails(int companyId, int employeeId) {

		List<EducationDetails> educationDetailsList = new ArrayList<>();

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);

		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getEmployeeWorkExperience(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				EducationDetails educationDetails = new EducationDetails();
				educationDetails.setEmployeeId(employeeId);
				educationDetails.setEducationDetailsId(rs.getInt("education_id"));
				educationDetails.setInstitution(rs.getString("institute_name"));
				educationDetails.setCertification(rs.getString("certification"));
				educationDetails.setStartDate(rs.getDate("from_date"));
				educationDetails.setEndDate(rs.getDate("to_date"));
				educationDetails.setLocation(rs.getString("location"));
				educationDetailsList.add(educationDetails);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while getting the employee basic details", e);
		}

		return educationDetailsList;

	}

	public List<Language> getLanguageMasterList(int companyId) {
		List<Language> languageList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			Connection conn = dch.getCompanyConnection(companyId);
			CallableStatement cstmt = conn.prepareCall("{call getLanguageMasterList(?)}");
			cstmt.setInt(1, companyId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Response.class).error("Exception while retrieving the ;anguage masterlist ", e);
		}
		Connection conn = dch.getCompanyConnection(companyId);
		return languageList;
	}

	// remove of all details given up
	// add of all details given up
}
