package owen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import owen.helper.DatabaseConnectionHelper;
import scala.collection.convert.Wrappers.SeqWrapper;

/**
 * Retrieves the list of Initiatives
 */
public class RetreiveInitiative {
	static DatabaseConnectionHelper dch = new DatabaseConnectionHelper();

	public static void main(String[] args) {
		try (Transaction tx = dch.graphDb.beginTx()) {
			RetreiveInitiative ri = new RetreiveInitiative();
			List<Initiative> initiativeList = ri.getInitiativeList();
			System.out.println(initiativeList.toString());
			tx.success();
		}
		dch.shutDown();
	}

	public List<Initiative> getInitiativeList() {
		Map<Integer, Initiative> initiativeIdMap = new HashMap<Integer, Initiative>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String initiativeListQuery = "match (o:Employee)-[:owner_of]->(i:Init)<-[r:part_of]-(a)"
					+ " return i.Id as Id,i.Name as Name,i.StartDate as StartDate,"
					+ "i.EndDate as EndDate,collect(distinct(a.Name))as PartOf, labels(a) as Filters,"
					+ "collect(distinct (o.Name)) as OwnersOf,i.Comment as Comments,i.Type as Type";
			Result res = dch.graphDb.execute(initiativeListQuery);
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

			List<Initiative> initiativeList = new ArrayList<>();
			for (int initiativeId : initiativeIdMap.keySet()) {
				initiativeList.add(initiativeIdMap.get(initiativeId));
			}
			return initiativeList;
		}
	}

	private void setInitiativeValues(Map<String, Object> resultMap, Initiative i) {
		i.setInitiativeId(Integer.valueOf(resultMap.get("Id").toString()));
		i.setInitiativeName((String) resultMap.get("Name"));
		i.setInitiativeStartDate((String) resultMap.get("StartDate"));
		i.setInitiativeEndDate((String) resultMap.get("EndDate"));
		i.setInitiativeComment((String) resultMap.get("Comment"));
		i.setInitiativeType((String) resultMap.get("Type"));
		i.setEmpIdList(getListFromResult(resultMap, "OwnersOf"));
		setPartOfConnections(resultMap, i);
	}

	private void setPartOfConnections(Map<String, Object> resultMap, Initiative i) {
		if (resultMap.get("Filters").toString().contains("Position")) {
			i.setPosList(getListFromResult(resultMap, "PartOf"));
		} else if (resultMap.get("Filters").toString().contains("Zone")) {
			i.setZoneList(getListFromResult(resultMap, "PartOf"));
		} else {
			i.setFuncList(getListFromResult(resultMap, "PartOf"));
		}
	}

	private ArrayList<String> getListFromResult(Map<String, Object> resultMap, String columnName) {
		SeqWrapper sw = (SeqWrapper) resultMap.get(columnName);
		ArrayList<String> result = new ArrayList<>();
		Iterator iter = sw.iterator();
		while (iter.hasNext()) {
			result.add((String) iter.next());
		}
		return result;
	}

}