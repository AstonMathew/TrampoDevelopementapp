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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import constants.JobStatuses;
import constants.ValidExtensions;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Administrator
 *
 * TODOGUINOW * TODOLATER This code breaks if the simulation folder already
 * exists. //runDataExtraction() JobTest name needs to be updated to latest sim
 * file in folder tree. move InstalledVersions.txt into the code.
 */
public class Job {

    // Job parameters from the website
    Integer _jobNumber = null; // = 65;
    String _customerNumber = null;
    Date _submissionDate = null; // UTC date
    String _submissionTime = null;// UTC Time
    Long _maxSeconds = null;
    String _simulation = null;
    Integer _fileCount = null;

    //Instance variables
    Process _simulationProcess = null;
    Date _creationTime = null;
    LocalTime _startTime = null;
    LocalTime _startSimulationTime = null;
    PrintStream _printStreamToLogFile = null;
    String _StarCcmPlusVersion = null;
    String _StarCcmPlusVersionPath = null;
    String _StarCcmPlusDefaultVersion = null;
    String _StarCcmPlusDefaultVersionPath = null;
    MoveTask moveTaskScenes;
    MoveTask moveTaskPlots;
    MoveTask moveTaskMesh;
    MoveTask moveTaskBackUp;

    // compute node and license parameter
    static DevProdSwitch config = new DevProdSwitch();
    static String _localHostNP = "localhost:" + config.getProperty("_numberComputeCores");
    static String PODKEY = "5vq0W6k4A3CThu7rcwFeS23KtqY"; //need to read the key from a text file that can be changed in the  middle of running
    
    static String DATAROOT = "S:\\";
    static String RUNROOT = "R:\\";
    static Path TRAMPOCLUSTERUTILFOLDERPATH = Paths.get(RUNROOT); 
    static Path CCMPLUSINSTALLEDVERSIONS = Paths.get(RUNROOT, "InstalledVersions.txt");

    static String CCMPLUSVERSIONFORINFOFLAGRUNPATH = "C:\\Program Files\\CD-adapco\\STAR-CCM+11.04.012\\star\\bin\\starccm+.exe";
    
    //log
    static final Logger LOG = LoggerFactory.getLogger(Job.class); //replace Test with actual class name (Job in this example)

