package com.trampo.process.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.trampo.process.domain.Customer;
import com.trampo.process.domain.Job;
import com.trampo.process.domain.Simulation;

@Service
public class MailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

  @Autowired
  private JavaMailSender javaMailSender;

  @Autowired
  private CustomerService customerService;

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

  public void sendSimulationStartedEmails(Simulation simulation, Job job) {
    try {
      Customer customer = customerService.getCustomer(simulation.getCustomerId());

      send(customer.getEmail(),
          "Simulation started to run!!", "Your simulation started to run. Simulation folder name: "
              + simulation.getFolderName() + " job id: " + job.getId(),
          "externalnotification@trampocfd.com");

      send("gui@trampocfd.com", "Simulation started to run!!",
          "Simulation started to run for customer id: " + customer.getId() + " email: "
              + customer.getEmail() + " simulation folder name: " + simulation.getFolderName()
              + " job id: " + job.getId(),
          "internalnotification@trampocfd.com");

      send("yeldanumit@gmail.com", "Simulation started to run!!",
          "Simulation started to run for customer id: " + customer.getId() + " email: "
              + customer.getEmail() + " simulation folder name: " + simulation.getFolderName()
              + " job id: " + job.getId(),
          "internalnotification@trampocfd.com");
    } catch (Exception e) {
      LOGGER.error("Error while sending simulation started email!!!", e);
    }
  }

  public void sendSimulationCompletedEmails(Simulation simulation, Job job) {
    try {
      Customer customer = customerService.getCustomer(simulation.getCustomerId());

      send(customer.getEmail(), "Simulation completed successfully!!",
          "Your simulation completed successfully. Simulation folder name: "
              + simulation.getFolderName() + " job id: " + job.getId(),
          "externalnotification@trampocfd.com");

      send("gui@trampocfd.com", "Simulation completed successfully!!",
          "Simulation completed successfully for customer id: " + customer.getId() + " email: "
              + customer.getEmail() + " simulation folder name: " + simulation.getFolderName()
              + " job id: " + job.getId(),
          "internalnotification@trampocfd.com");

      send("yeldanumit@gmail.com", "Simulation completed successfully!!",
          "Simulation completed successfully for customer id: " + customer.getId() + " email: "
              + customer.getEmail() + " simulation folder name: " + simulation.getFolderName()
              + " job id: " + job.getId(),
          "internalnotification@trampocfd.com");
    } catch (Exception e) {
      LOGGER.error("Error while sending simulation completed email!!!", e);
    }
  }

  public void sendFileUploadCompletedEmails(Simulation simulation) {
    try {
      Customer customer = customerService.getCustomer(simulation.getCustomerId());

      send(customer.getEmail(), "File upload completed!!",
          "File upload completed for your simulation. Simulation folder name: "
              + simulation.getFolderName(),
          "externalnotification@trampocfd.com");

      send("gui@trampocfd.com", "File upload completed!!",
          "File upload completed for customer id: " + customer.getId() + " email: "
              + customer.getEmail() + " simulation folder name: " + simulation.getFolderName(),
          "internalnotification@trampocfd.com");

      send("yeldanumit@gmail.com", "File upload completed!!",
          "File upload completed for customer id: " + customer.getId() + " email: "
              + customer.getEmail() + " simulation folder name: " + simulation.getFolderName(),
          "internalnotification@trampocfd.com");
    } catch (Exception e) {
      LOGGER.error("Error while sending simulation completed email!!!", e);
    }
  }

  public void sendSimulationErrorEmails(Simulation simulation, Job job, String errorMessage) {
    try {
      Customer customer = customerService.getCustomer(simulation.getCustomerId());

      String body = "simulation finished with error message: " + errorMessage
          + " Simulation folder name: " + simulation.getFolderName();
      if (job != null) {
        body = body + " job id: " + job.getId();
      }


      send(customer.getEmail(), "Simulation in error!!", body,
          "externalnotification@trampocfd.com");

      send("gui@trampocfd.com", "Simulation in error!!", body,
          "internalnotification@trampocfd.com");

      send("yeldanumit@gmail.com", "Simulation in error!!", body,
          "internalnotification@trampocfd.com");
    } catch (Exception e) {
      LOGGER.error("Error while sending simulation completed email!!!", e);
    }
  }

  public void sendJobHeldEmails(Simulation simulation, Job job) {
    try {
      String body = "Job is held. Job Id: " + job.getId();
      send("gui@trampocfd.com", "Job is held!!", body, "internalnotification@trampocfd.com");
      send("yeldanumit@gmail.com", "Job is held!!", body, "internalnotification@trampocfd.com");
    } catch (Exception e) {
      LOGGER.error("Error while sendJobHeldEmails!!!", e);
    }
  }
  
  public void sendErrorEmails(String body) {
    try {
      send("gui@trampocfd.com", "Error!!", body, "internalnotification@trampocfd.com");
      send("yeldanumit@gmail.com", "Error!!", body, "internalnotification@trampocfd.com");
    } catch (Exception e) {
      LOGGER.error("Error while sendErrorEmails!!!", e);
    }
  }
}
