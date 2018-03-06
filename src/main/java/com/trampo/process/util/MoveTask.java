package com.trampo.process.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trampo.process.service.SshService;

public class MoveTask {
  
  static final Logger LOG = LoggerFactory.getLogger(MoveTask.class);

  private Timer cron;
  private File source;
  private File destination;
  private SshService sshService;
  private String runRoot;

  public MoveTask(File source, File destination, SshService sshService, String runRoot) {
    this.source = source;
    this.destination = destination;
    this.sshService = sshService;
    this.runRoot = runRoot;
    cron = new Timer();
  }

  public void scheduleFileMove(String string, int integer) {
    LOG.debug("scheduleFileMove start");
    TimerTask moveTimerTask = new MoveTimerTask(source, destination, string, sshService, runRoot);
    LOG.debug("TimerTask moveTimerTask = new MoveTimerTask done");
    try {
      cron.schedule(moveTimerTask, 1, TimeUnit.SECONDS.toMillis(integer)); // this line is the issue
                                                                           // with the installer

    } catch (Exception e) {
      LOG.error("test exception", e);
    }
    LOG.debug("cron.schedule(moveTimerTask done");
  }

  public void cancelPurgeTimer() {
    cron.cancel();
    cron.purge();
  }

  class MoveTimerTask extends TimerTask {

    File source;
    File destination;
    String string;
    SshService sshService;
    private String runRoot;

    public MoveTimerTask(File source, File destination, String string, SshService sshService, String runRoot) {
      this.source = source;
      this.destination = destination;
      this.string = string;
      this.sshService = sshService;
      this.runRoot = runRoot;
      // LOG.debug(String.format("Move task for mask '%s' is scheduled to run every %d hour(s)",
      // string, PERIOD));
    }

    @Override
    public void run() {
      try{
        String command = "chmod -R 770 " + runRoot; // + "/" + jobName;
        LOG.info("submit command: " + command);
        List<String> result = sshService.execCommand(command);
        LOG.info("submitted");
        for (String string : result) {
          LOG.info(string);
        }
        LOG.info("submitting command fnished");
      }catch (Exception e) {
        LOG.error("Error while moving files", e);
      }
      
      try {        
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
          for (File child : directoryListing) {
            if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)
                && child.getName().toLowerCase().contains(string.toLowerCase())) {
              LOG.debug("directoryListing child.getName = " + child.getName());
              Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
              LOG.debug(" directoryListing child moved to  = "
                  + destination.toPath().resolve(child.getName()));
            }
          }
        }
      } catch (IOException ex) {
        LOG.error("Error while moving files", ex);
      }
    }
  }
}
