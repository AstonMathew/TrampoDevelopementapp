package com.trampo.process.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

  @Autowired
  private JavaMailSender javaMailSender;

  public void send(String to, String subject, String text, String from) {
    try {
      SimpleMailMessage mail = new SimpleMailMessage();
      mail.setFrom(from);
      mail.setTo(to);
      mail.setSubject(subject);
      mail.setText(text);
      javaMailSender.send(mail);
    } catch (Exception e) {
      LOGGER.error("Error while sending email", e);
    }
  }
}
