
package batchDebug;

/**
 *
 * @author Administrator
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import constants.SimulationStatuses;
import constants.ValidExtensions;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import java.nio.file.StandardCopyOption;

/**
 *
 * @author Administrator

 TODOGUINOW 

 TODOLATER This code breaks if the simulation folder already exists. 
 //runDataExtraction() JobTest name needs to be updated to latest sim file in folder tree.
 move InstalledVersions.txt into the code.
 */
public class JobTest {

    // JobTest parameters from the website
    Integer _jobNumber = null; // = 65;
    String _customerNumber = null;
    String _submissionDate = null; // UTC date
    String _submissionTime = null;// UTC Time
    Integer _maxSeconds = null;
    String _simulation = null;
    Integer _fileCount = null;

    //Instance variables
    Process _simulationProcess = null;
    LocalTime _creationTime = null;
    LocalTime _startTime = null;
    LocalTime _startSimulationTime = null;
    PrintStream _printStreamToLogFile = null;
    String _StarCcmPlusVersion = null;
    String _StarCcmPlusVersionPath = null;
    String _StarCcmPlusDefaultVersion = null;
    String _StarCcmPlusDefaultVersionPath = null;

    // compute node and license parameter need to be changed for production
    static String _numberComputeCores = "7"; //7 for testing on Gui's PC, 24 in production.
    static String _localHostNP = "localhost:" + _numberComputeCores;
    static String PODKEY = "5vq0W6k4A3CThu7rcwFeS23KtqY"; //need to read the key from a text file that can be changed in the  middle of running
    static Path TRAMPOCLUSTERUTILFOLDERPATH = Paths.get(
            "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\Code\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling");
    static String DATAROOT = "C:\\data\\";
    static String RUNROOT = "C:\\run\\";
    static Path CCMPLUSINSTALLEDVERSIONS = Paths.get("C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\Code\\Gui\\TrampoProcess\\src\\Constants\\InstalledVersions.txt");
    static String CCMPLUSVERSIONFORINFOFLAGRUNPATH = "C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe";
    static int TOBACKUPCOPYWAITINGTIME = 1000; //1000 MILLIsecond for testing, 300000 MILLIsecond =5 minutes for production

    /**
     * @param _jobNumber
     * @param _customerNumber
     * @param _submissionDate
     * @param _maxSeconds
     * @param _Simulation
     * 
     */
    public JobTest(Integer jobNumber, String customerNumber, String submissionDate, int maxSeconds,
            String simulation, int fileCount) {
        _jobNumber = jobNumber;
        _customerNumber = customerNumber;
        _submissionDate = submissionDate;
        _maxSeconds = maxSeconds;
        _simulation = simulation;
        _fileCount = fileCount;
        _simulationProcess = null;
        _startTime = null;
        _creationTime = LocalTime.now();
    }
    
    public LocalTime getCreationTime() {
    	return _creationTime;
    }
 

    public void runJobWorkflow() throws Exception {
        // System.out.println("1+1=" + 1 + 1);
        _fileCount = 1; ///TESTING ONLY WHILE FILE COUNT BROKEN ON WEBSITE
        _startTime = LocalTime.now();

        // for some reason, _simulation is sometimes missing its last " when checking the variable in debug mode. That kills the run processes. The line below is a first attenpt at fixing it
        createJobFolders();
        createLogAndBackupDirectories(); //remove the commeting out in production
        CreateLogHeader();
        _printStreamToLogFile.println("Starting processing time: " + _startTime);
        CopyCustomerSyncFolderIntoJobRunFolder();
        getCustomerStarCCMPlusVersion();
        selectStarCCMPlusRunVersion(); //LATER: need to read Star-CCM+ installed on machine itself. Adress issue of latest CCM+ version not loaded on Trampo yet.
        RunJob();
        copyLogOutputWindowToFile();
        //runDataExtraction; // 
        // check that the running JobTest is “alive” in all processes.
        System.out.println("End");
    }

