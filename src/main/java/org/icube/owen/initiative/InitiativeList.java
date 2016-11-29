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
	 * @param companyId - Company ID
	 * @param category - category for the initiative
	 * @param initiativeStatus - Status of the initiatives to be listed
	 * @return - list of initiatives
	 */

	public List<Initiative> getInitiativeListByStatus(int companyId, String category, String initiativeStatus) {
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		initiativeList = getInitiativeList(companyId, category, "Status", initiativeStatus);
		return initiativeList;
	}

	/**
	 * Get the list of initiatives based on the type provided in the filter
	 * @param companyId - Company ID
	 * @param category - category for the initiative
	 * @param initiativeTypeId - ID of the type of initiative to be listed
	 * @return - list of initiatives
	 */

	public List<Initiative> getInitiativeListByType(int companyId, String category, int initiativeTypeId) {
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		initiativeList = getInitiativeList(companyId, category, "Type", initiativeTypeId);
		return initiativeList;
	}

	/**
	 * Retrieves the list of initiatives of specific category and status/type
	 * @param companyId - Company ID
	 * @param category - Category of the initiative(Team/Individual)
	 * @param viewByCriteria - Retrieves list of initiative based on criteria(Status/Type)
	 * @param viewByValue - Status/Type of initiative to be viewed
	 * @return List of initiatives
	 */
	private List<Initiative> getInitiativeList(int companyId, String category, String viewByCriteria, Object viewByValue) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
				"entering getInitiativeList with category : " + category + " ; viewByCriteria : " + viewByCriteria + " viewByValue : " + viewByValue);

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).info("HashMap created!!!");
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try {
			String initiativeListQuery = "";
			if (viewByCriteria.equalsIgnoreCase("Type")) {
				initiativeListQuery = "match (i:Init {Type:"
						+ (Integer) viewByValue
						+ ", Category:'"
						+ category
						+ "'})<-[r:part_of]-(a) WITH i,a optional match (o:Employee)-[:owner_of]->(i) return i.Id as Id, "
						+ "i.Name as Name,i.StartDate as StartDate, i.EndDate as EndDate, i.CreatedByEmpId as CreatedByEmpId, i.CreatedOn as CreationDate, "
						+ "case i.Category when 'Individual' then collect(distinct(a.emp_id)) else collect(distinct(a.Id)) "
						+ "end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
						+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
						"Query for retrieving initiative of type " + viewByValue + " : " + initiativeListQuery);

			} else if (viewByCriteria.equalsIgnoreCase("Status")) {
				initiativeListQuery = "match (i:Init {Status:'"
						+ (String) viewByValue
						+ "', Category:'"
						+ category
						+ "'})<-[r:part_of]-(a) WITH i,a optional match (o:Employee)-[:owner_of]->(i) return i.Id as Id, "
						+ "i.Name as Name,i.StartDate as StartDate, i.EndDate as EndDate, i.CreatedByEmpId as CreatedByEmpId, i.CreatedOn as CreationDate, "
						+ "case i.Category when 'Individual' then collect(distinct(a.emp_id)) else collect(distinct(a.Id)) "
						+ "end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
						+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
						"Query for retrieving initiative with status " + viewByValue + " : " + initiativeListQuery);

			}

			if (initiativeListQuery.isEmpty()) {
				org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Incorrect criteria has been given " + viewByCriteria);
				throw new Exception("Incorrect criteria has been given " + viewByCriteria);
			}
			try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement();
					ResultSet res = stmt.executeQuery(initiativeListQuery)) {
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
						"Executed query for retrieving initiative list with " + viewByCriteria + " : " + viewByValue);
				while (res.next()) {
					int initiativeId = res.getInt("Id");
					if (initiativeIdMap.containsKey(initiativeId)) {
						Initiative i = initiativeIdMap.get(initiativeId);
						if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
							i.setFilterList(ih.setPartOfConnections(companyId, res, i));
						} else if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
							i.setPartOfEmployeeList(ih.setPartOfEmployeeList(companyId, res, i));
						}
						initiativeIdMap.put(initiativeId, i);
					} else {
						Initiative i = new Initiative();
						setInitiativeValues(companyId, res, i);
						initiativeIdMap.put(initiativeId, i);
					}

				}
			}
			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}

			// sort initiatives based on the end date for displaying purpose
			Collections.sort(initiativeList, (o1, o2) -> o1.getInitiativeEndDate().compareTo(o2.getInitiativeEndDate()));

			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
					"List of initiatives of " + viewByCriteria + viewByValue + ": " + initiativeList.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Exception while getting the initiative list", e);
		}
		return initiativeList;

	}

	/**
	 * Retrieves the list of Initiatives along with all its attributes and connections
	 * @param companyId - Company ID
	 * @param category - Category of the initiative(Team/Individual)
	 * @return A list of Initiatives
	 */
	public List<Initiative> getInitiativeList(int companyId, String category) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).info("HashMap created!!!");
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try (Statement stmt = dch.companyConnectionMap.get(companyId).getNeoConnection().createStatement()) {
			String initiativeListQuery = "match (i:Init {Category:'"
					+ category
					+ "'})<-[r:part_of]-(a) where i.Status in ['Active','Pending'] WITH i,a optional "
					+ "match (o:Employee)-[:owner_of]->(i) return i.Id as Id, i.Name as Name,i.StartDate as StartDate, "
					+ "i.EndDate as EndDate,i.CreatedByEmpId as CreatedByEmpId, i.CreatedOn as CreationDate, case i.Category when 'Individual' then collect(distinct(a.emp_id)) "
					+ "else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
					+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
					"Query for retrieving all initiatives for category " + category + " : " + initiativeListQuery);
			try (ResultSet res = stmt.executeQuery(initiativeListQuery);) {
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Executed query for retrieving initiative list");
				while (res.next()) {

					int initiativeId = res.getInt("Id");
					if (initiativeIdMap.containsKey(initiativeId)) {
						Initiative i = initiativeIdMap.get(initiativeId);
						i.setFilterList(ih.setPartOfConnections(companyId, res, i));
						initiativeIdMap.put(initiativeId, i);
					} else {
						Initiative i = new Initiative();
						setInitiativeValues(companyId, res, i);
						initiativeIdMap.put(initiativeId, i);
					}

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
	 * Sets the initiative values
	 * @param companyId - Company ID
	 * @param res- A map containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 */

	public void setInitiativeValues(int companyId, ResultSet res, Initiative i) {
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

			i.setCreatedByEmpId(res.getInt("CreatedByEmpId"));
			i.setInitiativeComment(res.getString("Comments"));
			i.setInitiativeTypeId(res.getInt("Type"));
			i.setOwnerOfList(ih.getOwnerOfList(companyId, res));
			if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
				i.setFilterList(ih.setPartOfConnections(companyId, res, i));
				org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
						"Filter list size for " + i.getInitiativeName() + " is " + i.getFilterList().size());
			} else if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
				i.setPartOfEmployeeList(ih.setPartOfEmployeeList(companyId, res, i));
			}
			i.setInitiativeMetrics(ih.setInitiativeMetrics(companyId, i));
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Error in setting initiative values", e);

		}

	}

}