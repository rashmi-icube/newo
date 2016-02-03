package org.icube.owen.initiative;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class InitiativeList extends TheBorg {

	/**
	 * Get the list of initiatives based on the status provided in the filter 
	 * @param initiativeStatus : Can be any one of the following : Active, Pending, Deleted, Completed
	 * @return list of initiatives
	 */
	public List<Initiative> getInitiativeList(String initiativeStatus) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String initiativeListQuery = "match (o:Employee)-[:owner_of]->(i:Init {Status:'" + initiativeStatus
					+ "'})<-[r:part_of]-(a) return i.Id as Id, i.Name as Name,"
					+ "i.StartDate as StartDate, i.EndDate as EndDate, case i.Category when 'Individual' then collect(distinct(a.emp_id)) "
					+ "else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
					+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Query for retrieving initiative with status " + initiativeStatus);
			Result res = dch.graphDb.execute(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
					"Executed query for retrieving initiative list with status " + initiativeStatus);
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				int initiativeId = Integer.valueOf(resultMap.get("Id").toString());
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
						i.setFilterList(ih.setPartOfConnections(resultMap, i));
					} else if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
						i.setPartOfEmployeeList(ih.setPartOfEmployeeList(resultMap, i));
					}
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					setInitiativeValues(resultMap, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}
			tx.success();

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug(
					"List of initiatives of type " + initiativeStatus + ": " + initiativeList.toString());
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
	public List<Initiative> getInitiativeList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		List<Initiative> initiativeList = new ArrayList<Initiative>();
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String initiativeListQuery = "match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a) return i.Id as Id, i.Name as Name,"
					+ "i.StartDate as StartDate, i.EndDate as EndDate, case i.Category when 'Individual' then collect(distinct(a.emp_id)) "
					+ "else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
					+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";

			Result res = dch.graphDb.execute(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Executed query for retrieving initiative list");
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				int initiativeId = Integer.valueOf(resultMap.get("Id").toString());
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					i.setFilterList(ih.setPartOfConnections(resultMap, i));
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					setInitiativeValues(resultMap, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}
			tx.success();

			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("List of initiatives : " + initiativeList.toString());
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Exception while getting the initiative list", e);
		}
		return initiativeList;

	}

	/**
	 * @param resultMap- A map containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 */
	private void setInitiativeValues(Map<String, Object> resultMap, Initiative i) {
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting initiative values");
		try {
			i.setInitiativeId(Integer.valueOf(resultMap.get("Id").toString()));
			i.setInitiativeName((String) resultMap.get("Name"));
			i.setInitiativeStatus((String) resultMap.get("Status"));
			i.setInitiativeCategory((String) resultMap.get("Category"));

			SimpleDateFormat parserSDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzz yyyy", Locale.ENGLISH);
			i.setInitiativeStartDate(parserSDF.parse((String) resultMap.get("StartDate")));
			i.setInitiativeEndDate(parserSDF.parse((String) resultMap.get("EndDate")));

			i.setInitiativeComment((String) resultMap.get("Comments"));
			i.setInitiativeType((String) resultMap.get("Type"));
			i.setOwnerOfList(ih.getOwnerOfList(resultMap));
			if (i.getInitiativeCategory().equalsIgnoreCase("Team")) {
				i.setFilterList(ih.setPartOfConnections(resultMap, i));
			} else if (i.getInitiativeCategory().equalsIgnoreCase("Individual")) {
				i.setPartOfEmployeeList(ih.setPartOfEmployeeList(resultMap, i));
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(InitiativeList.class).error("Error in setting initiative values", e);

		}

	}

}