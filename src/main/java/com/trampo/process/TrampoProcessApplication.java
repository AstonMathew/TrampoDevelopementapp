package com.trampo.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import com.trampo.process.domain.CcmPlus;
import com.trampo.process.domain.StarCcmPrecision;
import com.trampo.process.service.MailService;
import com.trampo.process.service.SshService;
import com.trampo.process.util.StarCcmPlusUtil;

@EnableScheduling
@SpringBootApplication
public class TrampoProcessApplication {

  public static void main(String[] args) {
    SpringApplication.run(TrampoProcessApplication.class, args);
  }

  @Component
  public static class Runner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger("Startup");

    private SshService sshService;
    private Set<CcmPlus> ccmPlusList;
    private String logFile;
    private ExecutorService executor;
    private long timeout = 5;
    private MailService mailService;
    private String productionOrTestingSwitch;

    @Autowired
    public Runner(SshService sshService, @Value("${trampo.simulation.logFile}") String logFile, 
        @Value("${trampo.simulation.timeout}") String timeout, MailService mailService, 
        @Value("${trampo.simulation.productionOrTestingSwitch}") String productionOrTestingSwitch) {
      this.sshService = sshService;
      ccmPlusList = new HashSet<>();
      this.logFile = logFile;
      executor = Executors.newCachedThreadPool();
      this.timeout = Long.parseLong(timeout);
      this.mailService = mailService;
      this.productionOrTestingSwitch= productionOrTestingSwitch;
    }

