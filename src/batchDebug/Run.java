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

    //Integer _simulationNumber = 149; // = 65;
    String _simulation = "Cube.sim";
    String _numberComputeCores = "7"; //7 for testing on Gui's PC, 24 in production.
    String _localHostNP = "localhost:" + _numberComputeCores;
    String PODKEY = "5vq0W6k4A3CThu7rcwFeS23KtqY";

    //Instance variables
    Process _simulationProcess = null;
    LocalTime _startTime = null;
    LocalTime _startSimulationTime = null;
    PrintStream _printStreamToLogFile = null;
    String _StarCcmPlusVersion = null;

    Path _StarCcmPlusVersionPath = null;
    static Path CCMPLUSINSTALLEDVERSIONS = Paths.get("C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\TrampoProcess\\src\\Constants\\InstalledVersions.txt");

    public void run() throws IOException {
        String[][] array = Files.lines(CCMPLUSINSTALLEDVERSIONS)
                .map(s -> s.split("\\s+", 2))
                .map(a -> new String[]{a[0], a[1]})
                .toArray(String[][]::new);

        System.out.println(Arrays.deepToString(array));
        String version = array[0][0];
        System.out.println("version is = " + version.replace(",", ""));
        version = array[1][0];
        System.out.println("version is = " + version.replace(",", ""));

        _StarCcmPlusVersion = "11.06.011";
        System.out.println("simulationCcmPlusVersion= " + _StarCcmPlusVersion);
        for (String[] array1 : array) {
            if (array1[0].replace(",", "").equals(_StarCcmPlusVersion)) {
                _StarCcmPlusVersionPath = Paths.get(array1[1]);
                System.out.println("simulationCcmPlusVersionPath= " + array1[1]);
            }
        }
        if (_StarCcmPlusVersionPath == null) {
            System.out.println("simulationCcmPlusVersion is NOT installed on compute node");
        } else {
            System.out.println("simulationCcmPlusVersion is installed on compute node");
        }

    }

    private void RunSimulation() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\CD-adapco\\STAR-CCM+" + _StarCcmPlusVersion + "\\star\\bin\\starccm+.exe", "-batch",
                TRAMPOCLUSTERUTILFOLDERPATH + "//SmartSimulationHandling.java", "-batch-report", "-on", _localHostNP, "-np", _numberComputeCores, "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", PODKEY,
                _simulation);

        File pbWorkingDirectory = getSimulationRunningFolderPath().toFile(); //(new File)?
        pb.directory(pbWorkingDirectory);
        try {

            _simulationProcess = pb.start();
            System.out.println("p started");
            InputStream stdout = _simulationProcess.getInputStream();
            while (stdout.read() >= 0) {;
            }
            _simulationProcess.waitFor();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Path getSimulationRunningFolderPath() {
        return Paths.get("C:\\test\\clusterSetUp\\Run Partition\\customer_5543813196\\test");// +File.separator+"temp"+File.separator+"nestedTemp");
    }
}
