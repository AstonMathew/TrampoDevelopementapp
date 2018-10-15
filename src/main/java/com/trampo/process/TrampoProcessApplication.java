package com.trampo.process;

import java.util.ArrayList;
import java.util.List;

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
    private List<CcmPlus> ccmPlusList;
    private String logFile;

    @Autowired
    public Runner(SshService sshService, @Value("${trampo.simulation.logFile}") String logFile) {
      this.sshService = sshService;
      ccmPlusList = new ArrayList<CcmPlus>();
      this.logFile = logFile;
    }

    @Override
    public void run(String... args) throws Exception {
      LOGGER.warn("---------------------START-UP start-------------------");
      LOGGER.warn("logging path: " + logFile);
      LOGGER.warn("started to search installed versions of STAR-CCM+");
      String command =
          "cd /short/uo95/gj5914/starccm/mixed/;ls -d $PWD/* | grep -E -w -o \"[0-9]{2}.[0-9]{2}.[0-9]{3}\" | sort";
      LOGGER.warn("command for mixed precision versions of STAR-CCM+ :" + command);
      List<String> mixedVersions = sshService.execCommand(command);
      LOGGER.warn("installed mixed precision versions: " + mixedVersions);
      command =
          "cd /short/uo95/gj5914/starccm/double/;ls -d $PWD/* | grep -E -w -o \"[0-9]{2}.[0-9]{2}.[0-9]{3}\" | sort";
      LOGGER.warn("command for double precision versions of STAR-CCM+ :" + command);
      List<String> doubleVersions = sshService.execCommand(command);
      LOGGER.warn("installed double precision versions: " + doubleVersions);

      command = "find /short/uo95/gj5914/starccm/mixed/ -maxdepth 5 -name starccm+ | sort";
      LOGGER.warn("command for paths of mixed precision executables: " + command);
      List<String> mixedPaths = sshService.execCommand(command);
      LOGGER.warn("installed mixed precision executables :" + mixedPaths);
      command = "find /short/uo95/gj5914/starccm/double/ -maxdepth 5 -name starccm+ | sort";
      LOGGER.warn("command for paths of double precision executables: " + command);
      List<String> doublePaths = sshService.execCommand(command);
      LOGGER.warn("installed double precision executables :" + doublePaths);
      for (String path : mixedPaths) {
        LOGGER.warn("testing mixed precision versions");
        command = path + " -info /short/uo95/gj5914/infotest/Cube.sim";
        LOGGER.warn("info command: " + command);
        List<String> infoResponse = sshService.execCommand(command);
        LOGGER.warn("info command response: " + infoResponse + " for path: " + path);
        for (String info : infoResponse) {
          if (info.contains("11.04.012")) {
            for (String mixedVersion : mixedVersions) {
              if (path.contains(mixedVersion)) {
                CcmPlus ccmPlus = new CcmPlus();
                ccmPlus.setPath(path);
                ccmPlus.setPrecision(StarCcmPrecision.MIXED);
                ccmPlus.setVersion(mixedVersion);
                LOGGER.info("working installation. mixed version: " + mixedVersion + " path: " + path);
                ccmPlusList.add(ccmPlus);
                break;
              }
            }
          }
        }
      }
      for (String path : doublePaths) {
        LOGGER.warn("testing double precision versions");
        command = path + " -info /short/uo95/gj5914/infotest/Cube.sim";
        LOGGER.warn("info command: " + command);
        List<String> infoResponse = sshService.execCommand(command);
        LOGGER.warn("info command response: " + infoResponse + " for path: " + path);
        for (String info : infoResponse) {
          if (info.contains("11.04.012")) {
            for (String doubleVersion : doubleVersions) {
              if (path.contains(doubleVersion)) {
                CcmPlus ccmPlus = new CcmPlus();
                ccmPlus.setPath(path);
                ccmPlus.setPrecision(StarCcmPrecision.DOUBLE);
                ccmPlus.setVersion(doubleVersion);
                LOGGER.info("working installation. double version: " + doubleVersion + " path: " + path);
                ccmPlusList.add(ccmPlus);
                break;
              }
            }
          }
        }
      }
      StarCcmPlusUtil.setInstalledCcmPluses(ccmPlusList);
      LOGGER.warn("---------------------START-UP end-------------------");
    }
  }
}
