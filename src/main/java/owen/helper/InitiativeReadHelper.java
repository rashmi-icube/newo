package owen.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * Populates the filter criteria for the initiative
 *
 */
public class InitiativeReadHelper {
	
	static DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
	
	public static void main(String[] args) {
		try (Transaction tx = dch.graphDb.beginTx()) {
			String filter = "Function";
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
	 */

	public List<Map<String, String>> getMasterFilterList(String filterName) {
		List<Map<String, String>> filterMapList = new ArrayList<Map<String, String>>();
		try (Transaction tx = dch.graphDb.beginTx()) {
			String query = "match (n:<<filterName>>) return n.Name as Name,n.Id as Id";
			query = query.replace("<<filterName>>", filterName);
			Result res = dch.graphDb.execute(query);
			while (res.hasNext()) {
				Map<String, String> filterRowMap = new HashMap<String, String>();
				Map<String, Object> resultMap = res.next();
				filterRowMap.put(resultMap.get("Id").toString(), resultMap.get("Name").toString());
				filterMapList.add(filterRowMap);
			}
			tx.success();
			return filterMapList;
		}
	}

}
