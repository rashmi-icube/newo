package org.icube.owen.initiative;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;

public class Initiative extends TheBorg {

	private int initiativeId;
	private String initiativeName = "";
	private int initiativeTypeId;
	private String initiativeCategory = "";
	private String initiativeStatus = "";
	private Date initiativeStartDate;
	private Date initiativeEndDate;
	private String initiativeComment = "";
	private List<Filter> filterList;
	private List<Employee> ownerOfList;
	private List<Employee> partOfEmployeeList;
	private List<Metrics> initiativeMetrics;

	/**
	 * Sets the initiative properties based on the values given in the
	 * parameters
	 * 
	 * @param initiativeName  - Name of the initiative
	 * @param initiativeTypeId - ID of the type of initiative
	 * @param initiativeCategory - team or individual
	 * @param initiativeStartDate - start date of the initiative
	 * @param initiativeEndDate - end date of the initiative
	 * @param initiativeComment - comments for the initiative
	 * @param filterList - filter list applicable to the initiative
	 * @param ownerOfList - key people assigned to the initiative
	 * @param partOfEmployeeList - applicable only for individual initiative should be null for team initiative list of employees for whom the initiative has been created
	 */
	public void setInitiativeProperties(String initiativeName, int initiativeTypeId, String initiativeCategory, Date initiativeStartDate,
			Date initiativeEndDate, String initiativeComment, List<Filter> filterList, List<Employee> ownerOfList, List<Employee> partOfEmployeeList) {
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Setting initiative properties");
		this.initiativeName = initiativeName;
		this.initiativeTypeId = initiativeTypeId;
		this.initiativeCategory = initiativeCategory;
		this.initiativeStartDate = initiativeStartDate;
		this.initiativeEndDate = initiativeEndDate;
		this.initiativeComment = initiativeComment;
		this.filterList = filterList;
		this.ownerOfList = ownerOfList;
		this.partOfEmployeeList = partOfEmployeeList;
	}

	/**
	 * Creation of the actual initiative happens here
	 * 
	 * @return - initiativeId of the newly created initiative
	 */
	public int create() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		int initiativeId = 0;
		try {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating the initiative");

			String createInitQuery = "match (i:Init)  with CASE count(i) WHEN 0  THEN 1 ELSE max(i.Id)+1 END as uid "
					+ "CREATE (i:Init {Id:uid,Status:'" + checkInitiativeStatus(initiativeStartDate) + "',Name:'" + initiativeName + "',Type:"
					+ initiativeTypeId + ", Category:'" + initiativeCategory + "',StartDate:'" + initiativeStartDate.toString() + "',EndDate:'"
					+ initiativeEndDate.toString() + "',Comment:'" + initiativeComment + "'}) return i.Id as Id";

			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Create initiative query : " + createInitQuery);
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(createInitQuery);
			while (res.next()) {
				initiativeId = res.getInt("Id");
			}
			if (initiativeId > 0) {
				this.initiativeId = initiativeId;

				// Check if the initiative is of the category Individual

				if (this.initiativeCategory.equalsIgnoreCase("Individual")) {
					if (setEmployeesPartOf(initiativeId, this.partOfEmployeeList)) {
						org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting part_of connections for initiative");
					} else {
						org.apache.log4j.Logger.getLogger(Initiative.class).error("Unsuccessful in setting part_of connections for initiative");
					}
				} else if (this.initiativeCategory.equalsIgnoreCase("Team")) {
					if (setPartOf(initiativeId, this.filterList)) {
						org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting part of initiative");
					} else {
						org.apache.log4j.Logger.getLogger(Initiative.class).error("Unsuccessful in setting part of initiative");
					}
				}
				if (!this.ownerOfList.isEmpty() && setOwner(initiativeId, this.ownerOfList)) {
					org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting owner for initiative");
				} else {
					org.apache.log4j.Logger.getLogger(Initiative.class).error("Unsuccessful in setting owner for initiative");
				}

			} else {
				org.apache.log4j.Logger.getLogger(Initiative.class).error("Unable to create initiative");
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception in Create initiative query", e);

		}
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Initiative ID : " + initiativeId);

		return initiativeId;
	}

