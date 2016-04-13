package org.icube.owen.initiative;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class InitiativeList extends TheBorg {

	/**
	 * Get the list of initiatives based on the status provided in the filter
	 * @param category - category for the initiative
	 * @param initiativeStatus - Status of the initiatives to be listed
	 * @return - list of initiatives
	 */
	public List<Initiative> getInitiativeListByStatus(String category, String initiativeStatus) {
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		initiativeList = getInitiativeList(category, "Status", initiativeStatus);
		return initiativeList;
	}

	/**
	 * Get the list of initiatives based on the type provided in the filter
	 * @param category - category for the initiative
	 * @param initiativeTypeId - ID of the type of initiative to be listed
	 * @return - list of initiatives
	 */
	public List<Initiative> getInitiativeListByType(String category, int initiativeTypeId) {
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		initiativeList = getInitiativeList(category, "Type", initiativeTypeId);
		return initiativeList;
	}

	/**
	 * @param viewByCriteria - Retrieves list of initiative based on criteria(Status/Type)
	 * @param viewByValue - Status/Type of initiative to be viewed
	 * @return List of initiatives
	 */
	private List<Initiative> getInitiativeList(String category, String viewByCriteria, Object viewByValue) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try {
			String initiativeListQuery = "";
			if (viewByCriteria.equalsIgnoreCase("Type")) {
				initiativeListQuery = "match (i:Init {Type:" + (Integer) viewByValue + ", Category:'" + category
						+ "'}) WITH i optional match (o:Employee)-[:owner_of]->(i)<-[r:part_of]-(a) return i.Id as Id, "
						+ "i.Name as Name,i.StartDate as StartDate, i.EndDate as EndDate, i.CreatedOn as CreationDate, "
						+ "case i.Category when 'Individual' then collect(distinct(a.emp_id)) else collect(distinct(a.Id)) "
						+ "end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
						+ "collect(distinct (o.emp_id)) asOwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
						"Query for retrieving initiative of type " + viewByValue + " : " + initiativeListQuery);

			} else if (viewByCriteria.equalsIgnoreCase("Status")) {
				initiativeListQuery = "match (i:Init {Status:'" + (String) viewByValue + "', Category:'" + category
						+ "'}) WITH i optional match (o:Employee)-[:owner_of]->(i)<-[r:part_of]-(a) return i.Id as Id, "
						+ "i.Name as Name,i.StartDate as StartDate, i.EndDate as EndDate, i.CreatedOn as CreationDate, "
						+ "case i.Category when 'Individual' then collect(distinct(a.emp_id)) else collect(distinct(a.Id)) "
						+ "end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
						+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
						"Query for retrieving initiative with status " + viewByValue + " : " + initiativeListQuery);

			}

			if (initiativeListQuery.isEmpty()) {
				org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Incorrect criteria has been given " + viewByCriteria);
				throw new Exception();
			}
			Statement stmt = dch.neo4jCon.createStatement();
			ResultSet res = stmt.executeQuery(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
					"Executed query for retrieving initiative list with " + viewByCriteria + " : " + viewByValue);
			while (res.next()) {

				int initiativeId = res.getInt("Id");
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
						i.setFilterList(ih.setPartOfConnections(res, i));
					} else if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
						i.setPartOfEmployeeList(ih.setPartOfEmployeeList(res, i));
					}
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					setInitiativeValues(res, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}

			Collections.sort(initiativeList, (o1, o2) -> o1.getInitiativeEndDate().compareTo(o2.getInitiativeEndDate()));

			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
					"List of initiatives of " + viewByCriteria + viewByValue + ": " + initiativeList.toString());
			stmt.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Exception while getting the initiative list", e);
		}
		return initiativeList;

	}

	/**
	 * Retrieves the list of Initiatives along with all its attributes and connections
	 * 
	 * @return - A list of Initiatives
	 */
	public List<Initiative> getInitiativeList(String category) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try {
			String initiativeListQuery = "match (i:Init {Category:'" + category + "'}) where i.Status in ['Active','Pending'] WITH i optional "
					+ "match (o:Employee)-[:owner_of]->(i)<-[r:part_of]-(a) return i.Id as Id, i.Name as Name,i.StartDate as StartDate, "
					+ "i.EndDate as EndDate, i.CreatedOn as CreationDate, case i.Category when 'Individual' then collect(distinct(a.emp_id)) "
					+ "else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
					+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
			Statement stmt = dch.neo4jCon.createStatement();
			ResultSet res = stmt.executeQuery(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Executed query for retrieving initiative list");
			while (res.next()) {

				int initiativeId = res.getInt("Id");
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					i.setFilterList(ih.setPartOfConnections(res, i));
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					setInitiativeValues(res, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("List of initiatives : " + initiativeList.toString());
			stmt.close();
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Exception while getting the initiative list", e);
		}
		Collections.sort(initiativeList, (o1, o2) -> o1.getInitiativeEndDate().compareTo(o2.getInitiativeEndDate()));
		return initiativeList;

	}

	/**
	 * @param res- A map containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 */
	public void setInitiativeValues(ResultSet res, Initiative i) {
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting initiative values");

		try {
			i.setInitiativeId(res.getInt("Id"));
			i.setInitiativeName(res.getString("Name"));
			i.setInitiativeStatus(res.getString("Status"));
			i.setInitiativeCategory(res.getString("Category"));

			SimpleDateFormat parserSDF = new SimpleDateFormat(UtilHelper.dateTimeFormat);
			i.setInitiativeStartDate(parserSDF.parse(res.getString("StartDate")));
			i.setInitiativeEndDate(parserSDF.parse(res.getString("EndDate")));
			i.setInitiativeCreationDate(parserSDF.parse(res.getString("CreationDate")));

			i.setInitiativeComment(res.getString("Comments"));
			i.setInitiativeTypeId(res.getInt("Type"));
			i.setOwnerOfList(ih.getOwnerOfList(res));
			if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
				i.setFilterList(ih.setPartOfConnections(res, i));
			} else if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
				i.setPartOfEmployeeList(ih.setPartOfEmployeeList(res, i));
			}
			i.setInitiativeMetrics(ih.setInitiativeMetrics(i));
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Error in setting initiative values", e);

		}

	}

}