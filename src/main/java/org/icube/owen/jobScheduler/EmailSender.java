package org.icube.owen.jobScheduler;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

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

}
