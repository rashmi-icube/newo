package org.icube.owen.jobScheduler;

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

public class EmailSender {

	DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

	public static final int MAX_EMAILS_TO_BE_SENT = 50;
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
		String username = "info@i-cube.in";
		String password = "test1234";
		String from = "info@i-cube.in";
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
	public void sendEmailforQuestions(List<String> addresses) {
		String host = "smtp.zoho.com";
		String username = "info@i-cube.in";
		String password = "test1234";
		String from = "info@i-cube.in";
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
				msg.setFrom(new InternetAddress(from));
				msg.setRecipients(Message.RecipientType.BCC, getEmailsArray(addr));
				msg.setSubject("You have a new question");
				msg.setText("You have new questions to answer.Please login to answer: http://ec2-52-35-113-15.us-west-2.compute.amazonaws.com:8080/OWENWeb/individual/login.jsp");
				Transport.send(msg, username, password);
			} catch (MessagingException e) {
				org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in sending Emails for current questions", e);
				dch.releaseRcon();
			}
			fromlist = fromlist + MAX_EMAILS_TO_BE_SENT;
		}

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
}
