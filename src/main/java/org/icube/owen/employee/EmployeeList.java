package org.icube.owen.employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class EmployeeList extends TheBorg{
	
	
	
	/**
	 * Returns the employee smart list based on the function, zone and position selected
	 * @param params Map of function, zone, positions
	 * @return list of employee objects
	 */
	public List<Employee> getEmployeeList(Map<String, Object> params) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Employee> employeeList = new ArrayList<Employee>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeSmartList method started");
			
			String funcQuery = "", posQuery = "", zoneQuery = "";
			ArrayList<String> funcParam = (ArrayList<String>)params.get("funcList");
			ArrayList<String> zoneParam = (ArrayList<String>)params.get("zoneList");
			ArrayList<String> posParam = (ArrayList<String>)params.get("posList");
			if (funcParam.contains("all")) {
				funcQuery = "";
			} else {
				funcQuery = "f.Id in {funcList}";
			}
			
			if(zoneParam.contains("all")){
				zoneQuery = "";
			}else{
				zoneQuery = "z.Id in {zoneList}";
			}
			
			if(posParam.contains("all")){
				posQuery = "";
			} else {
				posQuery = "p.Id in {posList}";	
			}

			String query = "match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),(z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]"
					+ "->(f:Function),a-[:is_positioned]->(p:Position)<-[:is_positioned]-b"
					+ ((!zoneQuery.isEmpty() || !funcQuery.isEmpty() || !posQuery.isEmpty())? "where" : "" )
					+ (zoneQuery.isEmpty() ? "" : (zoneQuery + ((!funcQuery.isEmpty() || !posQuery.isEmpty() ? " and " : ""))))
					+ (funcQuery.isEmpty() ? "" : funcQuery+ (!posQuery.isEmpty() ? " and " : ""))
					+ (posQuery.isEmpty() ? "" : (posQuery))
					+ " with a,b,count(a)"
					+ "as TotalPeople optional match a<-[r:support]-b return a.EmpID as EmpId,a.Name as Name ,count(r) as Score";

			
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				String empId = resultMap.get("EmpId").toString();
				Employee e = new Employee();
				/*e.setInternalId(resultMap.get("neoId").toString());
				e.setEmployeeId(empId);
				e.setFirstName(resultMap.get("firstName").toString());
				e.setLastName("");
				e.setReportingManagerId(resultMap.get("reportingManagerId").toString());
				e.setScore((Long)resultMap.get("Score"));*/
				
				e.setEmployeeId(empId);
				e.setFirstName(resultMap.get("Name").toString());
				e.setScore((Long) resultMap.get("Score"));
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getScore());
				employeeList.add(e);
			}
			tx.success();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeList : " + employeeList.toString());
			return employeeList;
		}
	}
	
	
	public List<Map<String, String>> getEmployeeMasterList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Map<String, String>> employeeMapList = new ArrayList<Map<String, String>>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("getEmployeeMasterList method started");
			String query = "match (a:Employee) return a.EmpID,a.Name";
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, String> employeeMap = new HashMap<String, String>();
				Map<String, Object> resultMap = res.next();
				employeeMap.put(resultMap.get("EmpID").toString(), resultMap.get("Name").toString());
				org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("employeeMap : " + employeeMap.toString());
				employeeMapList.add(employeeMap);
			}
			tx.success();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("filterMapList : " + employeeMapList.toString());
			return employeeMapList;
		}
	}

}
