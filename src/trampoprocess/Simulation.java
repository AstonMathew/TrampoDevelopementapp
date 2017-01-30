/*
 * TODOLATER match # of files between app and sync folder
 */
package trampoprocess;

/**
 *
 * @author Administrator
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import java.util.logging.Level;
import java.util.logging.Logger;

import constants.SimulationStatuses;
import static java.time.temporal.ChronoUnit.MINUTES;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Administrator 
 * TODOGUINOW 
 * troubleshoot the pb1.
 * make CCM+ version a variable in the pb, and trouble shoot meshCount pb;
 * output list of files no match= Status= "CANCELLED:" , put Sims in canceled folder 
 * simulationLog.txt gets overwritten, check why and simulationStatus.txt does not list the various simulation status one after another with date/time
 * File LogFile = new File(getSimulationLogPath()); // don't create a new log file at every process, need to do 1 after the Simulation params have loaded;
 * TODOLATER 
 * This code breaks if the simulation folder already exists.
 * star-CCM+ version used to determine version used by customer to create the file needs to be adjusted to default version.
 */
public class Simulation {

    static Path TRAMPOCLUSTERUTILFOLDERPATH = Paths.get(
            "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling");
    static String RUNFOLDERROOT = "C:\\test\\clusterSetUp\\Run Partition"; // for
    // testing
    // only
    static String STORAGEFOLDERROOT = "C:\\test\\clusterSetUp\\Storage Partition"; // for
    // testing
    // only

    Integer _simulationNumber =1; // = 65;
    String _customerNumber = "5543813196";
    String _submissionDate = "2016/12/01"; // UTC date
    String _submissionTime = "17:05";// UTC Time
    Integer _maxSeconds = 30;
    String _simulation = "Cube.sim";
    String _numberComputeCores = "7"; //7 for testing on Gui's PC, 24 in production.
    String _PODkey = "5vq0W6k4A3CThu7rcwFeS23KtqY";
    Integer _fileCount;

    String _simulationFilename;
    String _StarCcmPlusVersion = "11.04.012";
    Process _simulationProcess = null;
    LocalTime _startTime = null;
    LocalTime _startSimulationTime = null;

    

    /**
     * @param _simulationNumber
     * @param _customerNumber
     * @param _submissionDate
     * @param _maxSeconds
     * @param _Simulation
     * @param _simulationRunningFolder
     */
    public Simulation(Integer simulationNumber, String customerNumber, String submissionDate, int maxSeconds,
            String simulation, int fileCount) {
        _simulationNumber = simulationNumber;
        _customerNumber = customerNumber;
        _submissionDate = submissionDate;
        _maxSeconds = maxSeconds;
        _simulation = simulation;
        _fileCount = fileCount;
        _simulationProcess = null;
        _startTime = null;
    }

    public long maximumClocktimeInSeconds() {
        return _maxSeconds;
    }

    public void runSimulation() throws Exception {
        // System.out.println("1+1=" + 1 + 1);
        _startTime = LocalTime.now();
        CreateSimulationRunFolder();
                CreateLogAndStatusFiles();
        writeLog(getSimulationLogPath(), "Starting processing time: " + _startTime);
//        checkFiles(); Not working
        CopyCustomerSyncFolderIntoSimulationRunFolder();
        FindSimulationFileAndStarCCMPlusVersion();
        UseStarCCMPlusDefaultVersion(); //Not tested
        RunSimulationAndUpdateStatus();
        //CopyResultsBackToSynchronised folder();
        meshcount();
        // check that the running simulation is “alive” in all processes.
        System.out.println("End");
    }

