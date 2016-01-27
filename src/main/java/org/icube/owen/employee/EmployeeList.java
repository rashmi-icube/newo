package org.icube.owen.employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class EmployeeList extends TheBorg {

	/**
	 * Returns the employee smart list based on the filter objects provided
	 * 
	 * @param params
	 * list of filter objects
	 * @return list of employee objects
	 */
	public List<Employee> getEmployeeSmartList(List<Filter> filterList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeSmartList = new ArrayList<Employee>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeSmartList method started");

			Map<String, Object> params = new HashMap<>();

			for (int i = 0; i < filterList.size(); i++) {
				Filter f = filterList.get(i);
				params.put(f.getFilterName(), getFilterKeyList(f.getFilterValues()));
			}
			
			//TODO make this dynamic based on filter list 
			String funcQuery = "", posQuery = "", zoneQuery = "";
			ArrayList<String> funcParam = (ArrayList<String>) params.get("Function");
			ArrayList<String> zoneParam = (ArrayList<String>) params.get("Zone");
			ArrayList<String> posParam = (ArrayList<String>) params.get("Position");

			if (funcParam.contains("all") || funcParam.contains("All")) {
				funcQuery = "";
			} else {
				funcQuery = "f.Id in {Function}";
			}

			if (zoneParam.contains("all") || zoneParam.contains("All")) {
				zoneQuery = "";
			} else {
				zoneQuery = "z.Id in {Zone}";
			}

			if (posParam.contains("all") || posParam.contains("All")) {
				posQuery = "";
			} else {
				posQuery = "p.Id in " + "{Position}";
			}

			String query = "match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),(z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]"
					+ "->(f:Function),a-[:is_positioned]->(p:Position)<-[:is_positioned]-b"
					+ ((!zoneQuery.isEmpty() || !funcQuery.isEmpty() || !posQuery.isEmpty()) ? " where " : "")
					+ (zoneQuery.isEmpty() ? "" : (zoneQuery + ((!funcQuery.isEmpty() || !posQuery.isEmpty() ? " and " : ""))))
					+ (funcQuery.isEmpty() ? "" : funcQuery + (!posQuery.isEmpty() ? " and " : ""))
					+ (posQuery.isEmpty() ? "" : (posQuery))
					+ " with a,b,count(a)"
					+ "as TotalPeople optional match a<-[r:support]-b return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName,"
					+ "a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId, count(r) as Score";

			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query, params);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				Employee e = setEmployeeDetails(resultMap, true);
				employeeSmartList.add(e);
			}
			tx.success();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeSmartList.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while getting the employeeSmartList", e);

		}
		return employeeSmartList;

	}

	/**
	 * Get a list of all employee objects
	 * 
	 * @return employeeList
	 */
	public List<Employee> getEmployeeMasterList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeList = new ArrayList<>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeMasterList method started");
			String query = "match (a:Employee) return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				Employee e = setEmployeeDetails(resultMap, false);
				employeeList.add(e);
			}
			tx.success();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeList.toString());

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while getting the employee master list", e);

		}

		return employeeList;

	}

	/**
	 * Set the employee details based on the result from the cypher query
	 * 
	 * @param resultMap
	 * actual result from cypher
	 * @param setScore
	 * if the score should be set for the employee or not
	 * @return employee object
	 */
	protected Employee setEmployeeDetails(Map<String, Object> resultMap, boolean setScore) {
		Employee e = new Employee();
		e.setEmployeeId(resultMap.get("employeeId").toString());
		e.setFirstName(resultMap.get("firstName").toString());
		e.setLastName(resultMap.get("lastName").toString());
		e.setReportingManagerId(resultMap.get("reportingManagerId").toString());
		e.setCompanyEmployeeId(resultMap.get("companyEmployeeId").toString());
		if (setScore) {
			e.setScore((Long) resultMap.get("Score"));
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
					"Employee  : " + "-" + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getScore());
		} else {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug(
					"Employee  : " + "-" + e.getEmployeeId() + "-" + e.getFirstName());
		}
		return e;
	}

	/**
	 * Returns a list of string filter ids from a map of filters
	 * 
	 * @param filterMap
	 * @return string list of filter keys
	 */
	private List<String> getFilterKeyList(Map<String, String> filterMap) {
		List<String> filterKeysStringList = new ArrayList<>();
		filterKeysStringList.addAll(filterMap.keySet());
		return filterKeysStringList;
	}
}
