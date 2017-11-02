package com.trampo.process.util;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendEmail {
	/** Note: Only work if the setting "Access for less secure apps" is ON in gmail 
	 * Access at https://myaccount.google.com/security 
         * set up dual factor authentiocation in account and generate app password. register app. put password in code.
         */
    
	public static final String TO = "gui@trampocfd.com";
        private final String _username = "trampointernalnotifications@gmail.com";
        private final String _password = "izcdreavdjooopxv";
	
	Properties _props = null;
	Session _session = null;
	
	public SendEmail() {
	}
	
	public void send(String to, String subject, String text) {
		try {
			_props = new Properties();
			_props.put("mail.smtp.auth", "true");
			_props.put("mail.smtp.starttls.enable", "true");
			_props.put("mail.smtp.host", "smtp.gmail.com");
			_props.put("mail.smtp.port", "587");
			

			_session = Session.getInstance(_props,
			  new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(_username, _password);
				}
			  });

			Message message = new MimeMessage(_session);
			message.setFrom(new InternetAddress(_username));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(to));
			message.setSubject(subject);
			message.setText(text);

			Transport.send(message);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

}
