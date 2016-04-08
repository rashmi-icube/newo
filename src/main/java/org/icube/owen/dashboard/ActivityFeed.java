package org.icube.owen.dashboard;

import java.util.Date;

public class ActivityFeed {

	private String activityType;
	private String headerText;
	private String bodyText;
	private Date date;

	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}

	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public String getBodyText() {
		return bodyText;
	}

	public void setBodyText(String bodyText) {
		this.bodyText = bodyText;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
