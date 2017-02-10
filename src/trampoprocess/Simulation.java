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
import java.io.File;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import constants.SimulationStatuses;
import constants.ValidExtensions;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.Paths.get;
import static java.nio.file.Paths.get;

/**
 *
 * @author Administrator
 *
 * TODOGUINOW version list of files no * match= * Status= "CANCELLED:" , put
 * Sims in canceled folder simulationLog.txt gets overwritten // check why and
 * simulationStatus.txt does not list the various simulation status one after
 * another with date/time //runDataExtraction() Simulation name needs to be
 * udated to latest sim file in folder tree. test if Simulation File Name "Cube"
 * without .sim works; If not might consider adding the .sim in the website or
 * database.
 *
 * TODOLATER This code breaks if the simulation folder already exists. star-CCM+
 * version used to determine version used by customer to create the file needs
 * to be adjusted to default version. make CCM+ version a variable in all the pb
 * and use CCM+ default version to run versioncheck and runDataExtraction
 */
public class Simulation {

    // Simulation parameters from the website
    Integer _simulationNumber = null; // = 65;
    String _customerNumber = null;
    String _submissionDate = null; // UTC date
    String _submissionTime = null;// UTC Time
    Integer _maxSeconds = null;
    String _simulation = null;
    Integer _fileCount = null;

    //Instance variables
    Process _simulationProcess = null;
    LocalTime _startTime = null;
    LocalTime _startSimulationTime = null;
    PrintStream _printStreamToLogFile = null;
    String _StarCcmPlusVersion = null;

    // compute node and license parameter need to be changed for production
    static String _numberComputeCores = "7"; //7 for testing on Gui's PC, 24 in production.
    static String _localHostNP = "localhost:" + _numberComputeCores;
    static String PODKEY = "5vq0W6k4A3CThu7rcwFeS23KtqY"; //need to read the key from a text file that can be changed in the  middle of running
    static Path TRAMPOCLUSTERUTILFOLDERPATH = Paths.get(
            "C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\smartSimulationHandling\\src\\smartsimulationhandling");
    static String RUNFOLDERROOT = "C:\\test\\clusterSetUp\\Run Partition"; // for testing only
    static String STORAGEFOLDERROOT = "C:\\test\\clusterSetUp\\Storage Partition"; // for testing only
    static Path CCMPLUSINSTALLEDVERSIONS = Paths.get("C:\\Users\\Administrator\\Dropbox\\Trampo\\IT\\BackEnd\\Gui\\TrampoProcess\\src\\Constants\\InstalledVersions.txt");
    

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
    
    public String getCustomerNumber() {
    	return _customerNumber;
    }

    public void markAsCanceled() {
    	_maxSeconds = ((_startSimulationTime == null) ? 0 : (int) timeInSeconds(_startSimulationTime));
    }
    
    public void updateMaximumClocktimeInSecondsFromWebApp() {
	     try {
			_maxSeconds = new WebAppGate().getSimulationMaxRuntime(this);
		} catch (Exception e) {
			// Can't update from the webapp, best to play it safe and keep the max seconds as is
		}
    }

    public long maximumClocktimeInSeconds() {
        return _maxSeconds;
    }

    public void updateSimulationStatus(String newStatus) throws Exception {
         new WebAppGate().updateSimulationStatus(this, newStatus);
         if (_printStreamToLogFile != null) {_printStreamToLogFile.println(LocalTime.now() + ": Simulation status updated to " + newStatus);}
    }
    
    public void runSimulationWorkflow() throws Exception {
        // System.out.println("1+1=" + 1 + 1);
        _startTime = LocalTime.now();

        // for some reason, _simulation is sometimes missing its last " when checking the variable in debug mode. That kills the run processes. The line below is a first attenpt at fixing it
        //_simulation = _simulation.concat("\""); 
        _simulation = _simulation.replaceAll("\\s+", ""); //DO NOT DELETE!!!
        CreateSimulationFolders();
        redirectOutANDErrToLog(); //remove the commeting out in production
        CreateLogHeader();
        _printStreamToLogFile.println("Starting processing time: " + _startTime);
        checkSim_name_AndFiles_count_extension(); //Not working // need to check only allowed files are copied over and block any illegal files. 
        //probably needs to go outside the queue, as file transfer will not happen instantly. Needs to be able to handle manual file input as well, delayed by half a day.
        CopyCustomerSyncFolderIntoSimulationRunFolder();
        getStarCCMPlusVersion();
        UseStarCCMPlusDefaultVersion(); //Not tested, need to read Star-CCM+ installed on machine itself. oldest install must be the default version
        RunSimulation();
        copyLogOutputWindowToFile();
        Create_Log_backUp_toSync_folders();
        Move_Log_BU_Tosync_foldersToStoragePartition();
        //runDataExtraction; // 
        // check that the running simulation is “alive” in all processes.
        System.out.println("End");
    }

