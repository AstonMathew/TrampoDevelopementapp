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
  public JobService(@Value("${trampo.simulation.host}") String host, @Value("${trampo.simulation.port}") int port, 
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

  public List<Job> getCurrentJobs() throws JSchException, IOException{
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
    channel.setCommand("qstat -u gj5914 -x;");
    channel.connect();
    List<Job> list = new ArrayList<Job>();
    String str=null;
    while((str=in.readLine())!=null){
      LOGGER.info("current running jobs script output lne: " + str);
      if(StringUtils.hasText(str) && !str.startsWith("r-man2") && !str.startsWith("Job ID") 
          && !str.startsWith("----") && !str.contains("Req'd")){
        String[] line = str.split(" ");
        int i = 0;
        while(i < line.length){
          LOGGER.info("line["+i+"]: " + line[i]);
          i++;
        }
        String jobId = line[0];
        String queue = line[6];
        String simulationId = line[7];
        String status = line[19];
        String walltime = line[20];
        LOGGER.info("queue: " + queue);
        LOGGER.info("simulationId: " + simulationId);
        LOGGER.info("status: " + status);
        LOGGER.info("walltime: " + walltime);
        Job job = new Job();
        job.setSimulationId(simulationId);
        job.setQueue(queue);
        job.setStatus(JobStatus.valueOf(status));
        job.setWalltime(walltime);
        list.add(job);
      }
    }
    return list;
  }
  
}