    public void checkFiles() throws Exception { // test the sim exits is file count and file name is wrong
        // Check file count

        if (FileFunctions.countFiles(getSimulationSendingToTrampoFolderPath()) != _fileCount) {
            System.out.println("!!! Actual file count does not match nominated file count !!!");
            new WebAppGate().updateSimulationStatus(this, SimulationStatuses.CANCELLED_NOFILEUPLOADED);
            throw new Exception("Simulation " + _simulationNumber + "!!! Actual file count does not match nominated file count !!!");
        } else {
            System.out.println("Actual file count matches nominated file count !!!");
        }

        // Check for .sim file
        String sim = (_simulation.toLowerCase().endsWith(".sim")) ? _simulation : (_simulation + ".sim");
        if (!FileFunctions.fileIsAvailable(getSimulationSendingToTrampoFolderPath().resolve(sim))) {
            System.out.println("Simulation file " + sim + " is NOT available");
            throw new Exception("Simulation " + _simulationNumber + " - Simulation file " + sim + " is NOT available");
        } else {
            System.out.println("Simulation file " + sim + " is available");
        }
    }

    public long currentRunTimeInSeconds() {
        return timeInSeconds(_startTime);
    }

    public long timeInSeconds(LocalTime time) {
        if (time == null) {
            return -1;
        }
        return ChronoUnit.SECONDS.between(time, LocalTime.now());
    }

    public void abort() throws InterruptedException {
        // To be implemented: soft abort with a grace period
        File file = new File(getSimulationRunningFolderPath() + "ABORT.txt");

        try {
            //Create the file
            file.createNewFile();
            System.out.println("ABORT.txt File is created!");
        } catch (IOException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("ABORT.txt File already exists or the operation failed for some reason");
        }
        _simulationProcess.waitFor(2, TimeUnit.MINUTES);

    }

    public void abortNow() {
        // To be implemented: hard abort NOW!!!!
        _simulationProcess.destroyForcibly();
    }

    private String getCustomerFolderRelativePath() {
        return "customer_" + _customerNumber.trim();
    }

    private Path getSimulationSendingToTrampoFolderPath() {
        return Paths.get(STORAGEFOLDERROOT,
                getCustomerFolderRelativePath(),
                "Synchronised folder", "1_SendingToTrampo");// +File.separator+"temp"+File.separator+"nestedTemp");
        // //the
        // synchronised
        // copy
        // of
        // the
        // folder
        // in
        // which
        // the
        // customer
        // pastes
        // his
        // files
        // to
        // send
        // to
        // Trampo
    }

    private Path getSimulationRunningFolderPath() {
        return Paths.get(RUNFOLDERROOT,
                getCustomerFolderRelativePath(),
                "simulation_" + _simulationNumber);// the
        // simulation
        // running
        // folder
    }

    private void CopyCustomerSyncFolderIntoSimulationRunFolder() throws IOException, InterruptedException {

        MyFileVisitor visitor = new MyFileVisitor(getSimulationSendingToTrampoFolderPath(),
                getSimulationRunningFolderPath());
        Files.walkFileTree(getSimulationSendingToTrampoFolderPath(), visitor);
    }

    private String getSimulationLogPath() {
        return getSimulationRunningFolderPath() + "\\simulationLog.txt";
    }

    private String getSimulationStatusPath() {
        return getSimulationRunningFolderPath() + "\\simulationStatus.txt";
    }

    private void writeLog(String filename, String text) throws IOException {
        try (FileWriter fw = new FileWriter(filename, true);
                BufferedWriter writer = new BufferedWriter(fw);) {
            writer.write(text + "\n");
        }
    }

    private void CreateLogAndStatusFiles() throws IOException, InterruptedException {

        String[] paths = {getSimulationLogPath(), getSimulationStatusPath()};
        // create Log and Status files
        for (String path : paths) {
            writeLog(path, "");
            writeLog(path, "simulationNumber = " + _simulationNumber + "\n");
            writeLog(path, "customerNumber = " + _customerNumber + "\n");
            writeLog(path, "submissionDate = " + _submissionDate + "\n");
            writeLog(path, "maxSeconds = " + _maxSeconds + "\n");
            writeLog(path, "Simulation = " + _simulation + "\n");
            writeLog(path, "simulationRunningFolder = " + getSimulationRunningFolderPath() + "\n");
            writeLog(path, "----------------------------------------------------------------------------------------------------------------\n");
        }
    }

