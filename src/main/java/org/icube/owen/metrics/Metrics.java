package org.icube.owen.metrics;

import java.sql.Date;

import org.icube.owen.TheBorg;

public class Metrics extends TheBorg {

	private String name;
	private String category;
	private int score;
	private double average;
	private boolean primary;
	private Date dateOfCalculation;
	private String direction; //can have values Positive/Negative/Neutral depending upon change from previous value

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * @return the dateOfCalculation
	 */
	public Date getDateOfCalculation() {
		return dateOfCalculation;
	}

	/**
	 * @param dateOfCalculation the dateOfCalculation to set
	 */
	public void setDateOfCalculation(Date dateOfCalculation) {
		this.dateOfCalculation = dateOfCalculation;
	}

	/**
	 * @return the direction
	 */
	public String getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setDirection(String direction) {
		this.direction = direction;
	}

}
