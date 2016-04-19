package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class EmployeeHelper extends TheBorg {

	/**
	 * Retrieves the basic details of an employee
	 * @param companyId - companyId
	 * @param employeeId - employeeId of the given employee
	 * @return BasicEmployeeDetails of the given employee
	 */
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
				bed.setDob(rs.getDate("dob"));
				bed.setEmailId(rs.getString("login_id"));
				bed.setSalutation(rs.getString("salutation"));
				bed.setFirstName(rs.getString("first_name"));
				bed.setLastName(rs.getString("last_name"));
				bed.setPhone(rs.getString("phone_no"));
				bed.setFunction(rs.getString("Function"));
				bed.setLocation(rs.getString("Zone"));
				bed.setDesignation(rs.getString("Position"));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while getting the employee basic details", e);
		}

		return bed;

	}

	/**
	 * Retrieves the list of work experiences of a given employee
	 * @param companyId - companyId
	 * @param employeeId - employeeId of the given employee
	 */
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
				workEx.setWorkExperienceDetailsId(rs.getInt("work_experience_id"));
				workEx.setCompanyName(rs.getString("organization_name"));
				workEx.setDesignation(rs.getString("position"));
				workEx.setStartDate(rs.getDate("from_date"));
				workEx.setLocation(rs.getString("location"));
				workEx.setEndDate(rs.getDate("to_date"));

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

	/**
	 * Returns a list of education details for a given employee
	 */
	public List<EducationDetails> getEducationDetails(int companyId, int employeeId) {

		List<EducationDetails> educationDetailsList = new ArrayList<>();

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);

		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getEmployeeEducation(?)}");
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

	/**
	 * Retrieves the list of languages spoken by a given employee
	 */
	public List<LanguageDetails> getEmployeeLanguageDetails(int companyId, int employeeId) {
		List<LanguageDetails> languageDetailsList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);

		try {
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getEmployeeLanguage(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				LanguageDetails languageDetails = new LanguageDetails();
				languageDetails.setEmployeeId(employeeId);
				languageDetails.setLanguageDetailsId(rs.getInt("employee_language_id"));
				languageDetails.setLanguageName(rs.getString("language_name"));
				languageDetails.setLanguageId(rs.getInt("language_id"));
				languageDetailsList.add(languageDetails);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while getting the employee basic details", e);
		}

		return languageDetailsList;

	}

	/**
	 * Returns a map of languages that can be spoken by any employee
	 */
	public Map<Integer, String> getLanguageMasterMap(int companyId) {
		Map<Integer, String> languageMasterMap = new HashMap<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {

			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call getLanguageList}");

			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				languageMasterMap.put(rs.getInt("language_id"), rs.getString("language_name"));

			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while retrieving the language master map ", e);
		}

		return languageMasterMap;
	}

	/**
	 * Removes a Work Experience
	 * @param companyId - Company Id of the employee
	 * @param workExperienceId - Work experience Id to be deleted
	 * @return true/false
	 */
	public boolean removeWorkExperience(int companyId, int workExperienceId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		boolean status = false;
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Removing work experience details with Id" + workExperienceId);
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call removeWorkExperience(?)}");
			cstmt.setInt(1, workExperienceId);
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug(
						"Successfully removed work experience details with Id" + workExperienceId);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error(
					"Exception while removing Work Experience details with Id" + workExperienceId, e);
		}

		return status;
	}

	/**
	 * Removes an Education 
	 * @param companyId - Company Id of the employee
	 * @param educationId - Work experience Id to be deleted
	 * @return true/false
	 */
	public boolean removeEducation(int companyId, int educationId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		boolean status = false;
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Removing Education details with Id" + educationId);
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call removeEducation(?)}");
			cstmt.setInt(1, educationId);
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Successfully removed work experience details with Id" + educationId);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while removing Education details with Id" + educationId, e);
		}

		return status;
	}

	/**
	 * Removes a Language
	 * @param companyId - Company Id of the employee
	 * @param languageId - Work experience Id to be deleted
	 * @return true/false
	 */
	public boolean removeLanguage(int companyId, int languageId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		boolean status = false;
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Removing Language details with Id" + languageId);
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call removeLanguage(?)}");
			cstmt.setInt(1, languageId);
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Successfully removed Language details with Id" + languageId);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error("Exception while removing Language details with Id" + languageId, e);
		}

		return status;
	}

	public boolean updateBasicDetails(int companyId, BasicEmployeeDetails bed) {
		boolean status = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Updating employee basic details" + bed.getEmployeeId());
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call updateEmployeeBasicDetails(?, ?)}");
			cstmt.setInt("empid", bed.getEmployeeId());
			cstmt.setString("phoneno", bed.getPhone());
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug(
						"Successfully updated employee basic details for employeeId : " + bed.getEmployeeId());
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error(
					"Exception while updating employee basic details for employeeId : " + bed.getEmployeeId());
		}

		return status;
	}

	/**
	 * Add a new work experience to the given employee
	 */
	public boolean addWorkExperience(int companyId, WorkExperience wek) {
		boolean status = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Adding new work experience for employee" + wek.getEmployeeId());
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call insertWorkExperience(?, ?, ?, ?, ?, ?)}");
			cstmt.setInt("emp_id_ip", wek.getEmployeeId());
			cstmt.setString("organization_name_ip", wek.getCompanyName());
			cstmt.setString("position_ip", wek.getDesignation());
			cstmt.setDate("from_date_ip", UtilHelper.convertJavaDateToSqlDate(wek.getStartDate()));
			cstmt.setDate("to_date_ip", wek.getEndDate() == null ? null : UtilHelper.convertJavaDateToSqlDate(wek.getEndDate()));
			cstmt.setString("location_ip", wek.getLocation());
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug(
						"Successfully added a new work experience for employeeId : " + wek.getEmployeeId());
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error(
					"Exception while adding a new work experience for employeeId : " + wek.getEmployeeId());
		}

		return status;
	}

	/**
	 * Add a new education to the given employee
	 */
	public boolean addEducation(int companyId, EducationDetails ed) {
		boolean status = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Adding new education for employee" + ed.getEmployeeId());
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call insertEducation(?, ?, ?, ?, ?, ?)}");
			cstmt.setInt("emp_id_ip", ed.getEmployeeId());
			cstmt.setString("institute_name_ip", ed.getInstitution());
			cstmt.setString("certification_ip", ed.getCertification());
			cstmt.setDate("from_date_ip", UtilHelper.convertJavaDateToSqlDate(ed.getStartDate()));
			cstmt.setDate("to_date_ip", UtilHelper.convertJavaDateToSqlDate(ed.getEndDate()));
			cstmt.setString("location_ip", ed.getLocation());
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug(
						"Successfully added a new education for employeeId : " + ed.getEmployeeId());
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error(
					"Exception while adding a new education for employeeId : " + ed.getEmployeeId());
		}

		return status;
	}

	/**
	 * Add a new language to the given employee
	 */
	public boolean addLanguage(int companyId, LanguageDetails ld) {
		boolean status = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		try {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug("Adding new language for employee" + ld.getEmployeeId());
			CallableStatement cstmt = dch.companySqlConnectionPool.get(companyId).prepareCall("{call insertLanguage(?, ?)}");
			cstmt.setInt("emp_id_ip", ld.getEmployeeId());
			cstmt.setInt("language_id_ip", ld.getLanguageId());
			ResultSet rs = cstmt.executeQuery();
			rs.next();
			if (rs.getBoolean(1)) {
				status = true;
				org.apache.log4j.Logger.getLogger(EmployeeHelper.class).debug(
						"Successfully added a new language for employeeId : " + ld.getEmployeeId());
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeHelper.class).error(
					"Exception while adding a new language for employeeId : " + ld.getEmployeeId());
		}

		return status;
	}
}
