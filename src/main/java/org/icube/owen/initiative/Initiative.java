package org.icube.owen.initiative;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class Initiative extends TheBorg{

	private int initiativeId;
	private String initiativeName = "";
	private String initiativeType = "";
	private String initiativeStartDate = "";
	private String initiativeEndDate = "";
	private String initiativeComment = "";
	private ArrayList<String> funcList;
	private ArrayList<String> zoneList;
	private ArrayList<String> posList;
	private ArrayList<String> empIdList;
	
	public static void main(String arg[]){
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		Initiative initiative = new Initiative();
		ArrayList<String> funcList = new ArrayList<String>();
		ArrayList<String> zoneList = new ArrayList<String>();
		ArrayList<String> posList = new ArrayList<String>();
		//zoneList.add("Z1");
		//zoneList.add("Z2");
		zoneList.add("all");
		//posList.add("P1");
		//posList.add("P2");
		posList.add("all");
		//funcList.add("F1");
		//funcList.add("F2");
		funcList.add("all");
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("zoneList", zoneList);
		params.put("funcList", funcList);
		params.put("posList", posList);
		
		EmployeeList el = new EmployeeList();
		List<Employee> employeeList = el.getEmployeeList(params);
		ArrayList<String> empIdList = new ArrayList<>();
		for(Employee e : employeeList){
			empIdList.add(e.getEmployeeId());
		}
		
		initiative.setInitiativeProperties("Initiative38", "Change Process", "01/01/2015", "01/05/2015", "xyz", funcList, zoneList, posList, empIdList);
		
		try (Transaction tx = dch.graphDb.beginTx()) {

			
			int initiativeId = initiative.create();
			System.out.println("Created initiative with ID : " + initiativeId);
			if (initiativeId > 0) {
				params.put("initiativeId", initiativeId);
				if(initiative.setPartOf(params)){
					org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting part of initiative");
				} 

				params.clear();
				params.put("initiativeId", initiativeId);
				params.put("empIdList", empIdList);
				if(initiative.setOwner(params)){
					org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting owner for initiative");
				}	
			}
			
			initiative.get(5);

			tx.success();
		}
		dch.shutDown();
	
	}
	
	public void setInitiativeProperties(String initiativeName, String initiativeType, String initiativeStartDate, String initiativeEndDate,
			String initiativeComment, ArrayList<String> funcList, ArrayList<String> zoneList, ArrayList<String> posList, ArrayList<String> empIdList) {
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Setting initiative properties");
		this.initiativeName = initiativeName;
		this.initiativeType = initiativeType;
		this.initiativeStartDate = initiativeStartDate;
		this.initiativeEndDate = initiativeEndDate;
		this.initiativeComment = initiativeComment;
		this.funcList = funcList;
		this.zoneList = zoneList;
		this.posList = posList;
		this.empIdList = empIdList;

	}
	
	/**
	 * Creation of the actual initiative happens here
	 * 
	 * @return - Initiative id of the newly created initiative
	 */
	 public int create() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		String initiativeId = "";
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating the initiative");
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			Date startDate = sdf.parse(initiativeStartDate);
			Date endDate = sdf.parse(initiativeEndDate);
			String createInitQuery = "match (i:Init)  with CASE count(i) WHEN 0  THEN 1 ELSE max(i.Id)+1 END as uid " +
					"CREATE (i:Init {Id:uid,Name:'<<InitName>>',Type:'<<InitType>>',StartDate:'<<StartDate>>',EndDate:'<<EndDate>>',Comment:'<<Comment>>'}) return i.Id as Id";
			createInitQuery = createInitQuery.replace("<<InitName>>", initiativeName);
			createInitQuery = createInitQuery.replace("<<InitType>>", initiativeType);
			createInitQuery = createInitQuery.replace("<<StartDate>>", startDate.toString());
			createInitQuery = createInitQuery.replace("<<EndDate>>", endDate.toString());
			createInitQuery = createInitQuery.replace("<<Comment>>", initiativeComment);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Create initiative query : " + createInitQuery);
			Result res = dch.graphDb.execute(createInitQuery);
			Iterator it = res.columnAs("Id");
			while (it.hasNext()){
				initiativeId = it.next().toString();
			}

			tx.success();

		} catch (ParseException e) {

			e.printStackTrace();
		}
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Initiative ID : " + initiativeId);
		return Integer.parseInt(initiativeId);
	}

	/**
	 * Creates the connections with the objects that are part of the initiative
	 * 
	 * @param params - Map of the objects that are part of the initiative taken as input from the user
	 */
	public boolean setPartOf(Map<String, Object> params) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Create Initiative Connections " + params.get("initiativeId"));
			String funcQuery = "", posQuery = "", zoneQuery = "";
			ArrayList<String> funcParam = (ArrayList<String>)params.get("funcList");
			ArrayList<String> zoneParam = (ArrayList<String>)params.get("zoneList");
			ArrayList<String> posParam = (ArrayList<String>)params.get("posList");
			if (funcParam.contains("all")) {
				funcQuery = "Match (i:Init),(f:Function) WHERE i.Id = {initiativeId} Create f-[:part_of]->i ";
			} else {
				funcQuery = "Match (i:Init),(f:Function) where i.Id = {initiativeId} and f.Id in {funcList} Create f-[:part_of]->i ";
			}
			
			if(zoneParam.contains("all")){
				zoneQuery = "Match (i:Init),(z:Zone) where i.Id = {initiativeId} create z-[:part_of]->i";
			}else{
				zoneQuery = "Match (i:Init),(z:Zone) where i.Id = {initiativeId} and z.Id in {zoneList} create z-[:part_of]->i";
				
			}
			
			if(posParam.contains("all")){
				posQuery = "Match (i:Init),(p:Position) where i.Id = {initiativeId} Create p-[:part_of]->i";
			} else {
				posQuery = "Match (i:Init),(p:Position) where i.Id = {initiativeId} and p.Id in {posList} Create p-[:part_of]->i";	
			}
			
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Function query : " + funcQuery);
			dch.graphDb.execute(funcQuery, params);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Position query : " + posQuery);
			dch.graphDb.execute(posQuery, params);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Zone query : " + zoneQuery);
			dch.graphDb.execute(zoneQuery, params);
			tx.success();
			return true;
		} catch (Exception e){
			return false;
		}

	}

	/**
	 * Creates the connections with the employees who are owners of the initiative
	 * 
	 * @param params - Map of the employee id's who would be the owner of the initiative taken as input from the user
	 */
	public boolean setOwner(Map<String, Object> params) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating connections for initiative : " + params.get("initiativeId"));
			String query = "Match (i:Init),(e:Employee) where i.Id = {initiativeId} and e.EmpID in {empIdList} Create e-[:owner_of]->i";
			dch.graphDb.execute(query, params);
			tx.success();
			return true;
		} catch (Exception e){
			return false;
		}
	}
	
	public static Initiative get(int initiativeId)
	{
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Initiative i = new Initiative();
		i.setInitiativeId(initiativeId);
		Map<String, Object> params = new HashMap<>();
		params.put("initiativeId", initiativeId);
		try (Transaction tx = dch.graphDb.beginTx()) {
			/* return initiative details*/

			String query = "match (i:Init) where i.Id = {initiativeId} "
					+ "return i.Id as id,i.Name as name,i.Type as type,i.StartDate as startDate,i.EndDate as endDate,i.Comment as comment";
			Result res = dch.graphDb.execute(query, params);
			while(res.hasNext()){
				Map<String, Object> result = res.next();
				i.setInitiativeName(result.get("name").toString());
				i.setInitiativeType(result.get("type").toString());
				i.setInitiativeStartDate(result.get("startDate").toString());
				i.setInitiativeEndDate(result.get("endDate").toString());
				i.setInitiativeComment(result.get("comment").toString());
			}
			
			/* return initiative part_of*/
			query= "match (i:Init)<-[r:part_of]-(a) where i.Id = {initiativeId} return a.Id,a.Name,labels(a)";
			res = dch.graphDb.execute(query, params);
			/* return initiative owner*/
			query = "match (i:Init)<-[r:owns]-(a:Employee) where i.Id = {initiativeId} return a.EmpId,a.Name";
			res = dch.graphDb.execute(query, params);
		}
				
		
		return i;
	}
	
	public boolean delete()
	{
		return true; // true if it is deleted properly, false otherwise
	}
	
	public boolean update()
	{
		return true; // saves / updated the current initiative to DB -return true if successful. This WILL NOT create a new initiative
	}

	public String getInitiativeName() {
		return initiativeName;
	}

	public void setInitiativeName(String initiativeName) {
		this.initiativeName = initiativeName;
	}

	public String getInitiativeType() {
		return initiativeType;
	}

	public void setInitiativeType(String initiativeType) {
		this.initiativeType = initiativeType;
	}

	public String getInitiativeStartDate() {
		return initiativeStartDate;
	}

	public void setInitiativeStartDate(String initiativeStartDate) {
		this.initiativeStartDate = initiativeStartDate;
	}

	public String getInitiativeEndDate() {
		return initiativeEndDate;
	}

	public void setInitiativeEndDate(String initiativeEndDate) {
		this.initiativeEndDate = initiativeEndDate;
	}

	public String getInitiativeComment() {
		return initiativeComment;
	}

	public void setInitiativeComment(String initiativeComment) {
		this.initiativeComment = initiativeComment;
	}

	public ArrayList<String> getFuncList() {
		return funcList;
	}

	public void setFuncList(ArrayList<String> funcList) {
		this.funcList = funcList;
	}

	public ArrayList<String> getZoneList() {
		return zoneList;
	}

	public void setZoneList(ArrayList<String> zoneList) {
		this.zoneList = zoneList;
	}

	public ArrayList<String> getPosList() {
		return posList;
	}

	public void setPosList(ArrayList<String> posList) {
		this.posList = posList;
	}

	public ArrayList<String> getEmpIdList() {
		return empIdList;
	}

	public void setEmpIdList(ArrayList<String> empIdList) {
		this.empIdList = empIdList;
	}

	public int getInitiativeId() {
		return initiativeId;
	}

	public void setInitiativeId(int initiativeId) {
		this.initiativeId = initiativeId;
	}

}
