package org.icube.owen.helper;

public class CompanyConfig {

	private String imagePath;
	private String slackUrl;
	private boolean sendSlack;
	private boolean sendEmail;
	private boolean displayNetworkName;
	private String smartList;
	private String status;

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
}
