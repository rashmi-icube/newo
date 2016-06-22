package org.icube.owen.jobScheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class EmailSender {

	DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

	public static final int MAX_EMAILS_TO_BE_SENT = 50;
	// private String loginUrl = UtilHelper.getConfigProperty("login_page_url");
	List<String> toAddresses = Arrays.asList("hpatel@i-cube.in", "rashmi@i-cube.in", "ssrivastava@i-cube.in", "adoshi@i-cube.in");

	/**
	 * Sends email for the Scheduler Job
	 * @param schedulerJobStatusMap 
	 * @param subject - The subject of the mail
	 * @param text - The text to be sent in the mail
	 * @throws AddressException - If the email ID is incorrect
	 * @throws MessagingException - If error in sending email
	 */
	public void sendEmail(Map<Integer, List<Map<String, String>>> schedulerJobStatusMap, String subject) throws AddressException, MessagingException {
		String host = "smtp.zoho.com";
		String username = "owen@owenanalytics.com";
		String password = "Abcd@654321";
		String from = "owen@owenanalytics.com";
		Properties props = new Properties();
		props.put("mail.debug", "true");
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", 465);
		Session session = Session.getInstance(props);
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.addRecipients(Message.RecipientType.TO, getEmailsArray(toAddresses));
		msg.setSubject(subject);
		msg.setContent((getEmailTable(schedulerJobStatusMap).toString()), "text/html");
		try {
			Transport.send(msg, username, password);
		} catch (MessagingException e) {
			org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in sending Email", e);
			dch.releaseRcon();
		}
	}

	/**
	 * Builds the email body in html
	 * @param schedulerJobStatusMap - map of company id, job name and status of job
	 * @return a StringBuilder object
	 */
	private StringBuilder getEmailTable(Map<Integer, List<Map<String, String>>> schedulerJobStatusMap) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<table border = 1>");
		sb.append("<body>");
		sb.append("<tr>");
		sb.append("<td><b>Company ID</b></td>");
		sb.append("<td><b>Job Name</b></td>");
		sb.append("<td><b>Status</b></td>");
		sb.append("</tr>");
		for (int companyId : schedulerJobStatusMap.keySet()) {
			List<Map<String, String>> companyStatusList = schedulerJobStatusMap.get(companyId);
			for (int i = 0; i < companyStatusList.size(); i++) {
				Map<String, String> jobStatusMap = companyStatusList.get(i);
				sb.append("<tr>");
				sb.append("<td>");
				sb.append(companyId);
				sb.append("</td>");
				sb.append("<td>");
				sb.append(jobStatusMap.keySet().iterator().next());
				sb.append("</td>");
				sb.append("<td>");
				sb.append(jobStatusMap.values().iterator().next());
				sb.append("</td>");
				sb.append("</tr>");
			}
		}
		sb.append("</body>");
		sb.append("</table>");
		sb.append("</html>");
		return sb;
	}

	/**
	 * Sends the email for active questions
	 * @param addresses - List of email ID's
	 */
	public void sendEmailforQuestions(int companyId, List<String> addresses) {
		String host = "smtp.zoho.com";
		String username = "owen@owenanalytics.com";
		String password = "Abcd@654321";
		String from = "owen@owenanalytics.com";
		Properties props = new Properties();
		props.put("mail.debug", "true");
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", 465);
		Session session = Session.getInstance(props);

		int emailSublist = ((addresses.size() % MAX_EMAILS_TO_BE_SENT) > 0) ? (addresses.size() / MAX_EMAILS_TO_BE_SENT) + 1 : addresses.size()
				/ MAX_EMAILS_TO_BE_SENT;
		int fromlist = 0;
		List<String> addr = new ArrayList<>();
		for (int i = 0; i < emailSublist; i++) {
			if ((fromlist + MAX_EMAILS_TO_BE_SENT) > addresses.size()) {
				addr = addresses.subList(fromlist, addresses.size());
			} else {
				addr = addresses.subList(fromlist, fromlist + MAX_EMAILS_TO_BE_SENT);
			}
			MimeMessage msg = new MimeMessage(session);
			try {
				msg.setFrom(new InternetAddress(from, "OWEN"));
				msg.setRecipients(Message.RecipientType.BCC, getEmailsArray(addr));
				msg.setSubject("You have a new question");
				msg.setContent((getNewQuesMailText().toString()), "text/html");
				try {
					Transport.send(msg, username, password);
				} catch (MessagingException e) {
					org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in sending Email for new question", e);
					dch.releaseRcon();
				}

			} catch (MessagingException | UnsupportedEncodingException e) {
				org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in sending Emails for current questions", e);
				dch.releaseRcon();
			}
			fromlist = fromlist + MAX_EMAILS_TO_BE_SENT;
		}

	}

	private StringBuilder getNewQuesMailText() {
		StringBuilder sb = new StringBuilder();
		String rScriptPath = UtilHelper.getConfigProperty("r_script_path");
		try (BufferedReader in = new BufferedReader(new FileReader(rScriptPath + "\\NewQuestionEmail.html"))) {
			String str;
			while ((str = in.readLine()) != null) {

				sb.append(str);

			}
		} catch (IOException e) {
			org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in building Email for new question", e);
		}
		return sb;
	}

	/**
	 * Retrieves an array of email ID's
	 * @param addr - List of email ID's
	 * @return - An array of email ID's
	 */
	private Address[] getEmailsArray(List<String> addr) {
		Address[] emailAddresses = new Address[addr.size()];
		for (int i = 0; i < addr.size(); i++) {
			try {
				emailAddresses[i] = new InternetAddress(addr.get(i));
			} catch (AddressException e) {
				org.apache.log4j.Logger.getLogger(EmailSender.class).error("Unable to retrieve email addresses", e);
			}
		}
		return emailAddresses;

	}

	/**
	 * Sends the email for new password
	 * @param lastName 
	 * @param firstName 
	 * @param address - email id of the employee
	 * @param newPassword - the new generated password
	 * @throws AddressException - if email id is not valid
	 * @throws MessagingException - if unable to send email
	 */
	public void sendNewPasswordEmail(String firstName, String lastName, List<String> address, String newPassword) throws AddressException,
			MessagingException {
		String host = "smtp.zoho.com";
		String username = "support@owenanalytics.com";
		String password = "Abcd@654321";
		String from = "support@owenanalytics.com";
		Properties props = new Properties();
		props.put("mail.debug", "true");
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", 465);
		Session session = Session.getInstance(props);
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.addRecipients(Message.RecipientType.TO, getEmailsArray(address));
		msg.setSubject("New Password");
		msg.setContent(getPasswordemailText(firstName, lastName, newPassword).toString(), "text/html");
		try {
			Transport.send(msg, username, password);
		} catch (MessagingException e) {
			org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in sending Email", e);
			dch.releaseRcon();
		}
	}

	/**
	 * Builds the email content for the forgot password
	 * @param newPassword - new random password
	 * @param newPassword2 
	 * @param lastName 
	 * @return String Builder object
	 */
	private StringBuilder getPasswordemailText(String firstName, String lastName, String newPassword) {
		StringBuilder sb = new StringBuilder();
		String rScriptPath = UtilHelper.getConfigProperty("r_script_path");
		try (BufferedReader in = new BufferedReader(new FileReader(rScriptPath + "\\ForgotPassword.html"))) {
			String str;
			while ((str = in.readLine()) != null) {
				if (str.contains("<P style=\"MARGIN-BOTTOM: 1em;\"><B>Password: </B>password</P>")) {
					sb.append("<P style=\"MARGIN-BOTTOM: 1em;\"><B>Password: </B> " + newPassword + "</P>");
				}

				else if (str.contains("Use this temporary password to sign into your account.")) {
					sb.append("<P style=\"MARGIN-BOTTOM: 14px; MIN-HEIGHT: 20px\">Hi <B style=\"color:#388E3C;\"> " + firstName + " " + lastName
							+ "</B>,<br>Use this temporary password to sign into your account.</P>");
				} else {
					sb.append(str);
				}
			}
		} catch (IOException e) {
			org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in building Email for new password", e);
		}
		return sb;
	}
}
