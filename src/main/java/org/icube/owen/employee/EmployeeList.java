package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.icube.owen.metrics.MetricsList;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

public class EmployeeList extends TheBorg {

	/**
	 * Returns the employee smart list based on the filter objects provided for initiatives of type Team
	 * 
	 * @param companyId - Company ID of the employee
	 * @param filterList - list of filter objects
	 * @param initiativeType - type of the initiative
	 * @return list of employee objects
	 */

	public List<Employee> getEmployeeSmartListForTeam(int companyId, List<Filter> filterList, int initiativeType) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Employee> employeeSmartList = new ArrayList<Employee>();
		try {
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("R Connection Available : " + rCon.isConnected());
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Filling up parameters for rscript function");
			List<Integer> funcList = new ArrayList<>();
			List<Integer> posList = new ArrayList<>();
			List<Integer> zoneList = new ArrayList<>();
			for (Filter f : filterList) {
				if (f.getFilterName().equalsIgnoreCase("Function")) {
					funcList.addAll(f.getFilterValues().keySet());
				} else if (f.getFilterName().equalsIgnoreCase("Position")) {
					posList.addAll(f.getFilterValues().keySet());
				} else if (f.getFilterName().equalsIgnoreCase("Zone")) {
					zoneList.addAll(f.getFilterValues().keySet());
				}
			}
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
					"Parameters for R function :  /n Function : " + funcList.toString() + "/n Position : " + posList.toString() + " /n Zone : "
							+ zoneList.toString() + "/n Initiative Type Id : " + initiativeType);

