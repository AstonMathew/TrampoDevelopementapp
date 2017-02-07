/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package batchDebug;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * @author Administrator
 */
public class Run {

//    static Path TRAMPOCLUSTERUTILFOLDERPATH = Paths.get(
//            "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling");
//    static String RUNFOLDERROOT = "C:\\test\\clusterSetUp\\Run Partition"; // for
//    // testing
//    // only
//    static String STORAGEFOLDERROOT = "C:\\test\\clusterSetUp\\Storage Partition"; // for
//    // testing
//    // only
//
//    Integer _simulationNumber = 149; // = 65;
//    String _simulation = "Cube.sim";
//    String _numberComputeCores = "7"; //7 for testing on Gui's PC, 24 in production.
//    String _localHostNP = "localhost:" + _numberComputeCores;
//    String _PODkey = "5vq0W6k4A3CThu7rcwFeS23KtqY";
//    String _StarCcmPlusVersion = "11.04.012";
    public void run() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe", "-batch", "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling\\run.java", "-batch-report", "-on", "localhost:7", "-np", "7", "-power", "-licpath", "1999@flex.cd-adapco.com", "-podkey", "5vq0W6k4A3CThu7rcwFeS23KtqY", "Cube.sim");
        File pbWorkingDirectory = getSimulationRunningFolderPath().toFile();
        pb.directory(pbWorkingDirectory);
        Process p = pb.start();
        Path SolvedSimulationPath = Paths.get(getSimulationRunningFolderPath() + "\\trampoSolved_Cube.sim");
        System.out.println("SolvedSimulationPath = " + SolvedSimulationPath.toString());
        File SolvedSimulationfile = SolvedSimulationPath.toFile();
        
        while (p.isAlive()) {
            Boolean simulationIsSolved = SolvedSimulationfile.exists();
            if (simulationIsSolved) {
                System.out.println("simulation has ended");
                Thread.sleep(4000);
                p.destroy();
                //p.destroyForcibly(); results in rogue processes andno -batch-report
                System.out.println("p has been destroyed");
            } else {
                p.waitFor(1, TimeUnit.SECONDS);
                System.out.println("waiting one second");
            }
        }
    }

private Path getSimulationRunningFolderPath() {
        return Paths.get("C:\\test\\clusterSetUp\\Run Partition\\customer_5543813196\\test");// +File.separator+"temp"+File.separator+"nestedTemp");
    }
}
//to test
//        //pb.redirectOutputStream(_printStreamToLogFile);
////        redirectOutput(ProcessBuilder.Redirect );
////        pb.redirectError(_logFile);
//        pb.inheritIO();
////        pb.redirectOutput(Redirect.INHERIT);
////        pb.redirectError(Redirect.INHERIT);
//        //pb.redirectError(Redirect.appendTo(_logFile));
// "-batch-report",
//        ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe", "-batch", "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling\\run.java","-batch-report", "-on", "localhost:7", "-np", "7", "-power", "-licpath", "1999@flex.cd-adapco.com", "-podkey", "5vq0W6k4A3CThu7rcwFeS23KtqY", "Cube.sim"); 
//        Process p = pb.start(); 
