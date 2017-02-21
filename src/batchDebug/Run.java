/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package batchDebug;

import constants.SimulationStatuses;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Arrays;
import trampoprocess.WebAppGate;


public class Run {
/**
 *
 * moves all files containing the string from the source directory to the destination directory
 * both directory need to exist!
 */
    public void ConditionalMoveFiles(File source, File destination, String string) throws IOException {
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.isFile() && child.getName().toLowerCase().contains(string.toLowerCase())) {
                    System.out.println("directoryListing child.getName = " + child.getName());
                    Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                    System.out.println(" directoryListing child moved to  = " + destination.toPath().resolve(child.getName()));

                }
            }
        }
    }
}
