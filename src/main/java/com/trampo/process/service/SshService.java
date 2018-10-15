package com.trampo.process.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Component
public class SshService {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(SshService.class);

  private Session session;

  @Autowired
  public SshService(@Value("${trampo.simulation.host}") String host,
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
  
  public List<String> execCommand(String command){
    ChannelExec channel = null;
    try{
      channel = (ChannelExec) session.openChannel("exec");
      BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
      channel.setCommand(command);
      channel.connect();
      List<String> result = new ArrayList<>();
      String str = null;
      while ((str = in.readLine()) != null) {
        result.add(str);
      }
      return result;
    }catch (Exception e) {
      LOGGER.error(e.getMessage());
    }finally {
      channel.disconnect();
    }
    return new ArrayList<>();
  }
  
  public void copyRemoteFile(String remoteFile, String localFile){
    ChannelSftp channel = null;
    try {
      channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      channel.get(remoteFile, localFile);
    } catch (JSchException | SftpException e) {
      LOGGER.error(e.getMessage());
    }finally {
      channel.disconnect();
    }
  }
}
