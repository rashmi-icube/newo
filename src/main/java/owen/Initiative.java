package owen;

import java.util.ArrayList;

public class Initiative {

	private int initiativeId;
	private String initiativeName = "";
	private String initiativeType = "";
	private String initiativeStartDate = "";
	private String initiativeEndDate = "";
	private String initiativeComment = "";
	private ArrayList<String> funcList;
	private ArrayList<String> zoneList;
	private ArrayList<String> posList;
	private ArrayList<String> empIdList;

	public String getInitiativeName() {
		return initiativeName;
	}

	public void setInitiativeName(String initiativeName) {
		this.initiativeName = initiativeName;
	}

	public String getInitiativeType() {
		return initiativeType;
	}

	public void setInitiativeType(String initiativeType) {
		this.initiativeType = initiativeType;
	}

	public String getInitiativeStartDate() {
		return initiativeStartDate;
	}

	public void setInitiativeStartDate(String initiativeStartDate) {
		this.initiativeStartDate = initiativeStartDate;
	}

	public String getInitiativeEndDate() {
		return initiativeEndDate;
	}

	public void setInitiativeEndDate(String initiativeEndDate) {
		this.initiativeEndDate = initiativeEndDate;
	}

	public String getInitiativeComment() {
		return initiativeComment;
	}

	public void setInitiativeComment(String initiativeComment) {
		this.initiativeComment = initiativeComment;
	}

	public ArrayList<String> getFuncList() {
		return funcList;
	}

	public void setFuncList(ArrayList<String> funcList) {
		this.funcList = funcList;
	}

	public ArrayList<String> getZoneList() {
		return zoneList;
	}

	public void setZoneList(ArrayList<String> zoneList) {
		this.zoneList = zoneList;
	}

	public ArrayList<String> getPosList() {
		return posList;
	}

	public void setPosList(ArrayList<String> posList) {
		this.posList = posList;
	}

	public ArrayList<String> getEmpIdList() {
		return empIdList;
	}

	public void setEmpIdList(ArrayList<String> empIdList) {
		this.empIdList = empIdList;
	}

	public int getInitiativeId() {
		return initiativeId;
	}

	public void setInitiativeId(int initiativeId) {
		this.initiativeId = initiativeId;
	}

}
