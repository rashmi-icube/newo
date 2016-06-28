package org.icube.owen.helper;

import java.sql.Connection;

public class CompanyConfig {

	private Connection sqlConnection;
	private Connection neoConnection;
	private String imagePath;
	private String slackUrl;
	private boolean sendSlack;
	private boolean sendEmail;
	private boolean displayNetworkName;
	private String smartList;

	public Connection getSqlConnection() {
		return sqlConnection;
	}

	public void setSqlConnection(Connection sqlConnection) {
		this.sqlConnection = sqlConnection;
	}

	public Connection getNeoConnection() {
		return neoConnection;
	}

	public void setNeoConnection(Connection neoConnection) {
		this.neoConnection = neoConnection;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getSlackUrl() {
		return slackUrl;
	}

	public void setSlackUrl(String slackUrl) {
		this.slackUrl = slackUrl;
	}

	public boolean isSendSlack() {
		return sendSlack;
	}

	public void setSendSlack(boolean sendSlack) {
		this.sendSlack = sendSlack;
	}

	public boolean isSendEmail() {
		return sendEmail;
	}

	public void setSendEmail(boolean sendEmail) {
		this.sendEmail = sendEmail;
	}

	public boolean isDisplayNetworkName() {
		return displayNetworkName;
	}

	public void setDisplayNetworkName(boolean displayNetworkName) {
		this.displayNetworkName = displayNetworkName;
	}

	public String getSmartList() {
		return smartList;
	}

	public void setSmartList(String smartList) {
		this.smartList = smartList;
	}
}
