package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.survey.Response;
import org.restlet.data.Language;

public class EmployeeHelper {

	public BasicEmployeeDetails getBasicEmployeeDetails(int companyId, int employeeId) {
		return null;

	}

	public List<WorkExperience> getWorkExperienceDetails(int companyId, int employeeId) {
		return null;
	}

	public List<EducationDetails> getEducationDetails(int companyId, int employeeId) {
		return null;
	}
	
	public List<Language> getLanguageMasterList(int companyId){
		List<Language> languageList = new ArrayList<>();
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try{
			Connection conn = dch.getCompanyConnection(companyId);
			CallableStatement cstmt = conn.prepareCall("{call getLanguageMasterList(?)}");
			cstmt.setInt(1, companyId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()){
			}
		}catch (SQLException e){
			org.apache.log4j.Logger.getLogger(Response.class).error("Exception while retrieving the ;anguage masterlist ", e);
		}
		Connection conn = dch.getCompanyConnection(companyId);
		return languageList;
	}

	public List<Language> getLanguageDetails(int companyId, int employeeId) {
		
		return null;
	}

	// remove of all details given up
	// add of all details given up
}
