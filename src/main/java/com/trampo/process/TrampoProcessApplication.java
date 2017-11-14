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
import com.trampo.process.service.JobService;
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
  
  @Autowired
  JobService jobService;
  
  @Override
  public void run(String... arg0) throws Exception {
    String scriptPath = "/g/data3/uo95/backend/scripts/BackendScript_33_run.sh";
    String cpuCount = "16";
    String memory = "10";
    String jobName = "TestJob4";
    String queueType = "normal";
    String walltime = "000:30:00";
    String root = "/data/backend/run";
    String macroPath = "/g/data3/uo95/TrampoParisSynchronisedFolder/benchmark/LeMans_100M/inputFiles/benchmark.java";
    String simulationPath = "/g/data3/uo95/TrampoParisSynchronisedFolder/benchmark/LeMans_100M/inputFiles/LeMans_100M.sim";
    String podKey = "CyG5P7Hx5gAluUz0YzWNqg"; 
    LOGGER.info("starting");
    jobService.submitJob(jobName, cpuCount, memory, queueType, scriptPath, walltime, root, 
        macroPath, simulationPath, podKey);
    LOGGER.info("finished");
  }
}
