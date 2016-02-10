package org.icube.owen.employee;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class EmployeeList extends TheBorg {

	/**
	 * Returns the employee smart list based on the filter objects provided for initiatives of type Team
	 * 
	 * @param filterList - list of filter objects
	 * @param initiativeType - type of the initiative
	 * @return list of employee objects
	 */
	@SuppressWarnings("unchecked")
	public List<Employee> getEmployeeSmartListForTeam(List<Filter> filterList, int initiativeType) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeSmartList = new ArrayList<Employee>();

		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeSmartListForTeam method started");

			Map<String, Object> params = new HashMap<>();

			for (int i = 0; i < filterList.size(); i++) {
				Filter f = filterList.get(i);
				params.put(f.getFilterName(), getFilterKeyList(f.getFilterValues()));
			}

			// TODO make this dynamic based on filter list
			String funcQuery = "", posQuery = "", zoneQuery = "", relation = "";
			ArrayList<String> funcParam = (ArrayList<String>) params.get("Function");
			ArrayList<String> zoneParam = (ArrayList<String>) params.get("Zone");
			ArrayList<String> posParam = (ArrayList<String>) params.get("Position");

			if (funcParam.contains("all") || funcParam.contains("All")) {
				funcQuery = "";
			} else {
				funcQuery = "f.Id in " + funcParam.toString();

			}

			if (zoneParam.contains("all") || zoneParam.contains("All")) {
				zoneQuery = "";
			} else {
				zoneQuery = "z.Id in " + zoneParam.toString();
			}

			if (posParam.contains("all") || posParam.contains("All")) {
				posQuery = "";
			} else {
				posQuery = "p.Id in " + posParam.toString();
			}
			switch (initiativeType) {
			case 6:
				relation = "learning";
				break;
			case 7:
				relation = "social";
				break;
			case 8:
				relation = "learning|social|innovation|mentor";
				break;
			case 9:
				relation = "innovation";
				break;
			case 10:
				relation = "mentor";
				break;
			}

			String query = "match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),(z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]"
					+ "->(f:Function),a-[:is_positioned]->(p:Position)<-[:is_positioned]-b"
					+ ((!zoneQuery.isEmpty() || !funcQuery.isEmpty() || !posQuery.isEmpty()) ? " where " : "")
					+ (zoneQuery.isEmpty() ? "" : (zoneQuery + ((!funcQuery.isEmpty() || !posQuery.isEmpty() ? " and " : ""))))
					+ (funcQuery.isEmpty() ? "" : funcQuery + (!posQuery.isEmpty() ? " and " : ""))
					+ (posQuery.isEmpty() ? "" : (posQuery))
					+ " with a,b,count(a)"
					+ "as TotalPeople optional match a<-[r:"
					+ relation
					+ "]-b return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName,"
					+ "a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId, count(r) as score";

			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(query);
			while (res.next()) {
				Employee e = setEmployeeDetails(res, true);
				employeeSmartList.add(e);
			}

			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeSmartList.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while getting the employeeSmartList", e);

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
		List<Employee> employeeSmartList = new ArrayList<Employee>();
		List<Integer> employeeIdList = new ArrayList<Integer>();
		List<Integer> partOfEmployeeIdList = new ArrayList<>();
		String relation = "";
		for (Employee e : partOfEmployeeList) {
			partOfEmployeeIdList.add(e.getEmployeeId());
		}
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getListColleague(?)}");
			cstmt.setString(1, partOfEmployeeIdList.toString().substring(1, partOfEmployeeIdList.toString().length() - 1));
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				employeeIdList.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		switch (initiativeType) {
		case 1:
			relation = "learning";
			break;
		case 2:
			relation = "social";
			break;
		case 3:
			relation = "learning|social|innovation|mentor";
			break;
		case 4:
			relation = "innovation";
			break;
		case 5:
			relation = "mentor";
			break;
		}
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeSmartListForIndividual method started");
			String query = "match (a:Employee)<-[r:"
					+ relation
					+ "]-(b:Employee) where a.emp_id in"
					+ employeeIdList
					+ " and b.emp_id in"
					+ employeeIdList
					+ " return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName, a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId, count(r) as score";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(query);
			while (res.next()) {

				Employee e = setEmployeeDetails(res, true);
				employeeSmartList.add(e);
			}

			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeSmartList.toString());

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while getting the employeeSmartList", e);

		}
		return employeeSmartList;
	}

	/**
	 * Get a list of all employee objects
	 * @return employeeList
	 */
	public List<Employee> getEmployeeMasterList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeList = new ArrayList<>();
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeMasterList method started");
			String query = "match (a:Employee) return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName, "
					+ "a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(query);
			while (res.next()) {
				Employee e = setEmployeeDetails(res, false);
				employeeList.add(e);
			}

			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeList.toString());

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while getting the employee master list", e);

		}

		return employeeList;

	}

	/**
	 * Set the employee details based on the result from the cypher query
	 * 
	 * @param resultMap actual result from cypher
	 * @param setScore if the score should be set for the employee or not
	 * @return employee object
	 */
	protected Employee setEmployeeDetails(ResultSet res, boolean setScore) throws SQLException {
		Employee e = new Employee();
		e.setEmployeeId(res.getInt("employeeId"));
		e.setCompanyEmployeeId(res.getString("companyEmployeeId"));
		e.setFirstName(res.getString("firstName"));
		e.setLastName(res.getString("lastName"));
		e.setReportingManagerId(res.getString("reportingManagerId"));
		if (setScore) {
			e.setScore(res.getLong("score"));
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
					"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getScore());
		} else {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Employee  : " + e.getEmployeeId() + "-" + e.getFirstName());
		}
		return e;
	}

	/**
	 * Returns a list of string filter ids from a map of filters
	 * 
	 * @param filterMap - Map of filters
	 * @return string list of filter keys
	 */
	private List<Integer> getFilterKeyList(Map<Integer, String> filterMap) {
		List<Integer> filterKeysStringList = new ArrayList<>();
		filterKeysStringList.addAll(filterMap.keySet());
		return filterKeysStringList;
	}

}
