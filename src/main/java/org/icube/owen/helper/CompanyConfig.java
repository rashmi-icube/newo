package org.icube.owen.helper;

public class CompanyConfig {

	private String imagePath;
	private String slackUrl;
	private boolean sendSlack;
	private boolean sendEmail;
	private boolean displayNetworkName;
	private String smartList;
	private String status;
	private String sqlUrl;
	private String sqlUserName;
	private String sqlPassword;
	private String neoUrl;
	private String neoUserName;
	private String neoPassword;
	private boolean runJobs;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSqlUrl() {
		return sqlUrl;
	}

	public void setSqlUrl(String sqlUrl) {
		this.sqlUrl = sqlUrl;
	}

	public String getSqlUserName() {
		return sqlUserName;
	}

	public void setSqlUserName(String sqlUserName) {
		this.sqlUserName = sqlUserName;
	}

	public String getSqlPassword() {
		return sqlPassword;
	}

	public void setSqlPassword(String sqlPassword) {
		this.sqlPassword = sqlPassword;
	}

	public String getNeoUrl() {
		return neoUrl;
	}

	public void setNeoUrl(String neoUrl) {
		this.neoUrl = neoUrl;
	}

	public String getNeoUserName() {
		return neoUserName;
	}

	public void setNeoUserName(String neoUserName) {
		this.neoUserName = neoUserName;
	}

	public String getNeoPassword() {
		return neoPassword;
	}

	public void setNeoPassword(String neoPassword) {
		this.neoPassword = neoPassword;
	}

	public boolean isRunJobs() {
		return runJobs;
	}

	public void setRunJobs(boolean runJobs) {
		this.runJobs = runJobs;
	}

}
