package owen;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import owen.helper.DatabaseConnectionHelper;

/**
 * Creates an initiative 
 * Connects objects that are part of the initiative 
 * Connects employees who are owners of the initiative
 *
 */
public class CreateInitiative {

	private String initiativeName = "";
	private String initiativeType = "";
	private String initiativeStartDate = "";
	private String initiativeEndDate = "";
	private String initiativeComment = "";
	private ArrayList<String> funcList;
	private ArrayList<String> zoneList;
	private ArrayList<String> posList;
	private ArrayList<String> empIdList;
	
	
	public static void main(final String[] args) throws IOException, ParseException {
		CreateInitiative initiative = new CreateInitiative();
		ArrayList<String> funcList = new ArrayList<String>();
		ArrayList<String> zoneList = new ArrayList<String>();
		ArrayList<String> posList = new ArrayList<String>();
		zoneList.add("Z1");
		zoneList.add("Z2");
		posList.add("P1");
		posList.add("P2");
		// fun.add("F1");
		// fun.add("F2");
		funcList.add("All");
		
		ArrayList<String> empIdList = new ArrayList<String>();
		empIdList.add("5029350");
		empIdList.add("508344");
		
		initiative.setInitiativeProperties("Initiative21", "Change Process", "01/01/2015", "01/05/2015", "xyz", funcList, zoneList, posList, empIdList);
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {

			
			int initiativeId = initiative.createInitiativeNode();
			System.out.println("Created initiative with ID : " + initiativeId);
			if (initiativeId > 0) {

				Map<String, Object> params = new HashMap<String, Object>();
				params.put("zoneList", zoneList);
				params.put("funcList", funcList);
				params.put("posList", posList);

				params.put("initiativeId", initiativeId);
				initiative.createConnectionsPartOf(params);
				System.out.println("Success");

				params.clear();
				params.put("initiativeId", initiativeId);
				params.put("empIdList", empIdList);
				initiative.createConnectionsOwnerOf(params);
				System.out.println("Success");
			}

			tx.success();
		}
		dch.shutDown();
	}

	public void setInitiativeProperties(String initiativeName, String initiativeType, String initiativeStartDate, String initiativeEndDate,
			String initiativeComment, ArrayList<String> funcList, ArrayList<String> zoneList, ArrayList<String> posList, ArrayList<String> empIdList) {
		this.initiativeName = initiativeName;
		this.initiativeType = initiativeType;
		this.initiativeStartDate = initiativeStartDate;
		this.initiativeEndDate = initiativeEndDate;
		this.initiativeComment = initiativeComment;
		this.funcList = funcList;
		this.zoneList = zoneList;
		this.posList = posList;
		this.empIdList = empIdList;

	}
	/**
	 * Creation of the actual initiative happens here
	 * 
	 * @return - Initiative id of the newly created initiative
	 */
	 public int createInitiativeNode() {
		 DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		String initiativeId = "";
		try (Transaction tx = dch.graphDb.beginTx()) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			Date startDate = sdf.parse(initiativeStartDate);
			Date endDate = sdf.parse(initiativeEndDate);
			String createInitQuery = "match (i:Init)  with CASE count(i) WHEN 0  THEN 1 ELSE max(i.Id)+1 END as uid " +
					"CREATE (i:Init {Id:uid,Name:'<<InitName>>',Type:'<<InitType>>',StartDate:'<<StartDate>>',EndDate:'<<EndDate>>',Comment:'<<Comment>>'}) return i.Id as Id";
			createInitQuery = createInitQuery.replace("<<InitName>>", initiativeName);
			createInitQuery = createInitQuery.replace("<<InitType>>", initiativeType);
			createInitQuery = createInitQuery.replace("<<StartDate>>", startDate.toString());
			createInitQuery = createInitQuery.replace("<<EndDate>>", endDate.toString());
			createInitQuery = createInitQuery.replace("<<Comment>>", initiativeComment);
			Result res = dch.graphDb.execute(createInitQuery);
			Iterator it = res.columnAs("Id");
			while (it.hasNext()){
				initiativeId = it.next().toString();
			}

			tx.success();

		} catch (ParseException e) {

			e.printStackTrace();
		}
		return Integer.parseInt(initiativeId);
	}

	/**
	 * Creates the connections with the objects that are part of the initiative
	 * 
	 * @param params - Map of the objects that are part of the initiative taken as input from the user
	 */
	public void createConnectionsPartOf(Map<String, Object> params) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {

			String funcQuery = "", posQuery = "", zoneQuery = "";
			ArrayList<String> funcParam = (ArrayList<String>)params.get("funcList");
			if (funcParam.contains("all")) {
				funcQuery = "Match (i:Init),(f:Function) WHERE i.Id = {initiativeId} Create f-[:part_of]->i ";
			} else {
				funcQuery = "Match (i:Init),(f:Function) where i.Id = {initiativeId} and f.Id in {funcList} Create f-[:part_of]->i ";
			}

			posQuery = "Match (i:Init),(p:Position) where i.Id = {initiativeId} and p.Id in {posList} Create p-[:part_of]->i";
			zoneQuery = "Match (i:Init),(z:Zone) where i.Id = {initiativeId} and z.Id in {zoneList} create z-[:part_of]->i";
			dch.graphDb.execute(funcQuery, params);
			dch.graphDb.execute(posQuery, params);
			dch.graphDb.execute(zoneQuery, params);
			tx.success();
		}

	}

	/**
	 * Creates the connections with the employees who are owners of the initiative
	 * 
	 * @param params - Map of the employee id's who would be the owner of the initiative taken as input from the user
	 */
	public void createConnectionsOwnerOf(Map<String, Object> params) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();
		try (Transaction tx = dch.graphDb.beginTx()) {

			String Str = "Match (i:Init),(e:Employee) where i.Id = {initiativeId} and e.EmpID in {empIdList} Create e-[:owner_of]->i";
			dch.graphDb.execute(Str, params);
			tx.success();
		}

	}
}