    /**
     * @param jobNumber
     * @param customerNumber
     * @param submissionDate
     * @param maxSeconds
     * @param simulation
     * @param fileCount
     *
     */
    public Job(Integer jobNumber, String customerNumber, String submissionDate, Long maxSeconds,
            String simulation, int fileCount) {
        _jobNumber = jobNumber;
        _customerNumber = customerNumber;

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            _submissionDate = df.parse(submissionDate);
        } catch (ParseException e) {
            LOG.debug("Error parsing date " + submissionDate + ". Revert to now");
            _submissionDate = new GregorianCalendar().getTime();
        };
        _maxSeconds = maxSeconds;
        _simulation = simulation;
        _fileCount = fileCount;
        _simulationProcess = null;
        _startTime = null;
        // _creationTime = LocalTime.now();
        _creationTime = new GregorianCalendar().getTime();
    }

    public Date getSubmissionDate() {
        return _submissionDate;
    }

    public Date getCreationTime() {
        return _creationTime;
    }

    public boolean areFilesAvailable() throws Exception { //used in Job queue/add job()

        //_fileCount = 1; //TESTING ONLY
        // test the sim exits is file count and file name is wrong
        _simulation = _simulation.replaceAll("\\s+", ""); //DO NOT DELETE!!!
        String sim = (_simulation.toLowerCase().endsWith(".sim")) ? _simulation : (_simulation + ".sim");
        if (!FileFunctions.fileIsAvailable(getCustomerSynchronisedFolder().resolve(sim))) {
            return false;
        }
        return (FileFunctions.countFiles(getCustomerSynchronisedFolder()) >= _fileCount);
    }

    public void checkSimName_FileCount_FileExtension_Scan4Macro() throws Exception { //used in simulation queue/add simulation()
        // test the sim exits is file count and file name is wrong
        // _simulation = _simulation.concat("\"");

        //_fileCount = 1; //TESTING ONLY
        _simulation = _simulation.replaceAll("\\s+", ""); //DO NOT DELETE!!!
        if (_simulation.isEmpty()) { // redundant with simulation file name a required field
            updateJobStatus(JobStatuses.CANCELLED_NULL_SIMULATION_NAME);
            LOG.debug(JobStatuses.CANCELLED_NULL_SIMULATION_NAME);

        } else {
            // Check for .sim file
            String sim = (_simulation.toLowerCase().endsWith(".sim")) ? _simulation : (_simulation + ".sim");
            if (!FileFunctions.fileIsAvailable(getCustomerSynchronisedFolder().resolve(sim))) {
                LOG.debug("Simulation file " + sim + " is NOT available");
                throw new Exception("Simulation " + _jobNumber + " - Simulation file " + sim + " is NOT available");
            } else {
                LOG.debug("Simulation file " + sim + " is available");
            }

            // check file extensions.
            File dir = getCustomerSynchronisedFolder().toFile();
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (child.isFile()) { //we're not copying directories, just files
                        boolean res = false;
                        for (int i = 0; i < ValidExtensions.EXTENSIONS.length; i++) {
                            if (child.getName().toLowerCase().endsWith(ValidExtensions.EXTENSIONS[i])) {
                                res = true;
                            }
                        }
                        if (res == false) {
                            LOG.warn("File " + child.getName() + " extension is not supported");
                            updateJobStatus(JobStatuses.CANCELLED_UNSAFE_FILES_EXTENSION);
                            throw new Exception("File " + child.getName() + " extension is not supported");
                        }
                        if (res == true) {
                            if (child.getName().toLowerCase().endsWith(ValidExtensions.EXTENSIONS[3])) { // 
                                scan4macro.Scan scan = new scan4macro.Scan(child);
                                if (!scan.scan()) {
                                    LOG.warn("!scan.scan()=" + !scan.scan());
                                    LOG.warn("scan4maco returned unsafe operation" + " customer = " + _customerNumber + " job = " + _jobNumber + " File = " + child.getName());
                                    new SendEmail().send(SendEmail.TO, "scan4maco returned unsafe operation", "customer = " + _customerNumber + " job = " + _jobNumber);
                                    updateJobStatus(JobStatuses.CANCELLED_SCAN4MACRO_UNSAFE_OPERATION);
                                    throw new Exception("File " + child.getName() + " SCAN4MACRO_UNSAFE_OPERATION");
                                }
                            }

                        }
                    }
                }
            }

            // Check files count
            LOG.debug("FileFunctions.countFiles(getCustomerSynchronisedFolder()) = " + FileFunctions.countFiles(getCustomerSynchronisedFolder()));
            LOG.debug("_fileCount = " + _fileCount);

            if (FileFunctions.countFiles(getCustomerSynchronisedFolder()) != _fileCount) {

                LOG.warn("!!! Actual file count does not match nominated file count !!!");
                updateJobStatus(JobStatuses.CANCELLED_NOFILEUPLOADED);
                throw new Exception("Simulation " + _jobNumber + "!!! Actual file count does not match nominated file count !!!");
            } else {
                LOG.debug("Actual file count matches nominated file count !!!");
            }
        }
    }

    public void runJobWorkflow() throws Exception {
        // LOG.debug("1+1=" + 1 + 1);
        //_fileCount = 1; //TESTING ONLY
        _startTime = LocalTime.now();

        // for some reason, _simulation is sometimes missing its last " when checking the variable in debug mode. That kills the run processes. The line below is a first attenpt at fixing it
        createJobRunAndSyncFolders();
        createLogAndBackupDirectories(); //remove the commeting out in production
        createLogHeader();
        _printStreamToLogFile.println("Starting processing time: " + _startTime);
        copyCustomerSyncFolderIntoJobRunFolder();
        getCustomerStarCCMPlusVersion();
        selectStarCCMPlusRunVersion(); //LATER: need to read Star-CCM+ installed on machine itself. Adress issue of latest CCM+ version not loaded on Trampo yet.
        createPostprocessingRunFolders();
        RunJob();
        copyLogOutputWindowToFile();
        //runDataExtraction; // 
        // check that the running Job is “alive” in all processes.
        LOG.info("End");
    }

    private void createJobRunAndSyncFolders() throws IOException, Exception {  // test the sim exits thye queue if CANCELLED_SIMULATION_FOLDER_PREEXISTING
//Files.createDirectory(getJobRunningFolderPath());
// Files.createTempFile(simulationSendingToTrampoFolderPath,
        // "tmp",".txt");
        if (Files.isDirectory(getJobRunningFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getJobRunningFolderPath());
            LOG.debug("JobRunningFolder created " + getJobRunningFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {

            updateJobStatus(JobStatuses.CANCELLED_JOB_RUN_FOLDER_PREEXISTING);
            LOG.error(
                    "ERROR: JOBRUNNINGFOLDER EXISTING !!! with Path: " + getJobRunningFolderPath());
            throw new Exception("ERROR: JOBRUNNINGFOLDER EXISTING !!! with Path: " + getJobRunningFolderPath());

            // this needs to make the simulation exist the queue as it indicates a major problem
        }
        if (Files.isDirectory(getJobSynchronisedFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getJobSynchronisedFolderPath());
            LOG.debug("getJobSynchronisedFolderPath Folder created " + getJobSynchronisedFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {
            LOG.error(
                    "ERROR: getJobSynchronisedFolderPath EXISTING !!! with Path: " + getJobSynchronisedFolderPath());
            // this needs to make the simulation exist the queue as it indicates a major problem
            updateJobStatus(JobStatuses.CANCELLED_JOB_SYNC_FOLDER_PREEXISTING);
            throw new Exception("ERROR: getJobSynchronisedFolderPath EXISTING !!! with Path: " + getJobSynchronisedFolderPath());
        }
    }

    private void createLogAndBackupDirectories() throws FileNotFoundException, IOException {
        Files.createDirectories(getJobLogsPath());
        boolean JobLogs = Files.isDirectory(getJobLogsPath(), LinkOption.NOFOLLOW_LINKS);
        LOG.debug("JobLogs isDirectory " + JobLogs);
        LOG.debug(" getJobLogsPath Folder created " + getJobLogsPath());

        Files.createDirectories(getJobBackupPath());
        LOG.debug(" getJobBackupPath Folder created " + getJobBackupPath());

        File log = getJobLogsPath().resolve("job_" + _jobNumber + ".log").toFile(); // this seems t save to the C:/drive for some reason
        LOG.debug("HERE writing job_" + _jobNumber + ".log at path= " + log.toString());
        try {
            //Create the file
            Files.createFile(log.toPath());
            LOG.debug("job_" + _jobNumber + ".log File is created!");
        } catch (IOException ex) {
            LOG.error("job_" + _jobNumber + ".log : File already exists or the operation failed for some reason", ex);
            //LOG.debug("job_" + _jobNumber + ".log : File already exists or the operation failed for some reason");
        }
        _printStreamToLogFile = new PrintStream(log);
//        System.setOut(_printStreamToLogFile);
//        System.setErr(_printStreamToLogFile);
    }

    private void createLogHeader() throws IOException, InterruptedException {

        _printStreamToLogFile.println("HEADER START----------------------------------------------------------------------------------------------------------------");
        _printStreamToLogFile.println("simulationNumber = " + _jobNumber);
        _printStreamToLogFile.println("customerNumber = " + _customerNumber);
        _printStreamToLogFile.println("submissionDate = " + _submissionDate);
        _printStreamToLogFile.println("maxSeconds = " + _maxSeconds);
        _printStreamToLogFile.println("Simulation = " + _simulation);
        _printStreamToLogFile.println("simulationRunningFolder = " + getJobRunningFolderPath());
        _printStreamToLogFile.println("HEADER END-------------------------------------------------------------------------------------------------------------------");
    }

    private void copyCustomerSyncFolderIntoJobRunFolder() throws IOException, InterruptedException {
        LOG.debug("starting CopyCustomerSyncFolderIntoJobRunFolder()");
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
//                    LOG.debug("File Copied: " + child.toString());//.replaceAll(Matcher.quoteReplacement(src.toString()), ""));
//                } else {
//                    LOG.debug("File NOT Copied: " + child.toString());
//                }
//            }
//        }

        LOG.debug("finished CopyCustomerSyncFolderIntoJobRunFolder()");
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
        LOG.debug("version.log created");
        String content = new String(Files.readAllBytes(InitialVersionLogPath));
        LOG.debug("version.log=" + content);

        int index = content.lastIndexOf("STAR-CCM+");
        LOG.debug("index=" + index);
        int startindex = index + 9;
        LOG.debug("versionsectionstartindex=" + startindex);
        int stopindex = startindex + 9;
        LOG.debug("stopindex=" + stopindex);
        String ccmplusversion = content.substring(startindex, stopindex);
        LOG.info("CCM+ version = " + ccmplusversion);
        _StarCcmPlusVersion = ccmplusversion.replace(" ", "");
        Path finalVersionLogPath = getJobLogsPath().resolve("version.log");
        LOG.info("finalVersionLogPath = " + finalVersionLogPath.toString());
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

        LOG.debug(Arrays.deepToString(array));
        String version = array[0][0];
        LOG.debug("version is = " + version.replace(",", ""));
        version = array[1][0];
        LOG.debug("version is = " + version.replace(",", ""));

        _StarCcmPlusVersion = "11.00.011"; // caught by getStarCCMPlusVersion()
        LOG.debug("simulationCcmPlusVersion= " + _StarCcmPlusVersion);
        for (int i = 0; i < array.length; i++) {
            _StarCcmPlusDefaultVersion = array[0][0].replace(",", "");
            _StarCcmPlusDefaultVersionPath = array[0][1].replace(",", "");

            if (array[i][0].replace(",", "").equals(_StarCcmPlusVersion)) {
                _StarCcmPlusVersionPath = array[i][1].replace(",", "");
                LOG.debug("simulationCcmPlusVersionPath= " + array[i][1]);
            }
        }
        if (_StarCcmPlusVersionPath == null) {
            LOG.debug("simulationCcmPlusVersion is NOT installed on compute node");
            LOG.debug("using DEFAULT VERSION");
            _StarCcmPlusVersion = _StarCcmPlusDefaultVersion;
            LOG.info("_StarCcmPlusDefaultVersion= " + _StarCcmPlusDefaultVersion);
            _StarCcmPlusVersionPath = _StarCcmPlusDefaultVersionPath;
            LOG.info("_StarCcmPlusDefaultVersionPath= " + _StarCcmPlusDefaultVersionPath);
        } else {
            LOG.debug("simulationCcmPlusVersion is installed on compute node");

        }

    }

    private void RunJob() throws Exception { //IF process desn't run while testing, i.e. no output a,d a single STAR-CCM+ process starts, make sure you have a sim file in the right folder to run!!!
        //move scenes           
        //if (moveTask == null) {
        moveTaskScenes = new MoveTask(getScenesRunFolderPath().toFile(), getScenesSyncFolderPath().toFile());
        //}
        moveTaskScenes.scheduleFileMove("Scene", Integer.parseInt(config.getProperty("SCHEDULEDMOVEPERIOD"))); // non-blocking

        //move plots
        moveTaskPlots = new MoveTask(getPlotsRunFolderPath().toFile(), getPlotsSyncFolderPath().toFile());
        //}
        moveTaskPlots.scheduleFileMove("Plot", Integer.parseInt(config.getProperty("SCHEDULEDMOVEPERIOD"))); // non-blocking
        //move plots
        moveTaskMesh = new MoveTask(getJobRunningFolderPath().toFile(), getJobSynchronisedFolderPath().toFile());
        //}
        moveTaskMesh.scheduleFileMove("Meshed", Integer.parseInt(config.getProperty("SCHEDULEDMOVEPERIOD"))); // non-blocking

//        moveTaskBackUp = new MoveTask(getJobRunningFolderPath().toFile(), getJobBackupPath().toFile());
//        //}
//        moveTaskBackUp.scheduleFileMove("TrampoBackup", config.getProperty("SCHEDULEDMOVEPERIOD")); // non-blocking
//        
        ProcessBuilder pb = new ProcessBuilder(
                _StarCcmPlusVersionPath, "-batch", TRAMPOCLUSTERUTILFOLDERPATH + "//SmartSimulationHandling.java",
                "-batch-report", "-on", _localHostNP, "-np", config.getProperty("_numberComputeCores"), "-power",
                "-collab", "-licpath", "1999@flex.cd-adapco.com", "-podkey", PODKEY,
                _simulation);

        File pbWorkingDirectory = getJobRunningFolderPath().toFile(); //(new File)?
        pb.directory(pbWorkingDirectory);
        try {
            updateJobStatus(JobStatuses.RUNNING);
            _startSimulationTime = LocalTime.now();
            _printStreamToLogFile.println("Starting simulation time: " + _startSimulationTime);
            _simulationProcess = pb.start();
            LOG.info("simulation run process started");

            //Redirection of stream and loop extremely important; http://baxincc.cc/questions/216451/windows-process-execed-from-java-not-terminating
            // if not redirected, Star-CCM+ processes hang and -batch*-report doesn't print
            InputStream stdout = _simulationProcess.getInputStream();
            while (stdout.read() >= 0) {
            }

            _simulationProcess.waitFor();
            //Stop the automatic move of Scenes and Plots from run folder to sync folder
            moveTaskScenes.cancelPurgeTimer();
            moveTaskPlots.cancelPurgeTimer();
            moveTaskMesh.cancelPurgeTimer();

            //REMOVE POD key FROM HTML report
            File[] directoryListing = getJobSynchronisedFolderPath().toFile().listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS) && child.getName().toLowerCase().contains(".html")) {
                        LOG.debug("HTML report file name = " + child.getName());
                        String content = new String(Files.readAllBytes(child.toPath()), Charset.forName("UTF-8"));
                        int index = content.indexOf("-podkey");
                        LOG.debug("indexOf -podkey in HTML report = " + index);
                        LOG.debug("OLD content.substring(index, index + 25) in HTML report = " + content.substring(index, index + 10));
                        content = content.replace(content.substring(index, index + 25), "-podkey XXXXXXXXXXXXXXXXX");
                        LOG.debug("NEW content.substring(index, index + 25) in HTML report = " + content.substring(index, index + 10));
                        try (PrintWriter out = new PrintWriter(new FileOutputStream(child.toPath().toString(), false))) {
                            out.println(content);
                        }
                    }
                }
            }
            //end of the run file move
            File sourceDirectory = pbWorkingDirectory;

            File destinationDirectory = getJobLogsPath().toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "log");

            destinationDirectory = getJobSynchronisedFolderPath().toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Trampo");

            sourceDirectory = getTablesRunFolderPath().toFile();
            destinationDirectory = getTablesSyncFolderPath().toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

            sourceDirectory = getStarViewRunFolderPath().toFile();
            destinationDirectory = getStarViewSyncFolderPath().toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

            sourceDirectory = getPowerPointRunFolderPath().toFile();
            destinationDirectory = getPowerPointSyncFolderPath().toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

            sourceDirectory = getJobSynchronisedFolderPath().toFile();
            destinationDirectory = getJobBackupPath().toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Backup");

            //delete anything left in the run/customer directory.  for symlink handling see http://stackoverflow.com/questions/779519/delete-directories-recursively-in-java/27917071#27917071
            Files.walkFileTree(getJobRunningFolderPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);

                    return FileVisitResult.CONTINUE;
                }
            });

            Files.walkFileTree(Paths.get(RUNROOT, getCustomerFolderRelativePath()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    //Files.delete(Paths.get(RUNROOT, getCustomerFolderRelativePath()));
                    return FileVisitResult.CONTINUE;
                }
            });

            // All below 
            _printStreamToLogFile.println("End simulation time: " + LocalTime.now()); // this doesn't seem to be done at the end of the process.
            _printStreamToLogFile.println("Simulation/Total processing time: " + (int) timeInSeconds(_startSimulationTime) + "s/" + (int) timeInSeconds(_startTime) + "s");
            updateJobActualRuntime();
            updateJobStatus(JobStatuses.COMPLETED);
            _printStreamToLogFile.println("Simulation complete...");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateJobActualRuntime() throws Exception {
        WebAppGate.make().updateJobActualRuntime(this, (int) timeInSeconds(_startSimulationTime));
    }

    private void copyLogOutputWindowToFile() throws IOException, InterruptedException {
        Files.copy(getOutputWindowLogToFile(), getJobLogsPath().resolve("outputWindowToFileJob_" + _jobNumber + ".log"), COPY_ATTRIBUTES);
        LOG.debug("OUTPUTWINDOWLOGTOFILELOCATION COPIED");
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
        if (!_isAborting) {
            File file = new File(getJobRunningFolderPath() + "\\ABORT.txt");

            try {
                //Create the file
                file.createNewFile();
                LOG.debug("ABORT.txt File is created!");
            } catch (IOException ex) {
                LOG.error("ABORT.txt File already exists or the operation failed for some reason", ex);
                //                LOG.debug("ABORT.txt File already exists or the operation failed for some reason");
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

    private Path getJobRunningFolderPath() {// the Job running folder
        return Paths.get(RUNROOT, getCustomerFolderRelativePath(), "Job_" + _jobNumber);
    }

    private Path getJobSynchronisedFolderPath() {// the Job synchronised folder where trampo send the results back live.
        return Paths.get(DATAROOT, getCustomerFolderRelativePath(), "Synchronised folder", "Job_" + _jobNumber);
    }

    private Path getJobLogsPath() {
        return Paths.get(DATAROOT, getCustomerFolderRelativePath(), "Job_" + _jobNumber, "logs");
    }

    private Path getJobBackupPath() {
        return Paths.get(DATAROOT, getCustomerFolderRelativePath(), "Job_" + _jobNumber, "backup");
    }

    private String getJobStatusPath() {
        return getJobRunningFolderPath() + "Job_" + _jobNumber + "_Status.txt";
    }

    private Path getOutputWindowLogToFile() { // might need update to work for all versions
        return Paths.get("C:\\Users\\Administrator\\AppData\\Local\\CD-adapco\\STAR-CCM+ " + _StarCcmPlusVersion + "\\var\\log\\messages.log");
    }

    private void createPostprocessingRunFolders() throws IOException, Exception {
        //scenes
        if (Files.isDirectory(getScenesSyncFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getScenesSyncFolderPath());
            LOG.debug("getRunScenesFolderPath created " + getScenesSyncFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {
            LOG.error(
                    "ERROR: getRunScenesFolderPath EXISTING !!! with Path: " + getScenesSyncFolderPath());
            throw new Exception("ERROR: getScenesSyncFolderPath EXISTING !!! with Path: " + getScenesSyncFolderPath());
        }
        //plots
        if (Files.isDirectory(getPlotsSyncFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getPlotsSyncFolderPath());
            LOG.debug("getRunPlotsFolderPath created " + getPlotsSyncFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {
            LOG.error(
                    "ERROR: getRunPlotsFolderPath EXISTING !!! with Path: " + getPlotsSyncFolderPath());
            throw new Exception("ERROR: getPlotsSyncFolderPath EXISTING !!! with Path: " + getPlotsSyncFolderPath());
        }

        //tables
        if (Files.isDirectory(getTablesSyncFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getTablesSyncFolderPath());
            LOG.debug("getRunTablesFolderPath created " + getTablesSyncFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {
            LOG.error(
                    "ERROR: getRunTablesFolderPath EXISTING !!! with Path: " + getTablesSyncFolderPath());
            throw new Exception("ERROR: getTablesSyncFolderPath EXISTING !!! with Path: " + getTablesSyncFolderPath());
        }

        //Starview
        if (Files.isDirectory(getStarViewSyncFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getStarViewSyncFolderPath());
            LOG.debug("getRunStarViewFolderPath created " + getStarViewSyncFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {
            LOG.error(
                    "ERROR: getRunStarViewFolderPath EXISTING !!! with Path: " + getStarViewSyncFolderPath());
            //throw new Exception("ERROR: getStarViewSyncFolderPath EXISTING !!! with Path: " + getStarViewSyncFolderPath());
        }

        //PowerPoint
        if (Files.isDirectory(getPowerPointSyncFolderPath(), LinkOption.NOFOLLOW_LINKS) == false) {
            Files.createDirectories(getPowerPointSyncFolderPath());
            LOG.debug("getRunPowerPointFolderPath created " + getPowerPointSyncFolderPath());
            //LOG.debug("src folder will show below as Directory copied");
        } else {
            LOG.error(
                    "ERROR: getRunPowerPointFolderPath EXISTING !!! with Path: " + getPowerPointSyncFolderPath());
            //throw new Exception("ERROR: getPowerPointSyncFolderPath EXISTING !!! with Path: " + getPowerPointSyncFolderPath());
        }
    }

    private Path getScenesSyncFolderPath() {
        return Paths.get(getJobSynchronisedFolderPath().toString(), "Scenes");
    }

    private Path getScenesRunFolderPath() {
        return Paths.get(getJobRunningFolderPath().toString(), "Scenes");
    }

    private Path getPlotsSyncFolderPath() {
        return Paths.get(getJobSynchronisedFolderPath().toString(), "Plots");
    }

    private Path getPlotsRunFolderPath() {
        return Paths.get(getJobRunningFolderPath().toString(), "Plots");
    }

    private Path getTablesSyncFolderPath() {

        return Paths.get(getJobSynchronisedFolderPath().toString(), "Tables");
    }

    private Path getTablesRunFolderPath() {

        return Paths.get(getJobRunningFolderPath().toString(), "Tables");
    }

    private Path getStarViewSyncFolderPath() {

        return Paths.get(getJobSynchronisedFolderPath().toString(), "StarView");
    }

    private Path getStarViewRunFolderPath() {

        return Paths.get(getJobRunningFolderPath().toString(), "StarView");
    }

    private Path getPowerPointSyncFolderPath() {

        return Paths.get(getJobSynchronisedFolderPath().toString(), "PowerPoint");
    }

    private Path getPowerPointRunFolderPath() {

        return Paths.get(getJobRunningFolderPath().toString(), "PowerPoint");
    }

    public String getCustomerNumber() {
        return _customerNumber;
    }

    public void markAsCanceled() {
        _maxSeconds = ((_startSimulationTime == null) ? 0 : (long) timeInSeconds(_startSimulationTime));
    }

    public void updateMaximumClocktimeInSecondsFromWebApp() {

        try {
            _maxSeconds = WebAppGate.make().getJobMaxRuntime(this);
        } catch (Exception e) {
            // Can't update from the webapp, best to play it safe and keep the max seconds as is
        }

    }

    public long maximumClocktimeInSeconds() {
        return _maxSeconds;
    }

    public void updateJobStatus(String newStatus) throws Exception {
        WebAppGate.make().updateJobStatus(this, newStatus);
        if (_printStreamToLogFile != null) {
            _printStreamToLogFile.println(LocalTime.now() + ": Job status updated to " + newStatus);
        }
        LOG.info("Job status updated to " + newStatus);
    }

    /**
     *
     * moves all files containing the string from the source directory to the
     * destination directory both directory need to exist!
     *
     * @param source
     * @param destination
     * @param string
     * @throws java.io.IOException
     */
    public void ConditionalMoveFiles(File source, File destination, String string) throws IOException {
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS) && child.getName().toLowerCase().contains(string.toLowerCase())) {
                    LOG.debug("directoryListing child.getName = " + child.getName());
                    Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                    LOG.debug(" directoryListing child moved to  = " + destination.toPath().resolve(child.getName()));

                }
            }
        }
    }
}
