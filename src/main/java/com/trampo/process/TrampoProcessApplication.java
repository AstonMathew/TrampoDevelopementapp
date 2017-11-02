package com.trampo.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.trampo.process.domain.Simulation;
import com.trampo.process.domain.SimulationStatus;
import com.trampo.process.service.SimulationService;

@EnableScheduling
@SpringBootApplication
public class TrampoProcessApplication implements CommandLineRunner{

  private static final Logger LOGGER = LoggerFactory.getLogger(TrampoProcessApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(TrampoProcessApplication.class, args);
  }

  @Autowired
  SimulationService simulationService;
  
  @Override
  public void run(String... arg0) throws Exception {
//    LOGGER.warn("Starting");
    
//    List<Simulation> list = simulationService.getByStatus(SimulationStatus.NEW);
//    
//    for (Simulation simulation : list) {
//      LOGGER.warn("id: " + simulation.getId());
//      LOGGER.warn("simulation: " + simulation.toString());
//    }
    
//    String host = "raijin.nci.org.au";
//    int    port = 22;
//    String username = "gj5914";
//    String dir = "/g/data3/uo95/test";
//    String privateKeyPath = "/home/guillaume/.ssh/id_rsa";
//    
//    JSch jsch=new JSch();
//    jsch.addIdentity(privateKeyPath); 
//    Session session=jsch.getSession(username, host, port);
//    session.setConfig("PreferredAuthentications", "publickey");
//    Properties config = new Properties();
//    config.put("StrictHostKeyChecking", "no");
//    session.setConfig(config);
//    session.connect();
//
//    ChannelExec channel=(ChannelExec) session.openChannel("exec");
//    BufferedReader in=new BufferedReader(new InputStreamReader(channel.getInputStream()));
    //channel.setCommand("pwd;ls;cd ..;ls;pwd;");
//    channel.setCommand("qstat -u gj5914;");
//    channel.connect();

    /*23-10-2017 17:10:32.555 [main] WARN  com.trampo.process.TrampoProcessApplication.run - Starting
23-10-2017 17:10:33.966 [main] WARN  com.trampo.process.TrampoProcessApplication.run -
23-10-2017 17:10:33.967 [main] WARN  com.trampo.process.TrampoProcessApplication.run - r-man2:
23-10-2017 17:10:33.967 [main] WARN  com.trampo.process.TrampoProcessApplication.run -                                                             Req'd  Req'd   Elap
23-10-2017 17:10:33.967 [main] WARN  com.trampo.process.TrampoProcessApplication.run - Job ID          Username Queue    Jobname    SessID NDS TSK Memory Time  S Time
23-10-2017 17:10:33.967 [main] WARN  com.trampo.process.TrampoProcessApplication.run - --------------- -------- -------- ---------- ------ --- --- ------ ----- - -----
23-10-2017 17:10:33.968 [main] WARN  com.trampo.process.TrampoProcessApplication.run - 62225.r-man2    gj5914   normal-d Benchmark1   4826  32 512  960gb 00:05 R 00:00
*/
    
//    String msg=null;
//    while((msg=in.readLine())!=null){
//      LOGGER.warn(msg);
//    }
//
//    channel.disconnect();
//    session.disconnect();
  }
}
