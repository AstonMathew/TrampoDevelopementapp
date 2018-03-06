package com.trampo.process.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.trampo.process.domain.Job;
import com.trampo.process.domain.JobStatus;
import com.trampo.process.util.FileUtils;

@Component
public class JobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

  private SshService sshService;

  @Autowired
  public JobService(SshService sshService) {
    this.sshService = sshService;
  }

  public List<Job> getCurrentJobs() throws JSchException, IOException {
    BufferedReader in = sshService.execCommand("qstat -u gj5914 -x;");
    List<Job> list = new ArrayList<Job>();
    String str = null;
    while ((str = in.readLine()) != null) {
      LOGGER.info("current running jobs script output lne: " + str);
      if (StringUtils.hasText(str) && !str.startsWith("r-man2") && !str.startsWith("Job ID")
          && !str.startsWith("----") && !str.contains("Req'd")) {
        String[] line = str.split(" ");
        int fieldNumber = 0;
        int i = 0;
        String jobId = "";
        String queue = "";
        String simulationId = "";
        String status = "";
        String actualWalltime = "";
        while (i < line.length) {
          LOGGER.info("line[" + i + "]: " + line[i]);
          if (StringUtils.hasText(line[i]) && fieldNumber == 0) {
            LOGGER.info("fieldNumber 0 set to jobId " + line[i]);
            jobId = line[i];
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 1) {
            LOGGER.info("fieldNumber 1 set to nothing " + line[i]);
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 2) {
            LOGGER.info("fieldNumber 2 set to queue " + line[i]);
            queue = line[i];
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 3) {
            LOGGER.info("fieldNumber 3 set to simulationId " + line[i]);
            simulationId = line[i];
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 4) {
            LOGGER.info("fieldNumber 4 set to nothing " + line[i]);
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 5) {
            LOGGER.info("fieldNumber 5 set to nothing " + line[i]);
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 6) {
            LOGGER.info("fieldNumber 6 set to nothing " + line[i]);
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 7) {
            LOGGER.info("fieldNumber 7 set to nothing " + line[i]);
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 8) {
            LOGGER.info("fieldNumber 8 set to nothing " + line[i]);
            fieldNumber++;
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 9) {
            LOGGER.info("fieldNumber 9 set to status " + line[i]);
            fieldNumber++;
            status = line[i];
          } else if (StringUtils.hasText(line[i]) && fieldNumber == 10) {
            LOGGER.info("fieldNumber 10 set to actualwalltime " + line[i]);
            fieldNumber++;
            if (line[i].contains(":")) {
              actualWalltime = line[i];
            } else {
              actualWalltime = "00:00:00";
            }
          }
          i++;
        }

        LOGGER.info("jobId: " + jobId);
        LOGGER.info("queue: " + queue);
        LOGGER.info("simulationId: " + simulationId);
        LOGGER.info("status: " + status);
        LOGGER.info("actualWalltime: " + actualWalltime);
        Job job = new Job();
        job.setSimulationId(simulationId);
        job.setQueue(queue);
        job.setStatus(JobStatus.valueOf(status));
        job.setId(jobId);
        job.setWalltime(actualWalltime);
        list.add(job);
      }
    }
    return list;
  }


  public void cancelJob(String jobId) throws JSchException, IOException {
    String command = "qdel " + jobId;
    LOGGER.info("cancel job command: " + command);
    BufferedReader in = sshService.execCommand(command);
    LOGGER.info("cancelling job ");
    String str = null;
    while ((str = in.readLine()) != null) {
      LOGGER.info(str);
    }
    LOGGER.info("cancelling job fnished");
  }

  public void submitJob(String jobName, String cpuCount, String memory, String queueType,
      String scriptPath, String walltime, String raijinLogRoot, String macroPath,
      String simulationPath, String podKey, String customerDataRoot, String customerRunRoot,
      String runRoot, String starCcmPlusVersion, Boolean meshOnly, Boolean runOnly)
      throws JSchException, IOException, InterruptedException {

    FileUtils.runChmod(customerRunRoot);
    FileUtils.runChmod(customerDataRoot);
    // No spaces in qsub command
    String command = "cd " + runRoot + "; qsub -N " + jobName + " -q " + queueType + " -lncpus="
        + cpuCount + " -e " + raijinLogRoot + "/out.err -o " + raijinLogRoot + "/out.out -lmem="
        + memory + "GB -lwalltime=" + walltime + " -v StarCcmRunVersionPath=" + starCcmPlusVersion
        + ",MacroPath=" + macroPath + ",SimulationPath=" + simulationPath + ",Podkey=" + podKey
        + ",meshOnly=" + meshOnly + ",runOnly=" + runOnly + " " + scriptPath + ";";
    LOGGER.info("submit job command: " + command);
    BufferedReader in = sshService.execCommand(command);
    LOGGER.info("submitting job ");
    String str = null;
    while ((str = in.readLine()) != null) {
      LOGGER.info(str);
    }
    LOGGER.info("submitting job fnished");
  }
}
