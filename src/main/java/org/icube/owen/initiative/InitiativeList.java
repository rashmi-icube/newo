package org.icube.owen.initiative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import scala.collection.convert.Wrappers.SeqWrapper;

/**
 * Retrieves the list of Initiatives
 */
public class InitiativeList extends TheBorg{

	public static void main(String[] args) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			InitiativeList ri = new InitiativeList();
			List<Initiative> initiativeList = ri.getInitiativeList();
			System.out.println(initiativeList.toString());
			tx.success();
		}
		dch.shutDown();
	}

	/**
	 * Retrieves the list of Initiatives along with all its attributes and connections
	 * @return - A list of Initiatives
	 */
	public List<Initiative> getInitiativeList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Get initiative list");
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String initiativeListQuery = "match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a)"
					+ " return i.Id as Id,i.Name as Name,i.StartDate as StartDate,"
					+ "i.EndDate as EndDate,collect(distinct(a.Name))as PartOf, labels(a) as Filters,"
					+ "collect(distinct (o.Name)) as OwnersOf,i.Comment as Comments,i.Type as Type";
			Result res = dch.graphDb.execute(initiativeListQuery);
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Executed query for retrieving initiative list");
			while (res.hasNext()) {
				Map<String, Object> resultMap = res.next();
				int initiativeId = Integer.valueOf(resultMap.get("Id").toString());
				if (initiativeIdMap.containsKey(initiativeId)) {
					Initiative i = initiativeIdMap.get(initiativeId);
					setPartOfConnections(resultMap, i);
					initiativeIdMap.put(initiativeId, i);
				} else {
					Initiative i = new Initiative();
					setInitiativeValues(resultMap, i);
					initiativeIdMap.put(initiativeId, i);
				}

			}
			tx.success();

			List<Initiative> initiativeList = new ArrayList<Initiative>();
			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("List of initiatives : " +initiativeList.toString());
			return initiativeList;
		}
	}

	/**
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 */
	private void setInitiativeValues(Map<String, Object> resultMap, Initiative i) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting initiative values");
		i.setInitiativeId(Integer.valueOf(resultMap.get("Id").toString()));
		i.setInitiativeName((String) resultMap.get("Name"));
		i.setInitiativeStartDate((String) resultMap.get("StartDate"));
		i.setInitiativeEndDate((String) resultMap.get("EndDate"));
		i.setInitiativeComment((String) resultMap.get("Comment"));
		i.setInitiativeType((String) resultMap.get("Type"));
		i.setEmpIdList(getListFromResult(resultMap, "OwnersOf"));
		setPartOfConnections(resultMap, i);
	}

	/**
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @param i - An Initiative object
	 */
	private void setPartOfConnections(Map<String, Object> resultMap, Initiative i) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Setting part of connections");
		if (resultMap.get("Filters").toString().contains("Position")) {
			i.setPosList(getListFromResult(resultMap, "PartOf"));
		} else if (resultMap.get("Filters").toString().contains("Zone")) {
			i.setZoneList(getListFromResult(resultMap, "PartOf"));
		} else {
			i.setFuncList(getListFromResult(resultMap, "PartOf"));
		}
	}

	/**
	 * @param resultMap - A map containing the Initiative attributes and connections
	 * @param columnName - Name of the column to iterate through from the resultMap 
	 * @return - Returns a list of strings from the resultMap
	 */
	private ArrayList<String> getListFromResult(Map<String, Object> resultMap, String columnName) {
		org.apache.log4j.Logger.getLogger(InitiativeList.class).debug("Converting result to a list");
		SeqWrapper sw = (SeqWrapper) resultMap.get(columnName);
		ArrayList<String> result = new ArrayList<String>();
		Iterator iter = sw.iterator();
		while (iter.hasNext()) {
			result.add((String) iter.next());
		}
		return result;
	}

}