	/**
	 * Creates the connections with the objects that are part of the initiative
	 * 
	 */
	@SuppressWarnings("unchecked")
	private boolean setPartOf(int initiativeId, List<Filter> filterList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Create Initiative Connections for initiativeId " + initiativeId);
			Map<String, Object> params = new HashMap<>();
			params.put("initiativeId", initiativeId);
			for (int i = 0; i < filterList.size(); i++) {
				Filter f = filterList.get(i);
				params.put(f.getFilterName(), getFilterValueList(f.getFilterValues()));
			}

			// TODO make this dynamic based on filter list
			String funcQuery = "", posQuery = "", zoneQuery = "";
			ArrayList<String> funcParam = (ArrayList<String>) params.get("Function");
			ArrayList<String> zoneParam = (ArrayList<String>) params.get("Zone");
			ArrayList<String> posParam = (ArrayList<String>) params.get("Position");
			if (funcParam.contains(0)) {
				funcQuery = "Match (i:Init),(f:Function) WHERE i.Id = " + initiativeId + " Create f-[:part_of]->i ";
			} else {
				funcQuery = "Match (i:Init),(f:Function) where i.Id = " + initiativeId + " and f.Id in " + funcParam.toString()
						+ " Create f-[:part_of]->i ";
			}

			if (zoneParam.contains(0)) {
				zoneQuery = "Match (i:Init),(z:Zone) where i.Id = " + initiativeId + " create z-[:part_of]->i";

			} else {
				zoneQuery = "Match (i:Init),(z:Zone) where i.Id = " + initiativeId + " and z.Id in " + zoneParam.toString()
						+ " create z-[:part_of]->i";
			}

			if (posParam.contains(0)) {
				posQuery = "Match (i:Init),(p:Position) where i.Id = " + initiativeId + " Create p-[:part_of]->i";

			} else {
				posQuery = "Match (i:Init),(p:Position) where i.Id = " + initiativeId + " and p.Id in " + posParam.toString()
						+ " Create p-[:part_of]->i";

			}

			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Function query : " + funcQuery);
			dch.neo4jCon.createStatement().executeQuery(funcQuery);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Position query : " + posQuery);
			dch.neo4jCon.createStatement().executeQuery(posQuery);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Zone query : " + zoneQuery);
			dch.neo4jCon.createStatement().executeQuery(zoneQuery);

			return true;
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception while setting part of for initiative ID" + initiativeId, e);
			return false;
		}

	}

	/**
	 * Creates the part of connections for initiatives of category Individual
	 * 
	 * @param initiativeId - ID of the initiative for which the part of connections are to be created
	 * @param employeeList - list of employee ID's which are part of the initiative
	 */
	private boolean setEmployeesPartOf(int initiativeId, List<Employee> employeeList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			ArrayList<Integer> empIdList = new ArrayList<>();
			for (Employee e : employeeList) {
				empIdList.add(e.getEmployeeId());
			}

			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating part_of connections for initiative : " + initiativeId);
			String query = "Match (i:Init),(e:Employee) where i.Id = " + initiativeId + " and e.emp_id in " + empIdList.toString()
					+ " Create e-[:part_of]->i";
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating part_of connections query : " + query);
			dch.neo4jCon.createStatement().executeQuery(query);
			return true;
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error(
					"Exception while creating part_of connections for initiative : " + initiativeId, e);
			return false;
		}
	}

	/**
	 * Internal Helper Function 
	 * Get list of string filterValues from a map of filterValues
	 * 
	 */
	private List<Integer> getFilterValueList(Map<Integer, String> filterValues) {
		List<Integer> filterValueStringList = new ArrayList<>();
		filterValueStringList.addAll(filterValues.keySet());
		return filterValueStringList;
	}

	/**
	 * Creates the connections with the employees who are owners of the initiative
	 * 
	 * @param initiativeId - ID of the initiative for which key people are to be assigned for
	 * @param employeeList - List of key people to be assigned to the given initiative
	 * @return true/false based on if the operation was successful
	 */
	private boolean setOwner(int initiativeId, List<Employee> employeeList) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			ArrayList<Integer> empIdList = new ArrayList<>();
			for (Employee e : employeeList) {
				empIdList.add(e.getEmployeeId());
			}
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating connections for initiative : " + initiativeId);
			String query = "Match (i:Init),(e:Employee) where i.Id = " + initiativeId + " and e.emp_id in " + empIdList.toString()
					+ " Create e-[:owner_of]->i";
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Creating connections for initiative query : " + query);
			dch.neo4jCon.createStatement().executeQuery(query);
			return true;
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception while creating owner for initiative : " + initiativeId, e);
			return false;
		}
	}

	/**
	 * Retrieves the single initiative based on the initiativeId given
	 * 
	 * @param initiativeId - ID of the initiative which needs to be retrieved
	 * @return initiative object 
	 */
	public Initiative get(int initiativeId) {
		InitiativeHelper ih = new InitiativeHelper();
		org.apache.log4j.Logger.getLogger(Initiative.class).debug("Retrieving the initiative with initiative ID " + initiativeId);

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Initiative i = new Initiative();
		InitiativeList il = new InitiativeList();
		i.setInitiativeId(initiativeId);
		try {
			String query = "match (o:Employee)-[:owner_of]->(i:Init{Id:"
					+ initiativeId
					+ "})<-[r:part_of]-(a) return i.Name as Name,"
					+ "i.StartDate as StartDate, i.EndDate as EndDate, i.Id as Id,case i.Category when 'Individual' then collect(distinct(a.emp_id)) "
					+ "else collect(distinct(a.Id))  end as PartOfID,collect(distinct(a.Name))as PartOfName, labels(a) as Filters, "
					+ "collect(distinct (o.emp_id)) as OwnersOf,i.Comment as Comments,i.Type as Type,i.Category as Category,i.Status as Status";
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Query : " + query);
			ResultSet res = dch.neo4jCon.createStatement().executeQuery(query);
			res.next();
			il.setInitiativeValues(res, i);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception while retrieving the initiative with ID" + initiativeId, e);

		}
		return i;
	}

	/**
	 * Returns the master list of initiative types based on category
	 * 
	 * @param category - team or individual
	 * @return initiativeTypeMap - Map of initiative types with ID/value
	 */
	public Map<Integer, String> getInitiativeTypeMap(String category) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> initiativeTypeMap = new HashMap<>();
		try {

			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getInitiativeTypeList(?)}");
			cstmt.setString(1, category);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				initiativeTypeMap.put(rs.getInt(1), rs.getString(2));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return initiativeTypeMap;
	}

	/**
	 * Changes the status of the Initiative with the given initiativeId to Deleted
	 * 
	 * @param initiativeId - ID of the initiative to be deleted
	 * @return true/false depending on whether the delete is done or not
	 */
	public boolean delete(int initiativeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		boolean status = false;

		try {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Starting to delete the initiative ID " + initiativeId);
			String query = "match(a:Init {Id:" + initiativeId + "}) set a.Status = 'Deleted' return a.Status as currentStatus";
			dch.neo4jCon.createStatement().executeQuery(query);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Deleted initiative with ID " + initiativeId);
			status = true;

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception in deleting initiative", e);

		}
		return status;
	}

	/**
	 * Updates the given initiative object
	 * 
	 * @param updatedInitiative - The Initiative object to be updated
	 * @return true/false depending on whether the update is done or not
	 */
	public boolean updateInitiative(Initiative updatedInitiative) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		boolean status = false;
		int updatedInitiativeId = updatedInitiative.getInitiativeId();
		try {
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Started update of The initiative with ID " + updatedInitiative.initiativeId);
			List<Employee> updatedOwnerOfList = updatedInitiative.getOwnerOfList();
			String ownersOfQuery = "match(i:Init {Id:" + updatedInitiativeId + "})<-[r:owner_of]-(e:Employee) delete r";
			dch.neo4jCon.createStatement().executeQuery(ownersOfQuery);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Ownersof list deleted from initiative " + updatedInitiative.initiativeId);
			updatedInitiative.setOwner(updatedInitiativeId, updatedOwnerOfList);
			String query = "match(a:Init {Id:" + updatedInitiativeId + "}) set a.Name = '" + updatedInitiative.getInitiativeName().toString()
					+ "',a.Status = '" + checkInitiativeStatus(updatedInitiative.getInitiativeStartDate()) + "'," + "a.Type = '"
					+ updatedInitiative.getInitiativeTypeId() + "',a.Category = '" + updatedInitiative.getInitiativeCategory() + "',"
					+ "a.Comment = '" + updatedInitiative.getInitiativeComment().toString() + "',a.EndDate = '"
					+ updatedInitiative.getInitiativeEndDate().toString() + "'," + "a.StartDate = '"
					+ updatedInitiative.getInitiativeStartDate().toString() + "' return a.Name as Name, " + "a.Type as Type,a.Category as Category, "
					+ "a.Status as Status,a.Comment as Comment,a.EndDate as endDate,a.StartDate as StartDate";
			dch.neo4jCon.createStatement().executeQuery(query);
			status = true;

			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Updated initiative with ID " + updatedInitiativeId);
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception in updating initiative " + updatedInitiativeId, e);
		}
		return status;
	}

	/**
	 * Return the initiative status based on the start date
	 * 
	 * @param initiativeStartDate - start date of the initiative
	 * @return initiative status - Active or Pending
	 */
	private String checkInitiativeStatus(Date initiativeStartDate) {
		String initiativeStatus = "Active";
		if (initiativeStartDate.after(Date.from(Instant.now()))) {
			initiativeStatus = "Pending";
		}
		return initiativeStatus;
	}

	/**
	 * Sets the status of the initiative to completed based on the initiativeId provided in the parameter
	 * 
	 * @param initiativeId - ID of the initiative to be set as completed
	 * @return true/false based on if the action was successful
	 */
	public boolean complete(int initiativeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			String query = "match(a:Init {Id:" + initiativeId + "}) set a.Status = 'Completed'";
			dch.neo4jCon.createStatement().executeQuery(query);
			org.apache.log4j.Logger.getLogger(Initiative.class).debug("Changed the status of initiative with ID " + initiativeId + " to Completed");

			return true;
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(Initiative.class).error("Exception in changing the status to Completed for initiative " + initiativeId,
					e);
		}

		return false;
	}

	public String getInitiativeName() {
		return initiativeName;
	}

	public void setInitiativeName(String initiativeName) {
		this.initiativeName = initiativeName;
	}

	public int getInitiativeTypeId() {
		return initiativeTypeId;
	}

	public void setInitiativeTypeId(int initiativeTypeId) {
		this.initiativeTypeId = initiativeTypeId;
	}

	public Date getInitiativeStartDate() {
		return initiativeStartDate;
	}

	public void setInitiativeStartDate(Date initiativeStartDate) {
		this.initiativeStartDate = initiativeStartDate;
	}

	public Date getInitiativeEndDate() {
		return initiativeEndDate;
	}

	public void setInitiativeEndDate(Date initiativeEndDate) {
		this.initiativeEndDate = initiativeEndDate;
	}

	public String getInitiativeComment() {
		return initiativeComment;
	}

	public void setInitiativeComment(String initiativeComment) {
		this.initiativeComment = initiativeComment;
	}

	public int getInitiativeId() {
		return initiativeId;
	}

	public void setInitiativeId(int initiativeId) {
		this.initiativeId = initiativeId;
	}

	public List<Filter> getFilterList() {
		return filterList;
	}

	public void setFilterList(List<Filter> filterList) {
		this.filterList = filterList;
	}

	public List<Employee> getOwnerOfList() {
		return ownerOfList;
	}

	public void setOwnerOfList(List<Employee> employeeList) {
		this.ownerOfList = employeeList;
	}

	public String getInitiativeCategory() {
		return initiativeCategory;
	}

	public void setInitiativeCategory(String initiativeCategory) {
		this.initiativeCategory = initiativeCategory;
	}

	public String getInitiativeStatus() {
		return initiativeStatus;
	}

	public void setInitiativeStatus(String initiativeStatus) {
		this.initiativeStatus = initiativeStatus;
	}

	public List<Employee> getPartOfEmployeeList() {
		return partOfEmployeeList;
	}

	public void setPartOfEmployeeList(List<Employee> partOfEmployeeList) {
		this.partOfEmployeeList = partOfEmployeeList;
	}

	/**
	 * @return the initiativeMetrics
	 */
	public List<Metrics> getInitiativeMetrics() {
		return initiativeMetrics;
	}

	/**
	 * @param initiativeMetrics the initiativeMetrics to set
	 */
	public void setInitiativeMetrics(List<Metrics> initiativeMetrics) {
		this.initiativeMetrics = initiativeMetrics;
	}
}
