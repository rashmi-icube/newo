package org.icube.owen.dashboard;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.metrics.Metrics;

public class Alert extends TheBorg {

	private int alertId;
	private String alertMessage;
	private String alertStatus;
	private List<Filter> filterList;
	private List<Employee> employeeList;
	private Metrics alertMetric;
	private int initiativeTypeId;
	private double deltaScore;
	private int teamSize;

	// private String cubeName;

	/**
	 * Get populated alert object based on the alertId
	 * @param alertId - ID of the alert for which the data is required
	 * @return alert object
	 */
	public Alert get(int alertId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Alert a = null;
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getAlert(?)}");
			cstmt.setInt(1, alertId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				a = fillAlertDetails(rs);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Alert.class).error("Exception while retrieving alert with ID : " + alertId, e);
		}
		return a;
	}

	/**
	 * Helper method to fill alert object from database query
	 * @param rs - result from the database query
	 * @return alert object
	 * @throws SQLException
	 */
	public Alert fillAlertDetails(ResultSet rs) throws SQLException {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		String zone = "", function = "", position = "";
		List<Filter> filterList = new ArrayList<>();
		List<Employee> employeeList = new ArrayList<>();
		Alert a = new Alert();
		a.setAlertId(rs.getInt("alert_id"));

		for (int i = 1; i <= 3; i++) {
			Filter f = new Filter();
			f.setFilterId(rs.getInt("dimension_id_" + i));
			f.setFilterName(rs.getString("dimension_name_" + i));
			Map<Integer, String> filterValuesMap = new HashMap<>();
			filterValuesMap.put(rs.getInt("dimension_val_id_" + i), rs.getString("dimension_val_name_" + i));
			f.setFilterValues(filterValuesMap);
			filterList.add(f);

			if (f.getFilterName().equalsIgnoreCase("zone")) {
				zone = f.getFilterValues().values().iterator().next();
			} else if (f.getFilterName().equalsIgnoreCase("function")) {
				function = f.getFilterValues().values().iterator().next();
			} else if (f.getFilterName().equalsIgnoreCase("position")) {
				position = f.getFilterValues().values().iterator().next();
			}
		}

		a.setFilterList(filterList);
		a.setAlertMessage(String.format(rs.getString("alert_statement"), zone, function, position));
		Metrics m = new Metrics();
		m.setId(rs.getInt("metric_id"));
		m.setName(rs.getString("metric_name"));
		m.setScore(rs.getInt("score"));
		m.setDateOfCalculation(rs.getDate("calc_time"));
		m.setCategory(rs.getString("category"));
		m.setDirection(rs.getDouble("delta_score") > 0 ? "Positive" : "Negative");
		a.setAlertMetric(m);
		a.setDeltaScore(rs.getDouble("delta_score"));
		a.setTeamSize(rs.getInt("team_size"));
		CallableStatement cstmt1 = dch.mysqlCon.prepareCall("{call getListOfPeopleForAlert(?)}");
		cstmt1.setInt(1, rs.getInt("alert_id"));
		ResultSet rs1 = cstmt1.executeQuery();
		while (rs1.next()) {
			Employee e = new Employee();
			employeeList.add(e.get(rs1.getInt("emp_id")));
		}
		a.setEmployeeList(employeeList);
		a.setAlertStatus(rs.getString("status"));
		a.setInitiativeTypeId(rs.getInt("init_type_id"));
		return a;
	}

	/**
	 * Deletes the alert 
	 * @return boolean value if the alert has been deleted or not
	 */
	public boolean delete() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call deleteAlert(?)}");
			cstmt.setInt(1, alertId);
			cstmt.executeQuery();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(Alert.class).error("Exception while deleting alert with ID : " + alertId, e);
		}
		return true;

	}

	public int getAlertId() {
		return alertId;
	}

	public void setAlertId(int alertId) {
		this.alertId = alertId;
	}

	public String getAlertMessage() {
		return alertMessage;
	}

	public void setAlertMessage(String alertMessage) {
		this.alertMessage = alertMessage;
	}

	public String getAlertStatus() {
		return alertStatus;
	}

	public void setAlertStatus(String alertStatus) {
		this.alertStatus = alertStatus;
	}

	public List<Filter> getFilterList() {
		return filterList;
	}

	public void setFilterList(List<Filter> filterList) {
		this.filterList = filterList;
	}

	public List<Employee> getEmployeeList() {
		return employeeList;
	}

	public void setEmployeeList(List<Employee> employeeList) {
		this.employeeList = employeeList;
	}

	public Metrics getAlertMetric() {
		return alertMetric;
	}

	public void setAlertMetric(Metrics alertMetric) {
		this.alertMetric = alertMetric;
	}

	public int getInitiativeTypeId() {
		return initiativeTypeId;
	}

	public void setInitiativeTypeId(int initiativeTypeId) {
		this.initiativeTypeId = initiativeTypeId;
	}

	public double getDeltaScore() {
		return deltaScore;
	}

	public void setDeltaScore(double deltaScore) {
		this.deltaScore = deltaScore;
	}

	public int getTeamSize() {
		return teamSize;
	}

	public void setTeamSize(int teamSize) {
		this.teamSize = teamSize;
	}

}
