package owen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import owen.helper.DatabaseConnectionHelper;

/**
 * retrieves the list of initiatives
 *
 */
public class RetreiveInitiative {
	public static void main(String[] args) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			RetreiveInitiative ri = new RetreiveInitiative();
			ri.getInitiativeList();
			tx.success();
		}
	}

	public List<Map<Integer, String>> getInitiativeList() {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {
			List<Map<Integer, String>> initiativeList = new ArrayList<Map<Integer, String>>();
			String query = "match (i:Init) return i.Name as Name,i.Id as Id";
			Result res = dch.graphDb.execute(query);
	        while (res.hasNext()) {
	        	List<String> columnNames = res.columns();
	        	for(String c : columnNames){
	        		String id = res.columnAs(c).toString();
	        	}
	        }
			
			/*Iterator it = res.columnAs("Id");
			while (it.hasNext()) {
				Map<Integer, String> initiative = new HashMap<Integer, String>();
				int initiativeId = Integer.parseInt(it.next().toString());
				
				initiativeList.add(initiative);
			}*/

			tx.success();
			return initiativeList;
		}

	}

	public Initiative getInitiativeDetails(int initiativeId) {
		Initiative i = new Initiative();
		String query = "match (i:Init)<-[r:part_of]-(a) WHERE i.Id = initiativeId return a.Name,labels(a)";
		return i;
	}
}