    private void createJobFolders() throws IOException, Exception {  // test the sim exits thye queue if CANCELLED_SIMULATION_FOLDER_PREEXISTING
//Files.createDirectory(getJobRunningFolderPath());
// Files.createTempFile(simulationSendingToTrampoFolderPath,
        // "tmp",".txt");
        if (Files.isDirectory(getJobRunningFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getJobRunningFolderPath());
            System.out.println("JobRunningFolder created " + getJobRunningFolderPath());
            //System.out.println("src folder will show below as Directory copied");
        } else {
            System.err.println(
                    "ERROR: JOBRUNNINGFOLDER EXISTING !!! with Path: " + getJobRunningFolderPath());
            // this needs to make the simulation exist the queue as it indicates a major problem
            updateJobStatus(SimulationStatuses.CANCELLED_SIMULATION_FOLDER_PREEXISTING);
            throw new Exception("ERROR: JOBRUNNINGFOLDER EXISTING !!! with Path: " + getJobRunningFolderPath());
        }
    }

    private void createLogAndBackupDirectories() throws FileNotFoundException, IOException {
        Files.createDirectories(getJobLogsPath());
        boolean JobLogs = Files.isDirectory(getJobLogsPath(), LinkOption.NOFOLLOW_LINKS);
        System.out.println("JobLogs isDirectory " + JobLogs);
        System.out.println(" getJobLogsPath Folder created " + getJobLogsPath());

        Files.createDirectories(getJobBackupPath());
        System.out.println(" getJobBackupPath Folder created " + getJobBackupPath());
        
        File log = getJobLogsPath().resolve("\\job_" + _jobNumber + ".log").toFile(); // this seems t save to the C:/drive for some reason
        System.out.println("HERE writing job_" + _jobNumber + ".log at path= "+ log.toString());
        try {
            //Create the file
            Files.createFile(log.toPath());
            System.out.println("job_" + _jobNumber + ".log File is created!");
        } catch (IOException ex) {
            Logger.getLogger(JobTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("job_" + _jobNumber + ".log : File already exists or the operation failed for some reason");
        }
        _printStreamToLogFile = new PrintStream(log);
//        System.setOut(_printStreamToLogFile); remove for production
//        System.setErr(_printStreamToLogFile);
    }

    private void CreateLogHeader() throws IOException, InterruptedException {

        _printStreamToLogFile.println("HEADER START----------------------------------------------------------------------------------------------------------------");
        _printStreamToLogFile.println("simulationNumber = " + _jobNumber);
        _printStreamToLogFile.println("customerNumber = " + _customerNumber);
        _printStreamToLogFile.println("submissionDate = " + _submissionDate);
        _printStreamToLogFile.println("maxSeconds = " + _maxSeconds);
        _printStreamToLogFile.println("Simulation = " + _simulation);
        _printStreamToLogFile.println("simulationRunningFolder = " + getJobRunningFolderPath());
        _printStreamToLogFile.println("HEADER END-------------------------------------------------------------------------------------------------------------------");
    }

    private void CopyCustomerSyncFolderIntoJobRunFolder() throws IOException, InterruptedException {
        System.out.println("starting CopyCustomerSyncFolderIntoJobRunFolder()");
//        SimulationFolderFileVisitor visitor = new SimulationFolderFileVisitor(getCustomerSynchronisedFolder(),
//                getJobRunningFolderPath());
//        Files.walkFileTree(getCustomerSynchronisedFolder(), visitor);

        File sourceDirectory = getCustomerSynchronisedFolder().toFile();
        File destinationDirectory = getJobRunningFolderPath().toFile();
        ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");
//        File[] directoryListing = dir.listFiles();
//        if (directoryListing != null) {
//            for (File child : directoryListing) {
//                if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)) { //we're not copying directories, just files
//                    Files.move(child.toPath(), getJobRunningFolderPath().resolve(child.getName()), StandardCopyOption.REPLACE_EXISTING);
//                    System.out.println("File Copied: " + child.toString());//.replaceAll(Matcher.quoteReplacement(src.toString()), ""));
//                } else {
//                    System.out.println("File NOT Copied: " + child.toString());
//                }
//            }
//        }

        System.out.println("finished CopyCustomerSyncFolderIntoJobRunFolder()");
    }

    private void getCustomerStarCCMPlusVersion() throws IOException, InterruptedException {
        Path InitialVersionLogPath = getJobRunningFolderPath().resolve("version.log");
        Files.createFile(InitialVersionLogPath);
        ProcessBuilder pb = new ProcessBuilder(CCMPLUSVERSIONFORINFOFLAGRUNPATH, "-info", "Cube.sim");
        pb.redirectOutput(InitialVersionLogPath.toFile());
        File pbWorkingDirectory = getJobRunningFolderPath().toFile(); //(new File)?
        pb.directory(pbWorkingDirectory);
        Process p = pb.start();
        p.waitFor();
        System.out.println("version.log created");
        String content = new String(Files.readAllBytes(InitialVersionLogPath));
        System.out.println("version.log=" + content);

        int index = content.lastIndexOf("STAR-CCM+");
        System.out.println("index=" + index);
        int startindex = index + 9;
        System.out.println("versionsectionstartindex=" + startindex);
        int stopindex = startindex + 9;
        System.out.println("stopindex=" + stopindex);
        String ccmplusversion = content.substring(startindex, stopindex);
        System.out.println("CCM+ version = " + ccmplusversion);
        _StarCcmPlusVersion = ccmplusversion.replace(" ", "");
        Path finalVersionLogPath = getJobLogsPath().resolve("version.log");
        System.out.println("finalVersionLogPath = " + finalVersionLogPath.toString());
        Files.createFile(finalVersionLogPath);
        Files.move(InitialVersionLogPath, finalVersionLogPath, StandardCopyOption.REPLACE_EXISTING);
        //Files.delete(InitialVersionLogPath);

//        try (PrintWriter out2 = new PrintWriter(InitialVersionLogPath.toString())) {
//            out2.println(_StarCcmPlusVersion);
//        }
    }

    private void selectStarCCMPlusRunVersion() throws IOException, InterruptedException { //oldest install is the default version
        String[][] array = Files.lines(CCMPLUSINSTALLEDVERSIONS)
                .map(s -> s.split("\\s+", 2))
                .map(a -> new String[]{a[0], a[1]})
                .toArray(String[][]::new);

        System.out.println(Arrays.deepToString(array));
        String version = array[0][0];
        System.out.println("version is = " + version.replace(",", ""));
        version = array[1][0];
        System.out.println("version is = " + version.replace(",", ""));

        _StarCcmPlusVersion = "11.00.011"; // caught by getStarCCMPlusVersion()
        System.out.println("simulationCcmPlusVersion= " + _StarCcmPlusVersion);
        for (int i = 0; i < array.length; i++) {
            _StarCcmPlusDefaultVersion = array[0][0].replace(",", "");
            _StarCcmPlusDefaultVersionPath = array[0][1].replace(",", "");

            if (array[i][0].replace(",", "").equals(_StarCcmPlusVersion)) {
                _StarCcmPlusVersionPath = array[i][1].replace(",", "");
                System.out.println("simulationCcmPlusVersionPath= " + array[i][1]);
            }
        }
        if (_StarCcmPlusVersionPath == null) {
            System.out.println("simulationCcmPlusVersion is NOT installed on compute node");
            System.out.println("using DEFAULT VERSION");
            _StarCcmPlusVersion = _StarCcmPlusDefaultVersion;
            System.out.println("_StarCcmPlusDefaultVersion= " + _StarCcmPlusDefaultVersion);
            _StarCcmPlusVersionPath = _StarCcmPlusDefaultVersionPath;
            System.out.println("_StarCcmPlusDefaultVersionPath= " + _StarCcmPlusDefaultVersionPath);
        } else {
            System.out.println("simulationCcmPlusVersion is installed on compute node");

        }

    }

    private void RunJob() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                _StarCcmPlusVersionPath, "-batch", TRAMPOCLUSTERUTILFOLDERPATH + "//SmartSimulationHandling.java",
                "-batch-report", "-on", _localHostNP, "-np", _numberComputeCores, "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", PODKEY,
                _simulation);

