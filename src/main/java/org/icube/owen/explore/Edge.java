package org.icube.owen.explore;

public class Edge {

	private int fromEmployeId;
	private int toEmployeeId;
	private String relationshipType;
	private double weight;

	public int getFromEmployeId() {
		return fromEmployeId;
	}

	public void setFromEmployeId(int fromEmployeId) {
		this.fromEmployeId = fromEmployeId;
	}

	public int getToEmployeeId() {
		return toEmployeeId;
	}

	public void setToEmployeeId(int toEmployeeId) {
		this.toEmployeeId = toEmployeeId;
	}

	public String getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(String relationshipType) {
		this.relationshipType = relationshipType;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

}
