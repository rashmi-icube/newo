package owen.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * Populates the filter criteria for the initiative
 *
 */
public class InitiativeReadHelper {
	
	static {
		//PropertyConfigurator.configure(InitiativeReadHelper.class.getResource("resources/log4j.properties"));
		PropertyConfigurator.configure("resources/log4j.properties");
	}
	
	static DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
	
	public static void main(String[] args) {
		try (Transaction tx = dch.graphDb.beginTx()) {
			String filter = "Employee";
			InitiativeReadHelper irh = new InitiativeReadHelper();
			irh.getMasterFilterList(filter);
			tx.success();
		}

		dch.shutDown();
	}
	
	/**
	 * Retrieves all the objects which belong to a particular filter item
	 * 
	 * @param filterName - Name of the filter that needs to be populated [Function, Zone, Position]
	 * @return returns the list based on filter name
	 */

	public List<Map<String, String>> getMasterFilterList(String filterName) {
		org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("filterName : " + filterName);
		List<Map<String, String>> filterMapList = new ArrayList<Map<String, String>>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("getMasterFilterList method started");
			String query = "match (n:<<filterName>>) return n.Name as Name,n.Id as Id";
			query = query.replace("<<filterName>>", filterName);
			org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, String> filterRowMap = new HashMap<String, String>();
				Map<String, Object> resultMap = res.next();
				filterRowMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
				org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("filterRowMap : " + filterRowMap.toString());
				filterMapList.add(filterRowMap);
			}
			tx.success();
			org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("filterMapList : " + filterMapList.toString());
			return filterMapList;
		}
	}
	
	public List<Map<String, String>> getEmployeeMasterList() {
		List<Map<String, String>> employeeMapList = new ArrayList<Map<String, String>>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("getEmployeeMasterList method started");
			String query = "match (a:Employee) return a.EmpID,a.Name";
			org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("query : " + query);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, String> employeeMap = new HashMap<String, String>();
				Map<String, Object> resultMap = res.next();
				employeeMap.put(resultMap.get("EmpID").toString(), resultMap.get("Name").toString());
				org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("employeeMap : " + employeeMap.toString());
				employeeMapList.add(employeeMap);
			}
			tx.success();
			org.apache.log4j.Logger.getLogger(InitiativeReadHelper.class).debug("filterMapList : " + employeeMapList.toString());
			return employeeMapList;
		}
	}
}
