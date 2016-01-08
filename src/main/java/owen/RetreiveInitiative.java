package owen;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import owen.helper.DatabaseConnectionHelper;

/**
 *retrieves the list of initiatives
 *
 */
public class RetreiveInitiative {
	public static void main(String[] args) {

		try (Transaction tx = DatabaseConnectionHelper.graphDb.beginTx()) {
			getInitiativeList();
			tx.success();
		}
	}

	static void getInitiativeList() {
		// 1.call the getInitiativeProperties
		// 2.store it in iterable object
		// 3.iterate through the iterator object
		// 4.display the list of initiatives

		try (Transaction tx = DatabaseConnectionHelper.graphDb.beginTx()) {

			String initiativeList = "match (i:Init) return i.Name as Name,i.Id as Id";
			// get query for all the nodes attached to the initiative
			Result res = DatabaseConnectionHelper.graphDb
					.execute(initiativeList);
			while (res.hasNext()) {
				System.out.println(res.resultAsString());
			}
			tx.success();

		}

	}
}