			rCon.assign("company_id", new int[] { companyId });
			rCon.assign("Function", UtilHelper.getIntArrayFromIntegerList(funcList));
			rCon.assign("Position", UtilHelper.getIntArrayFromIntegerList(posList));
			rCon.assign("Zone", UtilHelper.getIntArrayFromIntegerList(zoneList));
			rCon.assign("init_type_id", new int[] { initiativeType });

			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Calling the actual function in RScript TeamSmartList");
			REXP employeeSmartListForTeam = rCon.parseAndEval("try(eval(TeamSmartList(company_id, Function, Position, Zone, init_type_id)))");
			if (employeeSmartListForTeam.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error: " + employeeSmartListForTeam.asString());
				dch.releaseRcon();
				throw new Exception("Error: " + employeeSmartListForTeam.asString());
			} else {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
						"Successfully retrieved Smart List for team " + employeeSmartListForTeam.asList());
			}

			RList result = employeeSmartListForTeam.asList();
			REXPInteger empIdResult = (REXPInteger) result.get("emp_id");
			int[] empIdArray = empIdResult.asIntegers();
			REXPString gradeRseult = (REXPString) result.get("flag");
			String[] gradeArray = gradeRseult.asStrings();
			REXPInteger rank = (REXPInteger) result.get("Rank");
			int[] rankArray = rank.asIntegers();
			Map<Integer, Employee> empMap = new TreeMap<>();

			for (int i = 0; i < empIdArray.length; i++) {
				Employee e = new Employee();
				e = e.get(companyId, empIdArray[i]);
				e.setGrade(gradeArray[i]);
				empMap.put(rankArray[i], e);
			}

			for (Employee e : empMap.values()) {
				employeeSmartList.add(e);
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error while trying to retrieve the smart list for team ", e);
		} finally {
			dch.releaseRcon();
		}

		return employeeSmartList;

	}

	/**
	 * Returns the employee smart list for initiatives of type Individual based on the filter objects provided
	 * @param companyId - Company ID of the employee
	 * @param partOfEmployeeList - List of employee objects which are part of the initiative
	 * @param initiativeType - ID of the type of initiative
	 * @return List of employee objects
	 */

	public List<Employee> getEmployeeSmartListForIndividual(int companyId, List<Employee> partOfEmployeeList, int initiativeType) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Employee> individualSmartList = new ArrayList<Employee>();
		List<Integer> partOfEmployeeIdList = new ArrayList<>();
		for (Employee e : partOfEmployeeList) {
			partOfEmployeeIdList.add(e.getEmployeeId());
		}
		try {
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("R Connection Available : " + rCon.isConnected());
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Filling up parameters for rscript function");
			rCon.assign("company_id", new int[] { companyId });
			rCon.assign("emp_id", new int[] { partOfEmployeeIdList.get(0) });
			rCon.assign("init_type_id", new int[] { initiativeType });
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Calling the actual function in RScript IndividualSmartList");
			REXP employeeSmartList = rCon.parseAndEval("try(eval(IndividualSmartList(company_id, emp_id, init_type_id)))");
			if (employeeSmartList.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error: " + employeeSmartList.asString());
				dch.releaseRcon();
				throw new Exception("Error: " + employeeSmartList.asString());
			} else {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
						"Retrieval of the employee smart list completed " + employeeSmartList.asList());
			}

			RList result = employeeSmartList.asList();
			REXPInteger empIdResult = (REXPInteger) result.get("emp_id");
			int[] empIdArray = empIdResult.asIntegers();
			REXPString gradeRseult = (REXPString) result.get("flag");
			String[] gradeArray = gradeRseult.asStrings();
			REXPInteger rank = (REXPInteger) result.get("Rank");
			int[] rankArray = rank.asIntegers();
			Map<Integer, Employee> empMap = new TreeMap<>();

			for (int i = 0; i < empIdArray.length; i++) {
				Employee e = new Employee();
				e = e.get(companyId, empIdArray[i]);
				e.setGrade(gradeArray[i]);
				empMap.put(rankArray[i], e);
			}

			for (Employee e : empMap.values()) {
				individualSmartList.add(e);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error while trying to retrieve the smart list for employee ", e);
		} finally {
			dch.releaseRcon();
		}

		return individualSmartList;
	}

	/**
	 * Retrieves the employee master list from the company db
	 * @param companyId - Company ID of the employee
	 * @return list of employee objects
	 */

	public List<Employee> getEmployeeMasterList(int companyId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Employee> employeeList = new ArrayList<>();
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeMasterList method started");
			CallableStatement cstmt = dch.companyConfigMap.get(companyId).getSqlConnection().prepareCall("{call getEmployeeList()}");
			ResultSet res = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + cstmt);
			while (res.next()) {
				Employee e = setEmployeeDetails(companyId, res);
				e.setCompanyId(companyId);
				employeeList.add(e);
			}

			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeList.toString());

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while getting the employee master list", e);

		}

		return employeeList;

	}

	/**
	 * Set the employee details based on the result from sql
	 * @param companyId - Company ID of the employee
	 * @param res - actual result from sql
	 * @return employee object
	 * @throws SQLException - if employee details are not set
	 */
	protected Employee setEmployeeDetails(int companyId, ResultSet res) throws SQLException {
		Employee e = new Employee();
		e.setEmployeeId(res.getInt("emp_id"));
		e.setCompanyEmployeeId(res.getString("emp_int_id"));
		e.setFirstName(res.getString("first_name"));
		e.setLastName(res.getString("last_name"));
		e.setReportingManagerId(res.getString("reporting_emp_id"));
		if (res.getString("status") != null && res.getString("status").equalsIgnoreCase("active")) {
			e.setActive(true);
		} else {
			e.setActive(false);
		}

		e.setFunction(res.getString("Function"));
		e.setPosition(res.getString("Position"));
		e.setZone(res.getString("Zone"));
		if (UtilHelper.hasColumn(res, "score") && res.getDouble("score") >= 0) {
			e.setScore(res.getDouble("score"));
		}
		e.setCompanyId(companyId);
		return e;
	}

	/**
	 * Retrieves the employee list based on the dimension provided
	 * @param companyId - List of employee objects
	 * @param filterList - List of filter objects
	 * @return List of employee objects
	 */
	public List<Employee> getEmployeeListByFilters(int companyId, List<Filter> filterList) {
		org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Company ID " + companyId);

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Employee> employeeList = new ArrayList<>();
		try {
			int funcId = 0, posId = 0, zoneId = 0;
			for (Filter filter : filterList) {
				if (filter.getFilterName().equalsIgnoreCase("Function")) {
					funcId = filter.getFilterValues().keySet().iterator().next();
				} else if (filter.getFilterName().equalsIgnoreCase("Position")) {
					posId = filter.getFilterValues().keySet().iterator().next();
				} else if (filter.getFilterName().equalsIgnoreCase("Zone")) {
					zoneId = filter.getFilterValues().keySet().iterator().next();
				}
			}
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Function : " + funcId + " Zone : " + zoneId + " Position : " + posId);
			CallableStatement cstmt = dch.companyConfigMap.get(companyId).getSqlConnection().prepareCall("{call getEmpFromDimension(?,?,?)}");
			cstmt.setInt(1, funcId);
			cstmt.setInt(2, posId);
			cstmt.setInt(3, zoneId);
			ResultSet rs = cstmt.executeQuery();
			List<Integer> employeeIdList = new ArrayList<>();
			while (rs.next()) {
				employeeIdList.add(rs.getInt("emp_id"));
			}
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Employee ID List : " + employeeIdList);
			employeeList = get(companyId, employeeIdList);
		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while retrieving the employee list based on dimension", e1);
		}

		return employeeList;

	}

	/**
	 * Returns a list employee objects based on the employee IDs given
	 * @param companyId - List of employee objects
	 * @param employeeIdList - List of IDs of the employees that need to be retrieved
	 * @return employee object list
	 */
	public List<Employee> get(int companyId, List<Integer> employeeIdList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		List<Employee> empList = new ArrayList<>();

		// sub listing the employee ID list for every 100 employees due to db constraints
		int subListSize = 100;
		int empSubListCount = ((employeeIdList.size() % subListSize) > 0) ? (employeeIdList.size() / subListSize) + 1 : employeeIdList.size()
				/ subListSize;
		int listIndex = 0;
		List<Integer> empSubList = new ArrayList<>();
		for (int i = 0; i < empSubListCount; i++) {
			if ((listIndex + subListSize) > employeeIdList.size()) {
				empSubList = employeeIdList.subList(listIndex, employeeIdList.size());
			} else {
				empSubList = employeeIdList.subList(listIndex, listIndex + subListSize);
			}

			try {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("get method started");
				CallableStatement cstmt = dch.companyConfigMap.get(companyId).getSqlConnection().prepareCall("{call getEmployeeDetails(?)}");
				cstmt.setString(1, empSubList.toString().substring(1, empSubList.toString().length() - 1).replaceAll(" ", ""));
				ResultSet res = cstmt.executeQuery();
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + cstmt);
				while (res.next()) {
					Employee e = setEmployeeDetails(companyId, res);
					org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
							"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getLastName());
					empList.add(e);
				}
				listIndex = listIndex + subListSize;
			} catch (SQLException e1) {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).error(
						"Exception while retrieving employee object with employeeIds : " + employeeIdList, e1);

			}
		}
		return empList;
	}
}