    @Override
    public void run(String... args) throws Exception {
      LOGGER.warn("---------------------START-UP start-------------------");
      LOGGER.warn("logging path: " + logFile);
      LOGGER.warn("productionOrTestingSwitch: " + productionOrTestingSwitch);
      LOGGER.warn("started to search installed versions of STAR-CCM+");

      String command =
          "cd /short/uo95/gj5914/starccm/mixed/;ls -d $PWD/* | grep -E -w -o \"[0-9]{2}.[0-9]{2}.[0-9]{3}\" | sort";
      LOGGER.warn("command for mixed precision versions of STAR-CCM+ :" + command);
      List<String> mixedVersions = sshService.execCommand(command);
      LOGGER.warn("installed mixed precision versions: " + mixedVersions);
      if (mixedVersions.isEmpty()) {
        LOGGER.error("---------------------ERROR-------------------");
        LOGGER.error("there is no installed mixed precision versions");
        mailService.sendErrorEmails("there is no installed mixed precision versions");
      }

      command =
          "cd /short/uo95/gj5914/starccm/double/;ls -d $PWD/* | grep -E -w -o \"[0-9]{2}.[0-9]{2}.[0-9]{3}\" | sort";
      LOGGER.warn("command for double precision versions of STAR-CCM+ :" + command);
      List<String> doubleVersions = sshService.execCommand(command);
      LOGGER.warn("installed double precision versions: " + doubleVersions);
      if (doubleVersions.isEmpty()) {
        LOGGER.error("---------------------ERROR-------------------");
        LOGGER.error("there is no installed double precision versions");
        mailService.sendErrorEmails("there is no installed double precision versions");
      }

      command = "find /short/uo95/gj5914/starccm/mixed/ -maxdepth 5 -name starccm+ | sort";
      LOGGER.warn("command for paths of mixed precision executables: " + command);
      List<String> mixedPaths = sshService.execCommand(command);
      LOGGER.warn("installed mixed precision executables :" + mixedPaths);
      if (mixedPaths.isEmpty()) {
        LOGGER.error("---------------------ERROR-------------------");
        LOGGER.error("there is no installed mixed precision executables");
        mailService.sendErrorEmails("there is no installed mixed precision executables");
      }

      command = "find /short/uo95/gj5914/starccm/double/ -maxdepth 5 -name starccm+ | sort";
      LOGGER.warn("command for paths of double precision executables: " + command);
      List<String> doublePaths = sshService.execCommand(command);
      LOGGER.warn("installed double precision executables :" + doublePaths);
      if (doublePaths.isEmpty()) {
        LOGGER.error("---------------------ERROR-------------------");
        LOGGER.error("there is no installed double precision executables");
        mailService.sendErrorEmails("there is no installed double precision executables");
      }

      for (String path : mixedPaths) {
        for (String mixedVersion : mixedVersions) {
          if (path.contains(mixedVersion)) {
            CcmPlus ccmPlus = new CcmPlus();
            ccmPlus.setPath(path);
            ccmPlus.setPrecision(StarCcmPrecision.MIXED);
            ccmPlus.setVersion(mixedVersion);
            ccmPlusList.add(ccmPlus);
            break;
          }
        }
      }

      for (String path : doublePaths) {
        for (String doubleVersion : doubleVersions) {
          if (path.contains(doubleVersion)) {
            CcmPlus ccmPlus = new CcmPlus();
            ccmPlus.setPath(path);
            ccmPlus.setPrecision(StarCcmPrecision.DOUBLE);
            ccmPlus.setVersion(doubleVersion);
            ccmPlusList.add(ccmPlus);
            break;
          }
        }
      }

      StarCcmPlusUtil.setInstalledCcmPluses(ccmPlusList);
      LOGGER.warn("---------------------START-UP end-------------------");

      Runnable main = () -> {        
        for(CcmPlus ccmPlus : StarCcmPlusUtil.getInstalledCcmPluses()) {
          Callable<Boolean> c = () -> { 
            String commandText = ccmPlus.getPath() + " -info /short/uo95/gj5914/infotest/Cube.sim";
            LOGGER.warn("info command: " + commandText);
            List<String> infoResponse = sshService.execCommand(commandText);
            LOGGER.warn("info command response: " + infoResponse + " for path: " + ccmPlus.getPath());
            boolean working = false;
            for (String info : infoResponse) {
              if (info.contains("11.04.012")) { //what is this for ?
                LOGGER.info(
                    "working installation. precision: " + ccmPlus.getPrecision() 
                    + "version: " + ccmPlus.getVersion() + " path: " + ccmPlus.getPath());
                working= true;
                break;
              }
            }
            return working; 
          };
          
          List<Callable<Boolean>> list = new ArrayList<>();
          list.add(c);
          List<Future<Boolean>> result = null;
          try {
            result = executor.invokeAll(list, timeout, TimeUnit.MINUTES);
          } catch (InterruptedException e) {
            LOGGER.error("Unexpected error", e);
            mailService.sendErrorEmails("Unexpected error " + e.getMessage());
          }
          try {
            if(!result.get(0).get()) {
              LOGGER.error("---------------------ERROR-------------------");
              LOGGER.error("installation not working. precision: " + ccmPlus.getPrecision() 
              + "version: " + ccmPlus.getVersion() + " path: " + ccmPlus.getPath());
              StarCcmPlusUtil.remove(ccmPlus);
              mailService.sendErrorEmails("installation not working. precision: " + ccmPlus.getPrecision() 
              + "version: " + ccmPlus.getVersion() + " path: " + ccmPlus.getPath());
            }
          } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("---------------------ERROR-------------------");
            LOGGER.error("installation not working. precision: " + ccmPlus.getPrecision() 
            + "version: " + ccmPlus.getVersion() + " path: " + ccmPlus.getPath(), e);
            mailService.sendErrorEmails("installation not working. precision: " + ccmPlus.getPrecision() 
            + "version: " + ccmPlus.getVersion() + " path: " + ccmPlus.getPath());
            StarCcmPlusUtil.remove(ccmPlus);
          }
        }
        if(StarCcmPlusUtil.getDefaultDoublePrecisionVersion() == null) {
          LOGGER.error("---------------------ERROR-------------------");
          LOGGER.error("there is no installed double precision version");
          mailService.sendErrorEmails("there is no installed double precision version");
        }
        if(StarCcmPlusUtil.getDefaultMixedPrecisionVersion() == null) {
          LOGGER.error("---------------------ERROR-------------------");
          LOGGER.error("there is no installed mixed precision version");
          mailService.sendErrorEmails("there is no installed mixed precision version");
        }
      };
      executor.execute(main);
    }
  }
}
