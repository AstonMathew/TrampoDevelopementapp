package com.trampo.process.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

  static ExecutorService executor = Executors.newFixedThreadPool(20);

  public static void logFilePermissions(Path path) {
//    Runnable filePermissionLog = () -> {
//      while (true) {
//        try {
//          Thread.sleep(30000);
//        } catch (InterruptedException e) {
//          LOGGER.error("Error while logging file permissions", e);
//        }
//        Iterator<Path> fileIt = null;
//        try {
//          fileIt = Files.list(path).iterator();
//        } catch (IOException e) {
//          LOGGER.error("Error while logging file permissions", e);
//        }
//        while (fileIt.hasNext()) {
//          Path file = fileIt.next();
//          try {
//            LOGGER.info(
//                "File path: " + file.toString() + " - Permissions: " + getOctalPosixFilePermissions(
//                    Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS)));
//          } catch (IOException e) {
//            LOGGER.error("Error while logging file permissions", e);
//          }
//        }
//        try {
//          LOGGER.info("File path: " + path + " - Permissions: " + getOctalPosixFilePermissions(
//              Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)));
//        } catch (IOException e) {
//          LOGGER.error("Error while logging file permissions", e);
//        }
//      }
//    };
//    executor.submit(filePermissionLog);
  }

  public static String getOctalPosixFilePermissions(Set<PosixFilePermission> permissions) {
    Integer owner = 0;
    Integer group = 0;
    Integer other = 0;

    if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
      owner = owner + 1;
    }
    if (permissions.contains(PosixFilePermission.OWNER_WRITE)) {
      owner = owner + 2;
    }
    if (permissions.contains(PosixFilePermission.OWNER_READ)) {
      owner = owner + 4;
    }

    if (permissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
      group = group + 1;
    }
    if (permissions.contains(PosixFilePermission.GROUP_WRITE)) {
      group = group + 2;
    }
    if (permissions.contains(PosixFilePermission.GROUP_READ)) {
      group = group + 4;
    }

    if (permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
      other = other + 1;
    }
    if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
      other = other + 2;
    }
    if (permissions.contains(PosixFilePermission.OTHERS_READ)) {
      other = other + 4;
    }

    return "" + owner + group + other;
  }

  public static void runChmod(String path) {
    LOGGER.info("chmod command: " + path);
    try{
      Process p = Runtime.getRuntime().exec("chmod -R 770 " + path);
      p.waitFor();
      LOGGER.info("chmod exit status: " + p.exitValue());
      Scanner out = new Scanner(p.getInputStream()).useDelimiter("\\A");
      String result = out.hasNext() ? out.next() : "";
      LOGGER.info("chmod out: " + result);
      Scanner error = new Scanner(p.getErrorStream()).useDelimiter("\\A");
      result = error.hasNext() ? error.next() : "";
      LOGGER.info("chmod error: " + result);
    }catch (Exception e) {
      LOGGER.error("Error while running chmod", e);
    }
  }
}
