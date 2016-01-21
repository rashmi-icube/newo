package org.icube.owen.initiative;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class Initiative extends TheBorg {

	private int initiativeId;
	private String initiativeName = "";
	private String initiativeType = "";
	private String category = "";
	private Date initiativeStartDate;
	private Date initiativeEndDate;
	private String initiativeComment = "";
	private List<Filter> filterList;
	private List<Employee> ownerOfList;

	/**
	 * Sets the initiative properties based on the values given in the parameters
	 * 
	 * @param initiativeName
	 * @param initiativeType
	 * @param initiativeStartDate
	 * @param initiativeEndDate
	 * @param initiativeComment
	 * @param filterList
	 * @param ownerOfList
	 */
	public void setInitiativeProperties(String initiativeName, String initiativeType, Date initiativeStartDate, Date initiativeEndDate,
			String initiativeComment, List<Filter> filterList, List<Employee> ownerOfList) {
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Setting initiative properties");
		this.initiativeName = initiativeName;
		this.initiativeType = initiativeType;
		this.initiativeStartDate = initiativeStartDate;
		this.initiativeEndDate = initiativeEndDate;
		this.initiativeComment = initiativeComment;
		this.filterList = filterList;
		this.ownerOfList = ownerOfList;
	}

	/**
	 * Creation of the actual initiative happens here
	 * 
	 * @return - initiativeId of the newly created initiative
	 */
	public int create() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		String initiativeIdStr = "";
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating the initiative");

			String createInitQuery = "match (i:Init)  with CASE count(i) WHEN 0  THEN 1 ELSE max(i.Id)+1 END as uid "
					+ "CREATE (i:Init {Id:uid,Name:'<<InitName>>',Type:'<<InitType>>',StartDate:'<<StartDate>>',EndDate:'<<EndDate>>',Comment:'<<Comment>>'}) return i.Id as Id";
			createInitQuery = createInitQuery.replace("<<InitName>>", initiativeName);
			createInitQuery = createInitQuery.replace("<<InitType>>", initiativeType);
			createInitQuery = createInitQuery.replace("<<StartDate>>", initiativeStartDate.toString());
			createInitQuery = createInitQuery.replace("<<EndDate>>", initiativeEndDate.toString());
			createInitQuery = createInitQuery.replace("<<Comment>>", initiativeComment);

			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Create initiative query : " + createInitQuery);
			Result res = dch.graphDb.execute(createInitQuery);
			Iterator it = res.columnAs("Id");
			while (it.hasNext()) {
				initiativeIdStr = it.next().toString();
			}

			int initiativeId = Integer.parseInt(initiativeIdStr);

			if (setPartOf(initiativeId, this.filterList)) {
				org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting part of initiative");
			} else {
				org.apache.log4j.Logger.getLogger(Initiative.class).error("Unsuccessful in setting part of initiative");
			}

			if (setOwner(initiativeId, this.ownerOfList)) {
				org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting owner for initiative");
			} else {
				org.apache.log4j.Logger.getLogger(Initiative.class).error("Unsuccessful in setting owner for initiative");
			}

			tx.success();

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception in Create initiative query", e);

		}
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Initiative ID : " + initiativeIdStr);

		return initiativeId;
	}

	/**
	 * Creates the connections with the objects that are part of the initiative
	 * 
	 * @param params
	 * - Map of the objects that are part of the initiative taken as input from the user
	 */
	@SuppressWarnings("unchecked")
	private boolean setPartOf(int initiativeId, List<Filter> filterList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Create Initiative Connections for initiativeId " + initiativeId);
			Map<String, Object> params = new HashMap<>();
			params.put("initiativeId", initiativeId);
			for (int i = 0; i < filterList.size(); i++) {
				Filter f = filterList.get(i);
				params.put(f.getFilterName(), getFilterValueList(f.getFilterValues()));
			}
			String funcQuery = "", posQuery = "", zoneQuery = "";
			ArrayList<String> funcParam = (ArrayList<String>) params.get("Function");
			ArrayList<String> zoneParam = (ArrayList<String>) params.get("Zone");
			ArrayList<String> posParam = (ArrayList<String>) params.get("Position");
			if (funcParam.contains("all") || funcParam.contains("All")) {
				funcQuery = "Match (i:Init),(f:Function) WHERE i.Id = {initiativeId} Create f-[:part_of]->i ";
			} else {
				funcQuery = "Match (i:Init),(f:Function) where i.Id = {initiativeId} and f.Id in {Function} Create f-[:part_of]->i ";
			}

			if (zoneParam.contains("all") || zoneParam.contains("All")) {
				zoneQuery = "Match (i:Init),(z:Zone) where i.Id = {initiativeId} create z-[:part_of]->i";
			} else {
				zoneQuery = "Match (i:Init),(z:Zone) where i.Id = {initiativeId} and z.Id in {Zone} create z-[:part_of]->i";

			}

			if (posParam.contains("all") || posParam.contains("All")) {
				posQuery = "Match (i:Init),(p:Position) where i.Id = {initiativeId} Create p-[:part_of]->i";
			} else {
				posQuery = "Match (i:Init),(p:Position) where i.Id = {initiativeId} and p.Id in {Position} Create p-[:part_of]->i";
			}

			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Function query : " + funcQuery);
			dch.graphDb.execute(funcQuery, params);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Position query : " + posQuery);
			dch.graphDb.execute(posQuery, params);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Zone query : " + zoneQuery);
			dch.graphDb.execute(zoneQuery, params);
			tx.success();
			return true;
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception while setting part of for initiative ID" + initiativeId, e);
			return false;
		}

	}

	/**
	 * Get list of string filterValues from a map of filterValues
	 * 
	 * @param filterValues
	 * @return
	 */
	private List<String> getFilterValueList(Map<String, String> filterValues) {
		List<String> filterValueStringList = new ArrayList<>();
		filterValueStringList.addAll(filterValues.keySet());
		return filterValueStringList;
	}

	/**
	 * Creates the connections with the employees who are owners of the initiative
	 * 
	 * @param params
	 * - Map of the employee id's who would be the owner of the initiative taken as input from the user
	 */
	private boolean setOwner(int initiativeId, List<Employee> employeeList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			ArrayList<String> empIdList = new ArrayList<>();
			for (Employee e : employeeList) {
				empIdList.add(e.getEmployeeId());
			}

			Map<String, Object> params = new HashMap<>();
			params.put("initiativeId", initiativeId);
			params.put("empIdList", empIdList);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating connections for initiative : " + params.get("initiativeId"));
			String query = "Match (i:Init),(e:Employee) where i.Id = {initiativeId} and e.EmpID in {empIdList} Create e-[:owner_of]->i";
			dch.graphDb.execute(query, params);
			tx.success();
			return true;
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception while creating owner for initiative : " + initiativeId, e);
			return false;
		}
	}

	/**
	 * Retrieves the single initiative based on the initiativeId given
	 * 
	 * @param initiativeId
	 * @return initiative object
	 */
	public Initiative get(int initiativeId) {
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Retrieving the initiative with initiative ID " + initiativeId);

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Initiative i = new Initiative();
		i.setInitiativeId(initiativeId);
		Map<String, Object> params = new HashMap<>();
		params.put("initiativeId", initiativeId);
		try (Transaction tx = dch.graphDb.beginTx()) {
			String query = "match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a)"
					+ " where i.Id = {initiativeId}  return i.Name as Name,i.StartDate as StartDate,"
					+ "i.EndDate as EndDate,collect(distinct(a.Id))as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters,"
					+ "collect(distinct (o.EmpID)) as OwnersOf,i.Comment as Comments,i.Type as Type";
			Result res = dch.graphDb.execute(query, params);
			while (res.hasNext()) {
				Map<String, Object> result = res.next();
				i.setInitiativeName(result.get("Name").toString());
				i.setInitiativeType(result.get("Type").toString());
				SimpleDateFormat parserSDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzz yyyy", Locale.ENGLISH);
				i.setInitiativeStartDate(parserSDF.parse((String) result.get("StartDate")));
				i.setInitiativeEndDate(parserSDF.parse((String) result.get("EndDate")));
				i.setInitiativeComment(result.get("Comments").toString());
				i.setFilterList(ih.setPartOfConnections(result, i));
				i.setOwnerOf(ih.getOwnerOfList(result));
			}

		} catch (ParseException e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception while retrieving the initiative with ID" + initiativeId, e);

		}

		return i;
	}

	/**
	 * Returns the master list of initiative types based on category
	 * 
	 * @return initiativeTypeMap
	 */
	public Map<Integer, String> getInitiativeTypeMap(String category) {
		Map<Integer, String> initiativeTypeMap = new HashMap<>();
		if (category.equalsIgnoreCase("team")) {
			initiativeTypeMap.put(1, "Performance");
			initiativeTypeMap.put(2, "Social Cohesion");
			initiativeTypeMap.put(3, "Retention");
			initiativeTypeMap.put(4, "Innovation");
			initiativeTypeMap.put(5, "Sentiment");
		} else {
			initiativeTypeMap.put(1, "Expertise");
			initiativeTypeMap.put(2, "Mentorship");
			initiativeTypeMap.put(3, "Retention");
			initiativeTypeMap.put(4, "Influence");
			initiativeTypeMap.put(5, "Sentiment");
		}

		return initiativeTypeMap;
	}

	public boolean delete() {
		return true; // true if it is deleted properly, false otherwise
	}

	public boolean update() {
		return true; // saves / updated the current initiative to DB -return true if successful.
						// This WILL NOT create a new initiative
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

	public Date getInitiativeStartDate() {
		return initiativeStartDate;
	}

	public void setInitiativeStartDate(Date initiativeStartDate) {
		this.initiativeStartDate = initiativeStartDate;
	}

	public Date getInitiativeEndDate() {
		return initiativeEndDate;
	}

	public void setInitiativeEndDate(Date initiativeEndDate) {
		this.initiativeEndDate = initiativeEndDate;
	}

	public String getInitiativeComment() {
		return initiativeComment;
	}

	public void setInitiativeComment(String initiativeComment) {
		this.initiativeComment = initiativeComment;
	}

	public int getInitiativeId() {
		return initiativeId;
	}

	public void setInitiativeId(int initiativeId) {
		this.initiativeId = initiativeId;
	}

	public List<Filter> getFilterList() {
		return filterList;
	}

	public void setFilterList(List<Filter> filterList) {
		this.filterList = filterList;
	}

	public List<Employee> getOwnerOf() {
		return ownerOfList;
	}

	public void setOwnerOf(List<Employee> employeeList) {
		this.ownerOfList = employeeList;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
