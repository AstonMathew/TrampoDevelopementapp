package com.trampo.process.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileFunctions {

  static final Logger LOG = LoggerFactory.getLogger(FileFunctions.class);

  public static boolean fileIsAvailable(Path file) {
    return Files.isRegularFile(file);
  }

  public static int countFiles(Path dir) {
    int count = 0;
    try {
      Iterator<Path> fileIt = Files.list(dir).iterator();
      while (fileIt.hasNext()) {
        Path file = fileIt.next();
        LOG.debug("Going through file " + file);
        if (Files.isRegularFile(file)) {
          count++;
        }
      }
    } catch (IOException e) {
      LOG.error("Unable to count files in " + dir, e);
      count = 0;
    }
    return count;
  }

}
