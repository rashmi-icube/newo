package owen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import owen.helper.DatabaseConnectionHelper;


/**
 * Retrieves the list of Initiatives
 */
public class RetreiveInitiative {
	static DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
	public static void main(String[] args) {
		try (Transaction tx = dch.graphDb.beginTx()) {
			RetreiveInitiative ri = new RetreiveInitiative();
			List<Map<String, String>> initiativeList = ri.getInitiativeList();
			for (Map<String, Object> map : list) {
			    for (Map.Entry<String, Object> entry : map.entrySet()) {
			        String key = entry.getKey();
			        Object value = entry.getValue();
			    }
			}
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("initiativeList", initiativeList);
			RetreiveInitiative i = new RetreiveInitiative();
			//List<Map<String, String>> initiativeDetailsList = i.getInitiativeDetails(initiativeList);
			tx.success();
		}
		dch.shutDown();
	}

	public List<Map<String, String>> getInitiativeList() 
	{
		List<Map<String, String>> initiativeList = new ArrayList<>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String initiativeListQuery = "match (i:Init) return i.Name as Name,i.Id as Id";
			Result res = dch.graphDb.execute(initiativeListQuery);
			while (res.hasNext()) {
				Map<String, String> initiativeMap = new HashMap<String, String>();
				Map<String, Object> resultMap = res.next();
				initiativeMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
				initiativeList.add(initiativeMap);
			}
			tx.success();
			return initiativeList;
		}
	}
	
	/*public List<Map<String, String>> getInitiativeDetails(List<Map<String, String>> params) {
		List<Map<String, String>> initiativeDetailList = new ArrayList<>();
		try (Transaction tx = dch.graphDb.beginTx()) {
		String initiativeDetailsQuery = "match (i:Init)<-[r:part_of]-(a) WHERE i.Id in {Id} return a.Name,labels(a)";
		Result res = dch.graphDb.execute(initiativeDetailsQuery);
		while (res.hasNext()) {
			Map<String, String> initiativeDetailMap = new HashMap<String, String>();
			Map<String, Object> resultMap = res.next();
			initiativeDetailMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
			initiativeDetailList.add(initiativeDetailMap);
		}
		tx.success();
		return initiativeDetailList;
	}
	}*/
}