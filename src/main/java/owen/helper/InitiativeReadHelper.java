package owen.helper;

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
			getMasterFilterList(filter);
			tx.success();
		}

		DatabaseConnectionHelper.shutDown();
	}

	/**
	 * 
	 * @param filter
	 * @return
	 */
	/**
	 * Retrieves all the objects which belong to a particular filter item
	 * @param filter - A string which denotes a filter item
	*/
	
	static void getMasterFilterList(String filter) {
		try (Transaction tx = DatabaseConnectionHelper.graphDb.beginTx()) {
			String Str = "match (n:<<filter>>) return n.Name as Name,n.Id as Id";
			String Str1 = new String(Str);
			Str1 = Str1.replace("<<filter>>", filter);
			Result res = DatabaseConnectionHelper.graphDb.execute(Str1);
			while (res.hasNext()) {
				System.out.println(res.resultAsString());
			}
			tx.success();
			
		}
	}

}