        File pbWorkingDirectory = getJobRunningFolderPath().toFile(); //(new File)?
        pb.directory(pbWorkingDirectory);
        try {
            updateJobStatus(SimulationStatuses.RUNNING);
            _startSimulationTime = LocalTime.now();
            _printStreamToLogFile.println("Starting simulation time: " + _startSimulationTime);
            _simulationProcess = pb.start();
            System.out.println("p started");
            //Redirection of stream and loop extremely important; http://baxincc.cc/questions/216451/windows-process-execed-from-java-not-terminating
            // if not redirected, Star-CCM+ processes hang and -batch*-report doesn't print
            InputStream stdout = _simulationProcess.getInputStream();
            while (stdout.read() >= 0) {;
//                BackupFolderFileVisitor visitor = new BackupFolderFileVisitor(
//                        getJobRunningFolderPath(), getJobBackupPath());
//                Files.walkFileTree(getJobRunningFolderPath(), visitor);
          
                //Thread.sleep(TOBACKUPCOPYWAITINGTIME);
            }
            _simulationProcess.waitFor();
            File sourceDirectory = pbWorkingDirectory;
                File destinationDirectory = getJobBackupPath().toFile();
                ConditionalMoveFiles(sourceDirectory, destinationDirectory, "@");
                destinationDirectory = getJobLogsPath().toFile();
                ConditionalMoveFiles(sourceDirectory, destinationDirectory, "log");

            // All below 
            _printStreamToLogFile.println("End simulation time: " + LocalTime.now()); // this doesn't seem to be done at the end of the process.
            _printStreamToLogFile.println("Simulation/Total processing time: " + (int) timeInSeconds(_startSimulationTime) + "s/" + (int) timeInSeconds(_startTime) + "s");
            //updateJobActualRuntime(); 
            updateJobStatus(SimulationStatuses.COMPLETED);
            _printStreamToLogFile.println("Simulation complete...");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateJobActualRuntime() throws Exception {
        //new WebAppGate().updateJobActualRuntime(this, (int) timeInSeconds(_startSimulationTime)); //TESTING
    }

    private void copyLogOutputWindowToFile() throws IOException, InterruptedException {
        Files.copy(getOutputWindowLogToFile(), getJobLogsPath().resolve("outputWindowToFileJob_" + _jobNumber + ".log"), COPY_ATTRIBUTES);
        System.out.println("OUTPUTWINDOWLOGTOFILELOCATION COPIED");
    }

    private void runDataExtraction() throws IOException, InterruptedException {

        //extract mesh count from Cells
        //modules
        //iterations, time step inner iterations etc... 
        //Send all to database to be used by time estimator tool 
//        Started version file C:\Users\Administrator\AppData\Local\CD-adapco\STAR-CCM+ 11.04.012\var\log\output1486524196689.log.
//Starting local server: starccm+ -server -power -collab -np 7 -on localhost:7 -cpubind -podkey 5vq0W6k4A3CThu7rcwFeS23KtqY -licpath 1999@flex.cd-adapco.com -rsh ssh "Rotate Blind GJ1 Mini.sim"
//mpirun: Drive is not a network mapped - using local drive.
//WARNING: No cached password or password provided.
//         use '-pass' or '-cache' to provide password
//MPI Distribution : Platform Computing MPI-9.1.4.0
//Host 0 -- WIN-DF270PLD9CO -- Ranks 0-6
//Process rank 0 WIN-DF270PLD9CO 6152
//Total number of processes : 7
//
//STAR-CCM+ 11.04.012 (win64/intel15.0)
//License build date: 10 February 2015
//This version of the code requires license version 2016.06 or greater.
//Checking license file: 1999@flex.cd-adapco.com
//Checking license file: 1999@81.134.157.100
//1 copy of ccmppower checked out from 1999@flex.cd-adapco.com
//Feature ccmppower expires in 145 days
//Wed Feb 08 14:23:29 2017
//
//Server::start -host WIN-DF270PLD9CO:47827
//Loading object database: Rotate Blind GJ1 Mini.sim
//Loading module: StarMeshing
//Loading module: MeshingSurfaceRepair
//Loading module: StarResurfacer
//Loading module: StarTrimmer
//Loading module: SegregatedFlowModel
//Loading module: SegregatedEnergyModel
//Loading module: KeTurbModel
//Loading module: RadiationCommon
//Loading module: RadiationS2sModel
//Loading module: ViewFactorsModel
//Loading module: VofModel
//Loading module: SynTurbModel
//Loading module: KwTurbModel
//Loading module: SaTurbModel
//Loading module: CaeImport
//Simulation database saved by:
//  STAR-CCM+ 7.04.004 (win64/intel11.1) Fri May 11 13:34:38 UTC 2012 Serial 
//Loading into:
//  STAR-CCM+ 11.04.012 (win64/intel15.0) Tue Jul 12 21:40:30 UTC 2016 Np=7
//Started Parasolid modeler version 28.01.177
//Object database load completed.
//Initializing meshing pipeline...
//  All Geometry up to date.
//No parts-based mesh operations to execute
//Executing region-based surface meshers...
//   No region-based surface meshers selected.
//All region-based volume meshers up to date, skipping...
//No parts-based volume mesh operations to execute
//Volume Meshing Pipeline Completed: CPU Time: 0.00, Wall Time: 0.00, Memory: 176.88 MB
//Loading/configuring connectivity (old|new partitions: 1|7)
//  2 AIR OUTER (index 4): 7040 cells, 17362 faces, 10920 verts.
//  0 INSULATOR (index 5): 7810 cells, 19273 faces, 12096 verts.
//  0 INSULATOR 1 (index 17): 7810 cells, 19273 faces, 12096 verts.
//  3 11 RUBBER STOPPER [0] (index 24): 635 cells, 1202 faces, 1412 verts.
//  3 8 PC32 [0] (index 26): 3300 cells, 6485 faces, 6832 verts.
//  3 4 PC25 [0] (index 28): 3080 cells, 6049 faces, 6384 verts.
//  3 7 ALUMINIUM 2 [0] (index 29): 3300 cells, 6485 faces, 6832 verts.
//  3 5 ALUMINIUM 1 [0] (index 30): 3080 cells, 6049 faces, 6384 verts.
//  3 9 ALUMINIUM 3 [0] (index 31): 3300 cells, 6485 faces, 6832 verts.
//  3 1 VIP [0] (index 32): 12760 cells, 34638 faces, 16520 verts.
//  3 2 TIMBER 1 [0] (index 33): 220 cells, 381 faces, 560 verts.
//  5 GLAZING INNER 0 (index 3): 44 cells, 73 faces, 120 verts.
//  1 GLAZING OUTER 0 (index 9): 44 cells, 73 faces, 120 verts.
//  1 GLAZING OUTER 1 (index 11): 44 cells, 73 faces, 120 verts.
//  5 GLAZING INNER 1 (index 18): 44 cells, 73 faces, 120 verts.
//  4 AIR INNER (index 0): 2805 cells, 7264 faces, 4032 verts.
//  3 2 TIMBER 2 [0] (index 27): 275 cells, 490 faces, 672 verts.
//  3 0 AIR VIP [0] (index 10): 58780 cells, 171857 faces, 70973 verts.
//  3 10 AIR PCM [0] (index 20): 48800 cells, 142413 faces, 61039 verts.
//  AIR INDOOR (index 23): 51072 cells, 147832 faces, 56595 verts.
//  ALUMINIUM FRAME Y- (index 21): 1036 cells, 2007 faces, 2204 verts.
//  ALUMINIUM FRAME Y+ (index 22): 1036 cells, 2007 faces, 2204 verts.
//  ENDCAP Y- [0] (index 25): 77 cells, 124 faces, 220 verts.
//  3 3 ALUMINIUM 0 [0] (index 34): 2112 cells, 4112 faces, 4450 verts.
//  ENDCAP Y+ [0] (index 35): 79 cells, 127 faces, 226 verts.
//  3 6 PC29 [0] (index 36): 2112 cells, 4112 faces, 4450 verts.
//Configuring finished
//Reading material property database "C:\Program Files\CD-adapco\STAR-CCM+11.04.012\star\props.mdb"...
//	Cells: 220695	Faces: 606319	Vertices: 294413
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
    
    private boolean _isAborting = false;

    public void abort() throws InterruptedException {
    	if (! _isAborting) {
          File file = new File(getJobRunningFolderPath() + "\\ABORT.txt");

          try {
              //Create the file
              file.createNewFile();
              System.out.println("ABORT.txt File is created!");
          } catch (IOException ex) {
              Logger.getLogger(JobTest.class.getName()).log(Level.SEVERE, null, ex);
              System.out.println("ABORT.txt File already exists or the operation failed for some reason");
          }
          _simulationProcess.waitFor(2, TimeUnit.MINUTES);
          _isAborting = true;
    	}
    }

    public void abortNow() {
        // To be implemented: hard abort NOW!!!!
        _simulationProcess.destroyForcibly();
    }

    private String getCustomerFolderRelativePath() {
        return "customer_" + _customerNumber.trim();
    }

    private Path getCustomerSynchronisedFolder() { // the synchronised copy of the folder in which the customer pastes his files to send to Trampo
        return Paths.get(DATAROOT, getCustomerFolderRelativePath(), "Synchronised folder");
    }

    private Path getJobRunningFolderPath() {// the JobTest running folder
        return Paths.get(RUNROOT, getCustomerFolderRelativePath(), "Job_" + _jobNumber);
    }

    private Path getJobLogsPath() {
        return Paths.get(DATAROOT, getCustomerFolderRelativePath(), "\\Job_" + _jobNumber, "\\logs");
    }

    private Path getJobBackupPath() {
        return Paths.get(DATAROOT, getCustomerFolderRelativePath(), "\\Job_" + _jobNumber + "\\backup");
    }

    private String getJobStatusPath() {
        return getJobRunningFolderPath() + "\\Job_" + _jobNumber + "_Status.txt";
    }

    private Path getOutputWindowLogToFile() { // might need update to work for all versions
        return Paths.get("C:\\Users\\Administrator\\AppData\\Local\\CD-adapco\\STAR-CCM+ " + _StarCcmPlusVersion + "\\var\\log\\messages.log");
    }

    public String getCustomerNumber() {
        return _customerNumber;
    }

    public void markAsCanceled() {
        _maxSeconds = ((_startSimulationTime == null) ? 0 : (int) timeInSeconds(_startSimulationTime));
    }

    public void updateMaximumClocktimeInSecondsFromWebApp() {

        try {
//            _maxSeconds = new WebAppGate().getJobMaxRuntime(this); //TESTING
        } catch (Exception e) {
            // Can't update from the webapp, best to play it safe and keep the max seconds as is
        }

    }

    public long maximumClocktimeInSeconds() {
        return _maxSeconds;
    }

    public void updateJobStatus(String newStatus) throws Exception {
//        new WebAppGate().updateSimulationStatus(this, newStatus); //TESTING
//        if (_printStreamToLogFile != null) {
//            _printStreamToLogFile.println(LocalTime.now() + ": Job status updated to " + newStatus);
//        }
//        System.out.println("Job status updated to " + newStatus);
    }

    /**
     *
     * moves all files containing the string from the source directory to the
     * destination directory both directory need to exist!
     */
    public void ConditionalMoveFiles(File source, File destination, String string) throws IOException {
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS) && child.getName().toLowerCase().contains(string.toLowerCase())) {
                    System.out.println("directoryListing child.getName = " + child.getName());
                    Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                    System.out.println(" directoryListing child moved to  = " + destination.toPath().resolve(child.getName()));

                }
            }
        }
    }
}