    private void FindSimulationFileAndStarCCMPlusVersion() throws IOException, InterruptedException {
        // find sim file
        File[] files = getSimulationRunningFolderPath().toFile().listFiles(new SimulationFileFilter());
        for (File f : files) {
            String simulationFilename = f.getName();
            System.out.println("The simulation file name is: " + simulationFilename);
        }

        System.out.println(TRAMPOCLUSTERUTILFOLDERPATH + "\\ccmPlusVersion.java");
//        ProcessBuilder pb = new ProcessBuilder(
//                "C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe", "-macro",
//                TRAMPOCLUSTERUTILFOLDERPATH + "//ccmPlusVersion.java", "-on", "localhost:7", "-np", _numberComputeCores, "-power",
//                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", _PODkey,
//                _simulationFilename);

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe", "-macro",
                TRAMPOCLUSTERUTILFOLDERPATH + "//ccmPlusVersion.java", "-on", "localhost:7", "-np", "7", "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", "5vq0W6k4A3CThu7rcwFeS23KtqY",
                "Cube.sim");
        File LogFile = new File(getSimulationLogPath());
    //File outputFile = new File(getSimulationRunningFolderPath() + "\\Z_outputFile.txt"); Do we need these for each pb?
    //File errorFile = new File(getSimulationRunningFolderPath() + "\\Z_errorFile.txt");
    File pbWorkingDirectory = getSimulationRunningFolderPath().toFile(); //(new File)?

        pb.redirectOutput(LogFile);
        pb.redirectError(LogFile);

        pb.directory(pbWorkingDirectory);
        System.out.println("" + pb.directory());
        Process p = pb.start();
        p.waitFor();

