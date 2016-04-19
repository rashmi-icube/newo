package org.icube.owen.jobScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
	public static final int MAX_EMAILS_TO_BE_SENT = 50;

	public void sendEmail(String text) throws AddressException, MessagingException {
		String host = "smtp.zoho.com";
		String username = "info@i-cube.in";
		String password = "test1234";
		String from = "info@i-cube.in";
		String to = "hpatel@i-cube.in";
		Properties props = new Properties();
		props.put("mail.debug", "true");
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", 465);
		Session session = Session.getInstance(props);
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		msg.addRecipient(Message.RecipientType.CC, new InternetAddress("rashmi@i-cube.in"));
		msg.addRecipient(Message.RecipientType.CC, new InternetAddress("ssrivastava@i-cube.in"));
		msg.addRecipient(Message.RecipientType.CC, new InternetAddress("adoshi@i-cube.in"));
		msg.setSubject("subject");
		msg.setText(text);

		// set the message content here
		try {
			Transport.send(msg, username, password);
		} catch (MessagingException e) {
			org.apache.log4j.Logger.getLogger(EmailSender.class).error("Unable to send Email", e);
		}

	}

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
				org.apache.log4j.Logger.getLogger(EmailSender.class).error("Error in sending Emails", e);
			}
			fromlist = fromlist + MAX_EMAILS_TO_BE_SENT;
		}

	}

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
