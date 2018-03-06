package com.trampo.process;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import com.trampo.process.domain.CcmPlus;

@EnableScheduling
@SpringBootApplication
public class TrampoProcessApplication {

  public static void main(String[] args) {
    SpringApplication.run(TrampoProcessApplication.class, args);
  }

//  @Component
//  public static class Runner implements CommandLineRunner {
//
//    private TrampoConfig config;
//
//    @Autowired
//    public Runner(TrampoConfig config) {
//      this.config = config;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//      List<CcmPlus> list = config.getCcmplus();
//      for (CcmPlus ccmPlus : list) {
//        System.err.println(ccmPlus.getPath());
//        System.err.println(ccmPlus.getVersion());
//        System.err.println(ccmPlus.getPrecision());
//      }
//    }
//  }
}
