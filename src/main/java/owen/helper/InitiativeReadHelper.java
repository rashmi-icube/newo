package owen.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * Populates the filter criteria for the initiative
 *
 */
public class InitiativeReadHelper {
	public static void main(String[] args) {
		try (Transaction tx = DatabaseConnectionHelper.graphDb.beginTx()) {
			String filter = "Function";
			InitiativeReadHelper irh = new InitiativeReadHelper();
			irh.getMasterFilterList(filter);
			tx.success();
		}

		DatabaseConnectionHelper.shutDown();
	}

	/**
	 * Retrieves all the objects which belong to a particular filter item
	 * 
	 * @param filterName - Name of the filter that needs to be populated [Function,
	 *            Zone, Position]
	 */

	public List<Map<Integer, String>> getMasterFilterList(String filterName) {
		List<Map<Integer, String>> filterMapList = new ArrayList<>();
		try (Transaction tx = DatabaseConnectionHelper.graphDb.beginTx()) {
			String query = "match (n:<<filterName>>) return n.Name as Name,n.Id as Id";
			query = query.replace("<<filter>>", filterName);
			Result res = DatabaseConnectionHelper.graphDb.execute(query);
			while (res.hasNext()) {
				System.out.println(res.resultAsString());
			}
			tx.success();
			return filterMapList;
		}
	}

}
