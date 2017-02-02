/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package batchDebug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Administrator
 */
public class Run {

    static Path TRAMPOCLUSTERUTILFOLDERPATH = Paths.get(
            "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling");
    static String RUNFOLDERROOT = "C:\\test\\clusterSetUp\\Run Partition"; // for
    // testing
    // only
    static String STORAGEFOLDERROOT = "C:\\test\\clusterSetUp\\Storage Partition"; // for
    // testing
    // only

    Integer _simulationNumber = 149; // = 65;
    String _simulation = "Cube.sim";
    String _numberComputeCores = "7"; //7 for testing on Gui's PC, 24 in production.
    String _localHostNP = "localhost:" + _numberComputeCores;
    String _PODkey = "5vq0W6k4A3CThu7rcwFeS23KtqY";
    String _StarCcmPlusVersion = "11.04.012";
    Process _simulationProcess = null;

    public void run() throws IOException, InterruptedException {
        System.out.println("started run"); //"-batch", "-batch-report", 
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\CD-adapco\\STAR-CCM+" + _StarCcmPlusVersion + "\\star\\bin\\starccm+.exe", 
                "-macro", 
                TRAMPOCLUSTERUTILFOLDERPATH + "//SmartSimulationHandling.java", "-on", _localHostNP, "-np", _numberComputeCores, "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", _PODkey,
                _simulation); 
        File pbWorkingDirectory = Paths.get("C:\\test\\clusterSetUp\\Run Partition\\customer_5543813196\\simulation_149").toFile();

        pb.directory(pbWorkingDirectory);
        _simulationProcess = pb.start();
        Process p = pb.start();
        p.waitFor();
        System.out.println("funished run");

    }
}
