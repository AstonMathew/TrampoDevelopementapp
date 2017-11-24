package com.trampo.process.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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

@Component
public class JobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

  private Session session;

  @Autowired
  public JobService(@Value("${trampo.simulation.host}") String host,
      @Value("${trampo.simulation.port}") int port,
      @Value("${trampo.simulation.username}") String username,
      @Value("${trampo.simulation.privateKeyPath}") String privateKeyPath) {
    JSch jsch = new JSch();
    try {
      jsch.addIdentity(privateKeyPath);
      session = jsch.getSession(username, host, port);
      session.setConfig("PreferredAuthentications", "publickey");
      Properties config = new Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.connect();
    } catch (JSchException e) {
      LOGGER.error(e.getMessage());
    }
  }

  // TODO how should I get actual wall time
  public List<Job> getCurrentJobs() throws JSchException, IOException {
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
    channel.setCommand("qstat -u gj5914 -x;");
    channel.connect();
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
            LOGGER.info("fieldNumber 3 set to jobname " + line[i]);
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
          }
          i++;
        }

        LOGGER.info("jobId: " + jobId);
        LOGGER.info("queue: " + queue);
        LOGGER.info("simulationId: " + simulationId);
        LOGGER.info("status: " + status);
        Job job = new Job();
        job.setSimulationId(simulationId);
        job.setQueue(queue);
        job.setStatus(JobStatus.valueOf(status));
        job.setId(jobId);
        job.setWalltime("00:01:00"); // TODO get from ?
        list.add(job);
      }
    }
    return list;
  }


  public void cancelJob() {
    // TODO how to cancel job?
  }

  public void submitJob(String jobName, String cpuCount, String memory, String queueType,
      String scriptPath, String walltime, String root, String macroPath, String simulationPath,
      String podKey) throws JSchException, IOException {
    // No spaces in qsub command
    String command = "cd " + root + "; mkdir " + jobName + "; chmod 777 " + jobName + ";cd "
        + jobName + "; qsub -N " + jobName + " -q " + queueType + " -lncpus=" + cpuCount + " -e "
        + root + "/" + jobName + "/out.err -o " + root + "/" + jobName + "/out.out -lmem=" + memory
        + "GB -lwalltime=" + walltime + " -v MacroPath=" + macroPath + ",SimulationPath="
        + simulationPath + ",Podkey=" + podKey + " " + scriptPath + ";";
    LOGGER.info("command: " + command);
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
    LOGGER.info("submitting job ");
    channel.setCommand(command);
    channel.connect();
    String str = null;
    while ((str = in.readLine()) != null) {
      LOGGER.info(str);
    }
    LOGGER.info("submitting job fnished");
  }
}
