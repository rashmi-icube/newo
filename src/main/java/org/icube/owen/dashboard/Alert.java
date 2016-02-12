package org.icube.owen.dashboard;

import java.util.List;

import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.metrics.Metrics;

public class Alert {

	private List<Filter> filterList;
	private List<Employee> employeeList;
	private String alertMessage;
	private String cubeName;
	private Metrics alertMetric;
	private int initiativeTypeId;
	private String alertStatus;
	
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
	public String getAlertMessage() {
		return alertMessage;
	}
	public void setAlertMessage(String alertMessage) {
		this.alertMessage = alertMessage;
	}
	public String getCubeName() {
		return cubeName;
	}
	public void setCubeName(String cubeName) {
		this.cubeName = cubeName;
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
	public String getAlertStatus() {
		return alertStatus;
	}
	public void setAlertStatus(String alertStatus) {
		this.alertStatus = alertStatus;
	}
	
	public Alert get(){
		Alert a = new Alert();
		
		//call SQL procedure to get all alert details
		
		//fill in alert object with details retrieved from SQL
		
		return a;
	}
	
	public boolean changeStatus(Alert a , String status){
		a.setAlertStatus(status);
		//write sql/neo4j query to store the status
		
		return true;
		
	}
	
}