    private void CreateSimulationFolders() throws IOException, Exception {  // test the sim exits thye queue if CANCELLED_SIMULATION_FOLDER_PREEXISTING
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
            updateSimulationStatus(SimulationStatuses.CANCELLED_SIMULATION_FOLDER_PREEXISTING);
            throw new Exception("ERROR: SIMULATIONRUNNINGFOLDER EXISTING !!! with Path: " + getSimulationRunningFolderPath());
        }
    }

    private void redirectOutANDErrToLog() throws FileNotFoundException {

        _printStreamToLogFile = new PrintStream(getSimulationLogPath());
//        System.setOut(_printStreamToLogFile);
//        System.setErr(_printStreamToLogFile);
    }

    private void CreateLogHeader() throws IOException, InterruptedException {

        _printStreamToLogFile.println("HEADER START----------------------------------------------------------------------------------------------------------------");
        _printStreamToLogFile.println("simulationNumber = " + _simulationNumber);
        _printStreamToLogFile.println("customerNumber = " + _customerNumber);
        _printStreamToLogFile.println("submissionDate = " + _submissionDate);
        _printStreamToLogFile.println("maxSeconds = " + _maxSeconds);
        _printStreamToLogFile.println("Simulation = " + _simulation);
        _printStreamToLogFile.println("simulationRunningFolder = " + getSimulationRunningFolderPath());
        _printStreamToLogFile.println("HEADER END-------------------------------------------------------------------------------------------------------------------");
    }

    public void checkSim_name_AndFiles_count_extension() throws Exception { 
    	// test the sim exits is file count and file name is wrong
    	
    	// check file extensions.
    	File dir = getSimulationSendingToTrampoFolderPath().toFile();
    	File[] directoryListing = dir.listFiles();
    	if (directoryListing != null) {
    	    for (File child : directoryListing) {
    	      boolean res = false;
    	      for (int i = 0; i < ValidExtensions.EXTENSIONS.length; i++) {
    	    	  if (child.getName().toLowerCase().endsWith(ValidExtensions.EXTENSIONS[i])) { res = true; }
    	      }
    	      if (res == false) {
    	    	  System.out.println("File " + child.getName() + " extension is not supported");
    	    	  updateSimulationStatus(SimulationStatuses.CANCELLED_UNSAFE_FILES_EXTENSION);
    	    	  throw new Exception("File " + child.getName() + " extension is not supported");
    	      }
    	    }
    	}

    	// Check files count
        if (FileFunctions.countFiles(getSimulationSendingToTrampoFolderPath()) != _fileCount) {
            System.out.println("!!! Actual file count does not match nominated file count !!!");
            updateSimulationStatus(SimulationStatuses.CANCELLED_NOFILEUPLOADED);
            throw new Exception("Simulation " + _simulationNumber + "!!! Actual file count does not match nominated file count !!!");
        } else {
            System.out.println("Actual file count matches nominated file count !!!");
        }

//        // Check for .sim file
//        String sim = (_simulation.toLowerCase().endsWith(".sim")) ? _simulation : (_simulation + ".sim");
//        if (!FileFunctions.fileIsAvailable(getSimulationSendingToTrampoFolderPath().resolve(sim))) {
//            System.out.println("Simulation file " + sim + " is NOT available");
//            throw new Exception("Simulation " + _simulationNumber + " - Simulation file " + sim + " is NOT available");
//        } else {
//            System.out.println("Simulation file " + sim + " is available");
//        }
    }

    private void CopyCustomerSyncFolderIntoSimulationRunFolder() throws IOException, InterruptedException {

        MyFileVisitor visitor = new MyFileVisitor(getSimulationSendingToTrampoFolderPath(),
                getSimulationRunningFolderPath());
        Files.walkFileTree(getSimulationSendingToTrampoFolderPath(), visitor);
    }

    private void getStarCCMPlusVersion() throws IOException, InterruptedException {
        Path versionLogPath = getSimulationRunningFolderPath().resolve("version.log");
        Files.createFile(versionLogPath);
        ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe", "-info", "Cube.sim");
        pb.redirectOutput(versionLogPath.toFile());
        File pbWorkingDirectory = getSimulationRunningFolderPath().toFile(); //(new File)?
        pb.directory(pbWorkingDirectory);
        Process p = pb.start();
        p.waitFor();
        System.out.println("version.log created");
        String content = new String(Files.readAllBytes(versionLogPath));
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

        try (PrintWriter out2 = new PrintWriter(versionLogPath.toString())) {
            out2.println(ccmplusversion);
        }

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

    private void RunSimulation() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\CD-adapco\\STAR-CCM+" + _StarCcmPlusVersion + "\\star\\bin\\starccm+.exe", "-batch",
                TRAMPOCLUSTERUTILFOLDERPATH + "//SmartSimulationHandling.java", "-batch-report", "-on", _localHostNP, "-np", _numberComputeCores, "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", PODKEY,
                _simulation);

        File pbWorkingDirectory = getSimulationRunningFolderPath().toFile(); //(new File)?
        pb.directory(pbWorkingDirectory);
        try {
            updateSimulationStatus(SimulationStatuses.RUNNING);
            _startSimulationTime = LocalTime.now();
            _printStreamToLogFile.println("Starting simulation time: " + _startSimulationTime);
            _simulationProcess = pb.start();
            System.out.println("p started");
            //Redirection of stream exteremely important; http://baxincc.cc/questions/216451/windows-process-execed-from-java-not-terminating
            // if not redirected, Star-CCM+ processes hang and -batch*-report doesn't print
            InputStream stdout = _simulationProcess.getInputStream();
            while (stdout.read() >= 0) {;
            }
            _simulationProcess.waitFor();

            // All below 
            _printStreamToLogFile.println("End simulation time: " + LocalTime.now()); // this doesn't seem to be done at the end of the process.
            _printStreamToLogFile.println("Simulation/Total processing time: " + (int) timeInSeconds(_startSimulationTime) + "s/" + (int) timeInSeconds(_startTime) + "s");
            updateSimulationActualRuntime();
            updateSimulationStatus(SimulationStatuses.COMPLETED);
            _printStreamToLogFile.println("Simulation complete...");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateSimulationActualRuntime() throws Exception {
        new WebAppGate().updateSimulationActualRuntime(this, (int) timeInSeconds(_startSimulationTime));
    }

    private void copyLogOutputWindowToFile() throws IOException, InterruptedException {
        Files.copy(getOutputWindowLogToFile(), getSimulationRunningFolderPath().resolve("outputWindowToFileSimulation_" + _simulationNumber + ".log"), COPY_ATTRIBUTES);
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

    private void Create_Log_backUp_toSync_folders() throws IOException {
        Path logsPath = getSimulationRunningFolderPath().resolve("Logs");
        Files.createDirectory(logsPath);
        Path backUpPath = getSimulationRunningFolderPath().resolve("Backup");
        Files.createDirectory(backUpPath);
        Path toSyncPath = getSimulationRunningFolderPath().resolve("ToSync");
        Files.createDirectory(toSyncPath);

    }

    private void Move_Log_BU_Tosync_foldersToStoragePartition() {
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
        File file = new File(getSimulationRunningFolderPath() + "\\ABORT.txt");

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

    private Path getSimulationSendingToTrampoFolderPath() { // the synchronised copy of the folder in which the customer pastes his files to send to Trampo
        return Paths.get(STORAGEFOLDERROOT, getCustomerFolderRelativePath(), "Synchronised folder", "1_SendingToTrampo");
    }

    private Path getSimulationRunningFolderPath() {// the simulation running folder
        return Paths.get(RUNFOLDERROOT, getCustomerFolderRelativePath(), "simulation_" + _simulationNumber);
    }

    private String getSimulationLogPath() {
        return getSimulationRunningFolderPath() + "\\simulation_" + _simulationNumber + ".log";
    }

    private String getSimulationStatusPath() {
        return getSimulationRunningFolderPath() + "\\simulation_" + _simulationNumber + "_Status.txt";
    }

    private Path getOutputWindowLogToFile() {
        return Paths.get("C:\\Users\\Administrator\\AppData\\Local\\CD-adapco\\STAR-CCM+ " + _StarCcmPlusVersion + "\\var\\log\\messages.log");
    }

}
