package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.icube.owen.metrics.MetricsList;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

public class EmployeeList extends TheBorg {

	/**
	 * Returns the employee smart list based on the filter objects provided for initiatives of type Team
	 * 
	 * @param filterList - list of filter objects
	 * @param initiativeType - type of the initiative
	 * @return list of employee objects
	 */
	public List<Employee> getEmployeeSmartListForTeam(List<Filter> filterList, int initiativeType) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeSmartList = new ArrayList<Employee>();
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeSmartListForTeam method started");
			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("R Path for eval " + s);
			dch.rCon.eval(s);
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
			dch.rCon.assign("Function", UtilHelper.getIntArrayFromIntegerList(funcList));
			dch.rCon.assign("Position", UtilHelper.getIntArrayFromIntegerList(posList));
			dch.rCon.assign("Zone", UtilHelper.getIntArrayFromIntegerList(zoneList));
			dch.rCon.assign("init_type_id", new int[] { initiativeType });

			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Calling the actual function in RScript TeamSmartList");
			REXP employeeSmartListForTeam = dch.rCon.parseAndEval("try(eval(TeamSmartList(Function, Position, Zone, init_type_id)))");
			if (employeeSmartListForTeam.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error: " + employeeSmartListForTeam.asString());
				throw new Exception("Error: " + employeeSmartListForTeam.asString());
			} else {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
						"Successfully retrieved Smart List for team " + employeeSmartListForTeam.asList());
			}

			RList result = employeeSmartListForTeam.asList();
			REXPInteger empIdResult = (REXPInteger) result.get("emp_id");
			int[] empIdArray = empIdResult.asIntegers();
			REXPDouble scoreResult = (REXPDouble) result.get("Score");
			int[] scoreArray = scoreResult.asIntegers();
			REXPString gradeRseult = (REXPString) result.get("flag");
			String[] gradeArray = gradeRseult.asStrings();

			for (int i = 0; i < empIdArray.length; i++) {
				Employee e = new Employee();
				e = e.get(empIdArray[i]);
				e.setGrade(gradeArray[i]);
				e.setScore(scoreArray[i]);
				employeeSmartList.add(e);
			}

			Collections.sort(employeeSmartList, Collections.reverseOrder(new Comparator<Employee>() {
				public int compare(Employee e1, Employee e2) {
					return Double.compare(e1.getScore(), e2.getScore());
				}
			}));
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error while trying to retrieve the smart list for team ", e);
		}

		return employeeSmartList;

	}

	/**
	 * Returns the employee smart list for initiatives of type Individual based on the filter objects provided
	 * @param partOfEmployeeList - List of employee objects which are part of the initiative
	 * @param initiativeType - ID of the type of initiative
	 * @return List of employee objects
	 */
	public List<Employee> getEmployeeSmartListForIndividual(List<Employee> partOfEmployeeList, int initiativeType) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> individualSmartList = new ArrayList<Employee>();
		List<Integer> partOfEmployeeIdList = new ArrayList<>();
		for (Employee e : partOfEmployeeList) {
			partOfEmployeeIdList.add(e.getEmployeeId());
		}
		try {
			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("R Path for eval " + s);
			dch.rCon.eval(s);
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Filling up parameters for rscript function");
			dch.rCon.assign("emp_id", new int[] { partOfEmployeeIdList.get(0) });
			dch.rCon.assign("init_type_id", new int[] { initiativeType });
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Calling the actual function in RScript IndividualSmartList");
			REXP employeeSmartList = dch.rCon.parseAndEval("try(eval(IndividualSmartList(emp_id, init_type_id)))");
			if (employeeSmartList.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error: " + employeeSmartList.asString());
				throw new Exception("Error: " + employeeSmartList.asString());
			} else {
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
						"Retrieval of the employee smart list completed " + employeeSmartList.asList());
			}

			RList result = employeeSmartList.asList();
			REXPInteger empIdResult = (REXPInteger) result.get("emp_id");
			int[] empIdArray = empIdResult.asIntegers();
			REXPDouble scoreResult = (REXPDouble) result.get("Score");
			int[] scoreArray = scoreResult.asIntegers();
			REXPString gradeRseult = (REXPString) result.get("flag");
			String[] gradeArray = gradeRseult.asStrings();

			for (int i = 0; i < empIdArray.length; i++) {
				Employee e = new Employee();
				e = e.get(empIdArray[i]);
				e.setGrade(gradeArray[i]);
				e.setScore(scoreArray[i]);
				individualSmartList.add(e);
			}

			Collections.sort(individualSmartList, Collections.reverseOrder(new Comparator<Employee>() {
				public int compare(Employee e1, Employee e2) {
					return Double.compare(e1.getScore(), e2.getScore());
				}
			}));
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Error while trying to retrieve the smart list for employee ", e);
		}

		return individualSmartList;
	}

	/**
	 * Get a list of all employee objects
	 * @return employeeList
	 */

	// TODO make a copy of this function to get the master list accepting the companyId as an argument
	public List<Employee> getEmployeeMasterList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeList = new ArrayList<>();
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeMasterList method started");
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getEmployeeList()}");
			ResultSet res = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + cstmt);
			while (res.next()) {
				Employee e = setEmployeeDetails(res);
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
	 * @param res -  actual result from sql
	 * @param setScore - if the score should be set for the employee or not
	 * @return employee object
	 * @throws SQLException - if employee details are not set
	 */
	protected Employee setEmployeeDetails(ResultSet res) throws SQLException {
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

		if (UtilHelper.hasColumn(res, "score") && res.getDouble("score") >= 0) {
			e.setScore(res.getDouble("score"));
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
					"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getScore());
		} else {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Employee  : " + e.getEmployeeId() + "-" + e.getFirstName());
		}
		return e;
	}

	/*
		*//**
		* Returns a list of string filter ids from a map of filters
		* 
		* @param filterMap - Map of filters
		* @return string list of filter keys
		*/
	/*
	private List<Integer> getFilterKeyList(Map<Integer, String> filterMap) {
	List<Integer> filterKeysStringList = new ArrayList<>();
	filterKeysStringList.addAll(filterMap.keySet());
	return filterKeysStringList;
	}
	*/
	/**
	 * Retrieves the employee list based on the dimension provided 
	 * 
	 * @return List of employee objects
	 */
	public List<Employee> getEmployeeListByFilters(int companyId, List<Filter> filterList) {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeList = new ArrayList<>();
		Employee e = new Employee();
		Connection conn;
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
			conn = dch.getCompanyConnection(companyId);
			CallableStatement cstmt = conn.prepareCall("{call getEmpFromDimension(?,?,?)}");
			cstmt.setInt(1, funcId);
			cstmt.setInt(2, posId);
			cstmt.setInt(3, zoneId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				employeeList.add(e.get(rs.getInt("emp_id")));
			}
		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while retrieving the employee list based on dimension", e1);
		}

		return employeeList;

	}
}