        String StarCcmPlusVersion = new String(
                readAllBytes(get(getSimulationRunningFolderPath() + "//ccmplusversion.txt")));
        StarCcmPlusVersion = StarCcmPlusVersion.replace(" ", "");
        System.out.println("StarCcmPlusVersion=" + StarCcmPlusVersion);
    }

    private void UseStarCCMPlusDefaultVersion() throws IOException, InterruptedException {
        // Check that this version of Star-CCM+ is installed on computer.
        String InstalledStarCcmPlusVersionsString = new String(
                readAllBytes(get(TRAMPOCLUSTERUTILFOLDERPATH + "//InstalledCCMPlusVersions.txt")));
        List<String> InstalledStarCcmPlusVersionsArrayList = Arrays
                .asList(InstalledStarCcmPlusVersionsString.split("\\s*,\\s*"));
        System.out.println("InstalledStarCcmPlusVersionsArrayList.contains(\"11.04.012\")"
                + InstalledStarCcmPlusVersionsArrayList.contains("11.04.012"));
        System.out.println("InstalledStarCcmPlusVersionsArrayList.contains(\"11.06.10\")"
                + InstalledStarCcmPlusVersionsArrayList.contains("11.06.10"));
        System.out.println("InstalledStarCcmPlusVersionsArrayList.contains(\"11.06.11\")"
                + InstalledStarCcmPlusVersionsArrayList.contains("11.06.11"));
        int i = 0;
        for (String version : InstalledStarCcmPlusVersionsArrayList) {
            InstalledStarCcmPlusVersionsArrayList.set(i, version.replace(" ", ""));
            System.out.println(version);
            i++;
        }

        if (!InstalledStarCcmPlusVersionsArrayList.contains(_StarCcmPlusVersion)) {
            _StarCcmPlusVersion = InstalledStarCcmPlusVersionsArrayList
                    .get(InstalledStarCcmPlusVersionsArrayList.size() - 1);
        } else {
            System.out.println("InstalledStarCcmPlusVersionsArrayList.contains(StarCcmPlusVersion)");
        }
        System.out.println("StarCcmPlusVersion=" + _StarCcmPlusVersion);
    }

    private void RunSimulationAndUpdateStatus() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\CD-adapco\\STAR-CCM+" + _StarCcmPlusVersion + "\\star\\bin\\starccm+.exe", "-macro",
                TRAMPOCLUSTERUTILFOLDERPATH + "//SmartSimulationHandling.java", "-on", "localhost:7", "-np", _numberComputeCores, "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", _PODkey,
                _simulationFilename);
        
        File LogFile = new File(getSimulationLogPath());
    //File outputFile = new File(getSimulationRunningFolderPath() + "\\Z_outputFile.txt"); Do we need these for each pb?
    //File errorFile = new File(getSimulationRunningFolderPath() + "\\Z_errorFile.txt");
    File pbWorkingDirectory = getSimulationRunningFolderPath().toFile(); //(new File)?
        
        pb.redirectOutput(LogFile);
        pb.redirectError(LogFile);
        pb.directory(pbWorkingDirectory);
        try {
            new WebAppGate().updateSimulationStatus(this, SimulationStatuses.RUNNING);
            _startSimulationTime = LocalTime.now();
            writeLog(getSimulationLogPath(), "Starting simulation time: " + _startSimulationTime);
            // _simulationProcess = pb.start(); //
            writeLog(getSimulationLogPath(), "End simulation time: " + LocalTime.now());
            writeLog(getSimulationLogPath(), "Simulation/Total processing time: " + (int) timeInSeconds(_startSimulationTime) + "s/" + (int) timeInSeconds(_startTime) + "s");
            new WebAppGate().updateSimulationActualRuntime(this, (int) timeInSeconds(_startSimulationTime));
            new WebAppGate().updateSimulationStatus(this, SimulationStatuses.COMPLETED);
            writeLog(getSimulationLogPath(), "Simulation complete...");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void meshcount() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe", "-macro",
                TRAMPOCLUSTERUTILFOLDERPATH + "//meshCount.java", "-on", "localhost:7", "-np", _numberComputeCores, "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", _PODkey,
                _simulationFilename);
        
        File LogFile = new File(getSimulationLogPath());
    //File outputFile = new File(getSimulationRunningFolderPath() + "\\Z_outputFile.txt"); Do we need these for each pb?
    //File errorFile = new File(getSimulationRunningFolderPath() + "\\Z_errorFile.txt");
    File pbWorkingDirectory = getSimulationRunningFolderPath().toFile(); //(new File)?
        
        pb.redirectOutput(LogFile);
        pb.redirectError(LogFile);
        pb.directory(pbWorkingDirectory);
        Process p = pb.start();
        if (p.isAlive() == true) {
            p.waitFor(1, TimeUnit.MINUTES);
        } else {
            p.destroyForcibly();
        }

    }

    private void CreateSimulationRunFolder() throws IOException, Exception {  // test the sim exits thye queue if CANCELLED_SIMULATION_FOLDER_PREEXISTING
//Files.createDirectory(getSimulationRunningFolderPath());
// Files.createTempFile(simulationSendingToTrampoFolderPath,
        // "tmp",".txt");
        if (Files.isDirectory(getSimulationRunningFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getSimulationRunningFolderPath());
            System.out.println("simulationRunningFolder created " + getSimulationRunningFolderPath());
            //System.out.println("src folder will show below as Directory copied");
        } else {
            System.err.println(
                    "ERROR: SIMULATIONRUNNINGFOLDER EXISTING !!! with Path: " + getSimulationRunningFolderPath());
            // this needs to make the simulation exist the queue as it indicates a major problem
            new WebAppGate().updateSimulationStatus(this, SimulationStatuses.CANCELLED_SIMULATION_FOLDER_PREEXISTING);
        }
    }
}
