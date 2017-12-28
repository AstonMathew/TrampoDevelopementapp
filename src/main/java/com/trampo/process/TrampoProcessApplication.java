package com.trampo.process;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TrampoProcessApplication{

  public static void main(String[] args) {
    SpringApplication.run(TrampoProcessApplication.class, args);
  }
}
