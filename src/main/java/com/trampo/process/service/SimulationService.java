package com.trampo.process.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import com.trampo.process.domain.ByoLicensingType;
import com.trampo.process.domain.CcmPlus;
import com.trampo.process.domain.Job;
import com.trampo.process.domain.JobStatus;
import com.trampo.process.domain.Simulation;
import com.trampo.process.domain.SimulationStatus;
import com.trampo.process.domain.StarCcmPrecision;
import com.trampo.process.exception.RestException;
import com.trampo.process.util.FileUtils;
import com.trampo.process.util.MoveTask;
import com.trampo.process.util.MyResponseErrorHandler;
import com.trampo.process.util.Scan;
import com.trampo.process.util.SendEmail;
import com.trampo.process.util.StarCcmPlusUtil;
import com.trampo.process.util.ValidExtensions;

@Component
public class SimulationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationService.class);

    private JobService jobService;
    private Map<String, List<MoveTask>> moveTaskMap = new HashMap<>();

    String starCcmPlusVersion = null;
    String starCcmPlusVersionPath = null;

    private String productionOrTestingSwitch;
    private RestTemplate restTemplate;
    private ObjectMapper mapper;
    private String scheduleMovePeriod;
    private String dataRoot;
    private String runRoot;
    private String dataRootGadi;
    private String runRootGadi;
    private Integer maxWaitForFilesInDays;
    private String backendScriptPath;
    private String podKey;
    private String trampoLicensePath;
    private String macroPath;
    private String meshAndRunMacroPath;
    private SshService sshService;
    private MailService mailService;
    FileAttribute<Set<PosixFilePermission>> fileAttributes = null;

    @Autowired
    public SimulationService(RestTemplateBuilder builder, JobService jobService,
            SshService sshService, MailService mailService,
            @Value("${trampo.simulation.productionOrTestingSwitch}") String productionOrTestingSwitch,
            @Value("${webapp.api.root}") String apiRoot,
            @Value("${trampo.simulation.scheduleMovePeriod}") String scheduleMovePeriod,
            @Value("${trampo.simulation.dataRoot}") String dataRoot,
            @Value("${trampo.simulation.runRoot}") String runRoot,
            @Value("${trampo.simulation.dataRootGadi}") String dataRootGadi,
            @Value("${trampo.simulation.runRootGadi}") String runRootGadi,
            @Value("${trampo.simulation.maxWaitForFilesInDays}") Integer maxWaitForFilesInDays,
            @Value("${trampo.simulation.backendScriptPath}") String backendScriptPath,
            @Value("${trampo.simulation.podKey}") String podKey,
             @Value("${trampo.simulation.trampoLicensePath}") String trampoLicensePath,
            @Value("${trampo.simulation.macroPath}") String macroPath,
            @Value("${trampo.simulation.meshAndRunMacroPath}") String meshAndRunMacroPath) {
        restTemplate = builder.rootUri(apiRoot).build();
        MyResponseErrorHandler errorHandler = new MyResponseErrorHandler();
        restTemplate.setErrorHandler(errorHandler);
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.productionOrTestingSwitch = productionOrTestingSwitch;
        this.scheduleMovePeriod = scheduleMovePeriod;
        this.dataRoot = dataRoot;
        this.runRoot = runRoot;
        this.dataRootGadi = dataRootGadi;
        this.runRootGadi = runRootGadi;
        this.maxWaitForFilesInDays = maxWaitForFilesInDays;
        this.jobService = jobService;
        this.backendScriptPath = backendScriptPath;
        this.podKey = podKey;
        this.trampoLicensePath= trampoLicensePath;
        this.macroPath = macroPath;
        this.meshAndRunMacroPath = meshAndRunMacroPath;
        this.sshService = sshService;
        this.mailService = mailService;
        // add permission as rwxrwxrwx 770
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.GROUP_WRITE);
        // perms.add(PosixFilePermission.OTHERS_READ);
        // perms.add(PosixFilePermission.OTHERS_WRITE);
        // perms.add(PosixFilePermission.OTHERS_EXECUTE);
        fileAttributes = PosixFilePermissions.asFileAttribute(perms);
    }

    public Simulation getSimulation(String simulationId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = new HashMap<>();
            HttpEntity<String> postEntity
                    = new HttpEntity<String>(mapper.writeValueAsString(request), headers);
            ResponseEntity<String> response = restTemplate.exchange("/simulations/" + simulationId,
                    HttpMethod.GET, postEntity, String.class);
            checkError(response);
            return mapper.readValue(response.getBody(), Simulation.class);
        } catch (Exception e) {
            LOGGER.error("Error while getting simulation", e);
        }
        return null;
    }

    public List<Simulation> getByStatus(SimulationStatus status) {
        try {
            LOGGER.info("getting simulatios in status: " + status);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = new HashMap<>();
            HttpEntity<String> postEntity
                    = new HttpEntity<String>(mapper.writeValueAsString(request), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/simulations/getByStatus/?status=" + status, HttpMethod.GET, postEntity, String.class);
            checkError(response);
            List<Simulation> list
                    = mapper.readValue(response.getBody(), new TypeReference<List<Simulation>>() {
                    });
            LOGGER.info("found " + list.size() + " simulatios in status: " + status);
            return list;
        } catch (Exception e) {
            LOGGER.error("Error while getting simulations by status", e);
        }
        return new ArrayList<>();
    }

    public void updateStatus(String simulationId, SimulationStatus status) {
        try {
            LOGGER.info("Updating simulation: " + simulationId + " to status: " + status);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = new HashMap<>();
            HttpEntity<String> postEntity
                    = new HttpEntity<String>(mapper.writeValueAsString(request), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/simulations/updateStatus/?status=" + status + "&id=" + simulationId, HttpMethod.GET,
                    postEntity, String.class);
            checkError(response);
            LOGGER.info(
                    "Updating simulation: " + simulationId + " to status: " + status + "is successfull");
        } catch (Exception e) {
            LOGGER.error("Error while updating status", e);
        }
    }

    public void updateWalltime(String simulationId, long walltime) {
        LOGGER.info("Updating simulation: " + simulationId + " walltime: " + walltime);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = new HashMap<>();
            HttpEntity<String> postEntity
                    = new HttpEntity<String>(mapper.writeValueAsString(request), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/simulations/updateWalltime/?actualWalltime=" + walltime + "&id=" + simulationId,
                    HttpMethod.GET, postEntity, String.class);
            checkError(response);
            LOGGER.info("Updated simulation: " + simulationId + " walltime: " + walltime);
        } catch (Exception e) {
            LOGGER.error("Error while updating walltime", e);
        }
    }

    public void error(Simulation simulation, Job job, String errorMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = new HashMap<>();
            HttpEntity<String> postEntity
                    = new HttpEntity<String>(mapper.writeValueAsString(request), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/simulations/error/?errorMessage=" + errorMessage + "&id=" + simulation.getId(),
                    HttpMethod.GET, postEntity, String.class);
            checkError(response);
            mailService.sendSimulationErrorEmails(simulation, job, errorMessage);
        } catch (Exception e) {
            LOGGER.error("Error while updating status to error", e);
        }
    }

    private void checkError(ResponseEntity<String> response) throws RestException {
        if (!response.getStatusCode().is2xxSuccessful()) {
            LOGGER.error("Rest Exception: Status Code: " + response.getStatusCode() + " body: "
                    + response.getBody());
            throw new RestException();
        }
    }

    public void startSimulation(Simulation simulation) {
        if (!simulation.getFolderName().startsWith("sim")) {
            error(simulation, null, "Simulation name must start with sim!!!");
            return;
        }
        if (areFilesAvailable(simulation)) {
            updateStatus(simulation.getId(), SimulationStatus.MOVING_FILES);
            mailService.sendFileUploadCompletedEmails(simulation);
            try {
                //checkSimName_FileCount_FileExtension_Scan4Macro(simulation);

                createJobRunAndSyncFolders(simulation);
                if (simulation.getStatus().equals(SimulationStatus.ERROR)) {
                    return;
                }
                createLogAndBackupDirectories(simulation);
                createLogHeader(simulation);
                copyCustomerSyncFolderIntoJobRunFolder(simulation);
                Files.delete(getCustomerSynchronisedFolderSimulationFolderFullPath(simulation));
                getCustomerStarCCMPlusVersion(simulation);
                selectStarCCMPlusRunVersion(simulation);

                if ((simulation.getMesh() == null || !simulation.getMesh())
                        && (simulation.getRun() == null || !simulation.getRun())) {
                    createPostprocessingRunFolders(simulation);
                } // should be done only when using Smart Simulation Handling
                if (simulation.getStatus().equals(SimulationStatus.ERROR)) {
                    return;
                }
                RunJob(simulation);
            } catch (Exception e) {
                LOGGER.error("Error while starting siumlation", e);
            }
        } else {
            if (simulation.getStatus().equals(SimulationStatus.NEW)) {
                updateStatus(simulation.getId(), SimulationStatus.WAITING_FOR_FILES);
            } else {
                LOGGER.info("simulation date created: " + simulation.getDateCreated());
                if (simulation.getDateCreated().plusDays(maxWaitForFilesInDays)
                        .compareTo(LocalDateTime.now()) < 0) {
                    LOGGER.info("simulation date created plus days: "
                            + simulation.getDateCreated().plusDays(maxWaitForFilesInDays));
                    LOGGER.info("now: " + LocalDateTime.now());
                    LOGGER.error("files took too long to upload. simulation id: " + simulation.getId());
                    error(simulation, null, "files took too long to upload");
                }
            }
        }
    }

    public void cancelSimulation(Simulation simulation, Job job) {
        File file = new File(getJobRunningFolderPath(simulation) + "/ABORT");
        try {
            // Create the file
            file.createNewFile();
            LOGGER.info("ABORT File are created! " + file.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.error("ABORT File already exists or the operation failed for some reason", ex);
        }

        File file1 = new File(getJobRunningFolderPath(simulation) + "/LOOPABORT");
        try {
            // Create the file
            file1.createNewFile();
            LOGGER.info("LOOPABORT File are created! " + file1.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.error("LOOPABORT File already exists or the operation failed for some reason", ex);
        }

        Runnable r = () -> {
            try {
                Thread.sleep(8 * 60 * 1000);
            } catch (InterruptedException e) {
                LOGGER.error("Error while cancelling simulation", e);
            }
            try {
                List<Job> list = jobService.getCurrentJobs();
                for (Job j : list) {
                    if (j.getId().equals(job.getId()) && job.getStatus().equals(JobStatus.R)) {
                        jobService.cancelJob(job.getId());
                    }
                }
            } catch (JSchException | IOException e) {
                LOGGER.error("Error while cancelling simulation", e);
            }
        };
        Executors.newSingleThreadExecutor().execute(r);
    }

    public void finishSimulation(Simulation simulation) {
        String command = "chmod -R 770 " + getJobRunningFolderPathGadi(simulation);
        System.err.println("finish command ");
        sshService.execCommand(command);

        File pbWorkingDirectory = getJobRunningFolderPath(simulation).toFile();
        try {

            LOGGER.info("Sleeping started");

            // wait one more cycle to be sure that file transfers complete
            Thread.sleep(60 * 1000);

            LOGGER.info("Sleeping finished");

            List<MoveTask> moveTaskList = moveTaskMap.get(simulation.getId());
            if (moveTaskList != null) {
                for (MoveTask moveTask : moveTaskList) {
                    moveTask.cancelPurgeTimer();
                }
            }

            LOGGER.info("Move task purge finished");

            // REMOVE POD key FROM HTML report
            File[] directoryListing = pbWorkingDirectory.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)
                            && child.getName().toLowerCase().contains(".html")) {
                        LOGGER.info("HTML report file name = " + child.getName());
                        String content
                                = new String(Files.readAllBytes(child.toPath()), Charset.forName("UTF-8"));
                        int index = content.indexOf("-podkey");
                        LOGGER.info("indexOf -podkey in HTML report = " + index);
                        LOGGER.info("OLD content.substring(index, index + 25) in HTML report = "
                                + content.substring(index, index + 10));
                        content
                                = content.replace(content.substring(index, index + 25), "-podkey XXXXXXXXXXXXXXXXX");
                        LOGGER.info("NEW content.substring(index, index + 25) in HTML report = "
                                + content.substring(index, index + 10));
                        try (PrintWriter out
                                = new PrintWriter(new FileOutputStream(child.toPath().toString(), false))) {
                            out.println(content);
                        }
                    }
                }
            }
            // end of the run file move

            LOGGER.info("Starting to move logs and stuff");

            File sourceDirectory = pbWorkingDirectory; // JobRunningFolder

            File destinationDirectory = getJobLogsPath(simulation).toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "log");
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[1]);
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[2]);
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[3]);

            destinationDirectory = getJobSynchronisedFolderPath(simulation).toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory);
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Trampo");
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[0]);
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[4]);

            if ((simulation.getMesh() == null || !simulation.getMesh())
                    && (simulation.getRun() == null || !simulation.getRun())) {

                sourceDirectory = getTablesRunFolderPath(simulation).toFile();
                destinationDirectory = getTablesSyncFolderPath(simulation).toFile();
                ConditionalMoveFiles(sourceDirectory, destinationDirectory);

                sourceDirectory = getStarViewRunFolderPath(simulation).toFile();
                destinationDirectory = getStarViewSyncFolderPath(simulation).toFile();
                ConditionalMoveFiles(sourceDirectory, destinationDirectory);

                sourceDirectory = getPowerPointRunFolderPath(simulation).toFile();
                destinationDirectory = getPowerPointSyncFolderPath(simulation).toFile();
                ConditionalMoveFiles(sourceDirectory, destinationDirectory);
            }
            LOGGER.info("Finished condional move files");
            sourceDirectory = getJobSynchronisedFolderPath(simulation).toFile();
            destinationDirectory = getJobBackupPath(simulation).toFile();
            ConditionalCopyFiles(sourceDirectory, destinationDirectory);
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Backup");
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[0]);
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[4]);

            // sourceDirectory = getJobSynchronisedFolderPath(simulation).toFile();
            // destinationDirectory =
            // getCustomerSynchronisedFolderSimulationFolderFullPath(simulation).toFile();
            // ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Backup");
            LOGGER.info("Finished backup move files");

            // list all files before deleting run folder
            Files.walk(getJobRunningFolderPath(simulation)).forEach(p -> LOGGER.info(p.toString()));

            // deletes run folder
            org.apache.tomcat.util.http.fileupload.FileUtils
                    .deleteDirectory(getJobRunningFolderPath(simulation).toFile());

            LOGGER.info("Run folder deleted");

            // All below
            // _printStreamToLogFile.println("End simulation time: " + LocalTime.now()); // this doesn't
            // seem to be done at the end of the process.
            // _printStreamToLogFile.println("Simulation/Total processing time: " + (int)
            // timeInSeconds(_startSimulationTime) + "s/" + (int) timeInSeconds(_startTime) + "s");
            // updateJobActualRuntime();
            // updateJobStatus(JobStatuses.COMPLETED);
            // _printStreamToLogFile.println("Simulation complete...");
        } catch (IOException | NumberFormatException | InterruptedException e) {
            LOGGER.error("Error while finishing simulaton", e);
        }
        LOGGER.info("ending run job");
    }

    private void checkSimName_FileCount_FileExtension_Scan4Macro(Simulation simulation) {
        if (simulation.getFolderName().isEmpty()) {
            error(simulation, null, "Simulation folder name was not input");
            LOGGER.error("Simulation cancelled. id: " + simulation.getId()
                    + " Simulation folder name was not input");
            return;
        }

        // Check for folder
        if (!Files.isDirectory(getCustomerSynchronisedFolderSimulationFolderFullPath(simulation))) {
            LOGGER.error("Simulation cancelled. id: " + simulation.getId() + " Simulation folder "
                    + getCustomerSynchronisedFolderSimulationFolderFullPath(simulation)
                    + " is NOT available");
            error(simulation, null, "Simulation folder is NOT available!!!");
            return;
        }

        LOGGER.info("Simulation folder "
                + getCustomerSynchronisedFolderSimulationFolderFullPath(simulation) + " is available");

        // Check for .sim file
        boolean simFileExist = false;
        try {
            Iterator<Path> fileIt
                    = Files.list(getCustomerSynchronisedFolderSimulationFolderFullPath(simulation)).iterator();
            while (fileIt.hasNext()) {
                Path file = fileIt.next();
                if (file.toString().endsWith(".sim")) {
                    simFileExist = true;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to count files in "
                    + getCustomerSynchronisedFolderSimulationFolderFullPath(simulation), e);
        }
        if (!simFileExist) {
            error(simulation, null, "Simulation File Does not Exist!!!");
        }

        // check file extensions.
        File dir = getCustomerSynchronisedFolderSimulationFolderFullPath(simulation).toFile();
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.isFile()) { // we're not copying directories, just files
                    boolean res = false;
                    for (int i = 0; i < ValidExtensions.EXTENSIONS.length; i++) {
                        if (child.getName().toLowerCase().endsWith(ValidExtensions.EXTENSIONS[i])) {
                            res = true;
                        }
                    }
                    if (!res) {
                        LOGGER.error("File " + child.getName() + " extension is not supported");
                        error(simulation, null, "Unsafe uploaded files extension");
                    } else {
                        if (child.getName().toLowerCase().endsWith(ValidExtensions.EXTENSIONS[3])) { //
//              Scan scan = new Scan(child);
//              if (!scan.scan()) {
//                LOGGER.error("!scan.scan()=" + !scan.scan());
//                LOGGER.error("scan4maco returned unsafe operation" + " customer = "
//                    + simulation.getCustomerId() + " job = " + simulation.getId() + " File = "
//                    + child.getName());
//                new SendEmail().send(SendEmail.TO, "scan4maco returned unsafe operation",
//                    "customer = " + simulation.getCustomerId() + " job = " + simulation.getId());
//                error(simulation, null, "Scan4Macro detected an unsafe operation");
//                return;
//              }
                        }
                    }
                }
            }
        }

        // Check files count
        int count = 0;
        int simCount = 0;
        try {
            Iterator<Path> fileIt
                    = Files.list(getCustomerSynchronisedFolderSimulationFolderFullPath(simulation)).iterator();
            while (fileIt.hasNext()) {
                Path file = fileIt.next();
                if (Files.isRegularFile(file)) {
                    count++;
                }
                if (file.toString().endsWith(".sim")) {
                    simCount++;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to count files in "
                    + getCustomerSynchronisedFolderSimulationFolderFullPath(simulation), e);
            count = 0;
        }

        if (count != simulation.getFileCount() || simCount != 1) {
            LOGGER.error("!!! Actual file count does not match nominated file count !!!");
            error(simulation, null, "number of files in sync folder not matching");
            return;
        } else {
            LOGGER.info("Actual file count matches nominated file count !!!");
        }
    }

    private void RunJob(Simulation simulation) {
        LOGGER.info("starting run job");

        String runRoot = getJobRunningFolderPathGadi(simulation).toString();

        if ((simulation.getMesh() == null || !simulation.getMesh())
                && (simulation.getRun() == null || !simulation.getRun())) {

            List<MoveTask> moveTaskList = new ArrayList<MoveTask>();

            MoveTask moveTaskScenes = new MoveTask(getScenesRunFolderPath(simulation).toFile(),
                    getScenesSyncFolderPath(simulation).toFile(), sshService, runRoot);
            LOGGER.info("moveTaskScenes = new MoveTask done");

            moveTaskScenes.scheduleFileMove(".", Integer.parseInt(scheduleMovePeriod)); // non-blocking
            LOGGER.info("moveTaskScenes running");
            moveTaskList.add(moveTaskScenes);

            MoveTask moveTaskPlots = new MoveTask(getPlotsRunFolderPath(simulation).toFile(),
                    getPlotsSyncFolderPath(simulation).toFile(), sshService, runRoot);

            moveTaskPlots.scheduleFileMove(".", Integer.parseInt(scheduleMovePeriod)); // non-blocking
            moveTaskList.add(moveTaskPlots);
            LOGGER.info("moveTaskPlots running");

            MoveTask moveTaskStarView = new MoveTask(getStarViewRunFolderPath(simulation).toFile(),
                    getStarViewSyncFolderPath(simulation).toFile(), sshService, runRoot);

            moveTaskStarView.scheduleFileMove(".", Integer.parseInt(scheduleMovePeriod)); // non-blocking
            moveTaskList.add(moveTaskStarView);
            LOGGER.info("moveTaskStarView running");

            MoveTask moveTaskPowerPoint = new MoveTask(getPowerPointRunFolderPath(simulation).toFile(),
                    getPowerPointSyncFolderPath(simulation).toFile(), sshService, runRoot);

            moveTaskPowerPoint.scheduleFileMove(".", Integer.parseInt(scheduleMovePeriod)); // non-blocking
            moveTaskList.add(moveTaskPowerPoint);
            LOGGER.info("moveTaskPowerPoint running");

            MoveTask moveTaskTables = new MoveTask(getTablesRunFolderPath(simulation).toFile(),
                    getTablesSyncFolderPath(simulation).toFile(), sshService, runRoot);

            moveTaskTables.scheduleFileMove(".", Integer.parseInt(scheduleMovePeriod)); // non-blocking
            moveTaskList.add(moveTaskTables);
            LOGGER.info("moveTaskTables running");

            moveTaskMap.put(simulation.getId(), moveTaskList);

        } // should be done only when using Smart Simulation Handling

        int corePerNode = 0;
        int coreCount = 0;
        String queueType;
        int memory;
        if (simulation.getProcessorType().equals("INSTANT")) {
            coreCount = simulation.getNumberOfNodesStandardLowPriority() * 16;
            queueType = "express";
            memory = 30 * simulation.getNumberOfNodesStandardLowPriority();
        } else if (simulation.getProcessorType().equals("FAST")) {
            coreCount = simulation.getNumberOfNodesFast();
            queueType = "normal";
            memory = 190 * simulation.getNumberOfNodesFast();
            corePerNode = 48;
        } else {
            coreCount = simulation.getNumberOfNodesStandardLowPriority() * 16;
            queueType = "normal";
            memory = 30 * simulation.getNumberOfNodesStandardLowPriority();
        }
        if (productionOrTestingSwitch.equals("test")) {
            coreCount = 1;
        
                if (simulation.getProcessorType().equals("FAST")) {
                memory = 4;
                } else {
                memory = 3;
                }
        }
        String walltime = "000:00:00";
        long hours = 0;
        long minutes = 0;
        if (simulation.getMaxWalltime() > 60) {
            hours = simulation.getMaxWalltime() / 60;
            minutes = simulation.getMaxWalltime() - hours * 60;
        } else {
            minutes = simulation.getMaxWalltime();
        }
        walltime = String.format("%03d", hours) + ":" + String.format("%02d", minutes) + ":00";
        String simulationFileName = getCustomerSimulationFilePathGadi(simulation);
        String podKeyToSubmit = podKey;
        String licensePath = trampoLicensePath;
        if (simulation.getByoLicensingType().equals(ByoLicensingType.POD)) {
            licensePath=trampoLicensePath;
            podKeyToSubmit = simulation.getPodKey();
        }
        boolean meshOnly = false;
        if (simulation.getMesh() != null) {
            meshOnly = simulation.getMesh();
        }
        boolean runOnly = false;
        if (simulation.getRun() != null) {
            runOnly = simulation.getRun();
        }
        jobService.submitJob(simulation.getId(), "" + coreCount, memory + "", queueType,
                backendScriptPath, walltime, getJobLogsPathGadi(simulation).toString(), macroPath,
                meshAndRunMacroPath, simulationFileName,licensePath, podKeyToSubmit,
                getCustomerDataRoot(simulation).toString(), getJobRunningFolderPath(simulation).toString(),
                getJobRunningFolderPathGadi(simulation).toString(), starCcmPlusVersionPath, meshOnly,
                runOnly, corePerNode);
        updateStatus(simulation.getId(), SimulationStatus.SUBMITTED);
    }

    private String getCustomerSimulationFilePathGadi(Simulation simulation) {
        try {
            Iterator<Path> fileIt = Files.list(getJobRunningFolderPath(simulation)).iterator();
            while (fileIt.hasNext()) {
                Path file = fileIt.next();
                if (file.toString().endsWith(".sim")) {
                    return getJobRunningFolderPathGadi(simulation).resolve(file.getFileName()).toString();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to count files in " + getRunSimulationFolderFullPath(simulation), e);
        }
        return null;
    }

    private Path getPowerPointRunFolderPath(Simulation simulation) {
        return Paths.get(getJobRunningFolderPath(simulation).toString(), "PowerPoint");
    }

    private Path getStarViewRunFolderPath(Simulation simulation) {
        return Paths.get(getJobRunningFolderPath(simulation).toString(), "StarView");
    }

    private Path getTablesRunFolderPath(Simulation simulation) {
        return Paths.get(getJobRunningFolderPath(simulation).toString(), "Tables");
    }

    private Path getPlotsRunFolderPath(Simulation simulation) {
        return Paths.get(getJobRunningFolderPath(simulation).toString(), "Plots");
    }

    private Path getScenesRunFolderPath(Simulation simulation) {
        return Paths.get(getJobRunningFolderPath(simulation).toString(), "Scenes");
    }

    private void createPostprocessingRunFolders(Simulation simulation) {
        FileUtils.runChmod(getJobSynchronisedFolderPath(simulation).toString());
        try {
            // scenes
            if (Files.isDirectory(getScenesSyncFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getScenesSyncFolderPath(simulation), fileAttributes);
                FileUtils.runChmod(getScenesSyncFolderPath(simulation).toString());
                FileUtils.logFilePermissions(getScenesSyncFolderPath(simulation));
                LOGGER.info("getRunScenesFolderPath created " + getScenesSyncFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: getRunScenesFolderPath EXISTING !!! with Path: "
                        + getScenesSyncFolderPath(simulation));
                error(simulation, null, "scenes folder path EXISTING !!!");
            }
            // plots
            if (Files.isDirectory(getPlotsSyncFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getPlotsSyncFolderPath(simulation), fileAttributes);
                FileUtils.runChmod(getPlotsSyncFolderPath(simulation).toString());
                FileUtils.logFilePermissions(getPlotsSyncFolderPath(simulation));
                LOGGER.info("getRunPlotsFolderPath created " + getPlotsSyncFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: getRunPlotsFolderPath EXISTING !!! with Path: "
                        + getPlotsSyncFolderPath(simulation));
                error(simulation, null, "plots sync folder path EXISTING !!!");
            }

            // tables
            if (Files.isDirectory(getTablesSyncFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getTablesSyncFolderPath(simulation), fileAttributes);
                FileUtils.runChmod(getTablesSyncFolderPath(simulation).toString());
                FileUtils.logFilePermissions(getTablesSyncFolderPath(simulation));
                LOGGER.info("getRunTablesFolderPath created " + getTablesSyncFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: getRunTablesFolderPath EXISTING !!! with Path: "
                        + getTablesSyncFolderPath(simulation));
                error(simulation, null, "tables sync folder path EXISTING !!!");
            }

            // Starview
            if (Files.isDirectory(getStarViewSyncFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getStarViewSyncFolderPath(simulation), fileAttributes);
                FileUtils.runChmod(getStarViewSyncFolderPath(simulation).toString());
                FileUtils.logFilePermissions(getStarViewSyncFolderPath(simulation));
                LOGGER.info("getRunStarViewFolderPath created " + getStarViewSyncFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: getRunStarViewFolderPath EXISTING !!! with Path: "
                        + getStarViewSyncFolderPath(simulation));
                error(simulation, null, "Star View Folder Path EXISTING!!!");
            }

            // PowerPoint
            if (Files.isDirectory(getPowerPointSyncFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getPowerPointSyncFolderPath(simulation), fileAttributes);
                FileUtils.runChmod(getPowerPointSyncFolderPath(simulation).toString());
                FileUtils.logFilePermissions(getPowerPointSyncFolderPath(simulation));
                LOGGER
                        .info("getRunPowerPointFolderPath created " + getPowerPointSyncFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: getRunPowerPointFolderPath EXISTING !!! with Path: "
                        + getPowerPointSyncFolderPath(simulation));
                error(simulation, null, "Power Point Folder Path EXISTING!!!");
            }
        } catch (Exception e) {
            LOGGER.error("Error while createPostprocessingRunFolders", e);
            error(simulation, null, "Error while createPostprocessingRunFolders!!!");
        }
    }

    private Path getPowerPointSyncFolderPath(Simulation simulation) {
        return Paths.get(getJobSynchronisedFolderPath(simulation).toString(), "PowerPoint");
    }

    private Path getStarViewSyncFolderPath(Simulation simulation) {
        return Paths.get(getJobSynchronisedFolderPath(simulation).toString(), "StarView");
    }

    private Path getTablesSyncFolderPath(Simulation simulation) {
        return Paths.get(getJobSynchronisedFolderPath(simulation).toString(), "Tables");
    }

    private Path getPlotsSyncFolderPath(Simulation simulation) {
        return Paths.get(getJobSynchronisedFolderPath(simulation).toString(), "Plots");
    }

    private Path getScenesSyncFolderPath(Simulation simulation) {
        return Paths.get(getJobSynchronisedFolderPath(simulation).toString(), "Scenes");
    }

    private void selectStarCCMPlusRunVersion(Simulation simulation) {
        if (starCcmPlusVersionPath == null) {
            LOGGER.info("simulationCcmPlusVersion is NOT installed on compute node");
            LOGGER.info("using DEFAULT VERSION");
            if (StarCcmPlusUtil.getDefaultMixedPrecisionVersion() != null) {
                starCcmPlusVersionPath = StarCcmPlusUtil.getDefaultMixedPrecisionVersion().getPath();
                LOGGER.info("starCcmPlusDefaultVersionPath= " + starCcmPlusVersionPath);
            } else {
                LOGGER.info("unable to set defult version on compute node");
            }
        } else {
            LOGGER.info("simulationCcmPlusVersion is installed on compute node");
        }
    }

    private void getCustomerStarCCMPlusVersion(Simulation simulation) {
        if (StarCcmPlusUtil.getDefaultMixedPrecisionVersion() != null) {
            String command = StarCcmPlusUtil.getDefaultMixedPrecisionVersion().getPath() + " -info "
                    + getCustomerSimulationFilePathGadi(simulation);
            LOGGER.info("info command: " + command);
            List<String> result = sshService.execCommand(command);
            LOGGER.info("info command sumitted");
            try {
                for (String string : result) {
                    LOGGER.info(string);
                    Set<CcmPlus> list = StarCcmPlusUtil.getInstalledCcmPluses();
                    for (CcmPlus ccmPlus : list) {
                        if (ccmPlus.getPrecision().equals(StarCcmPrecision.MIXED)
                                && string.contains(ccmPlus.getVersion())) {
                            starCcmPlusVersionPath = ccmPlus.getPath();
                            starCcmPlusVersion = ccmPlus.getVersion();
                            LOGGER.info("starCcmPlusVersionPath selected: " + starCcmPlusVersionPath);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while trying to read info command response", e);
            }
            LOGGER.info("submitting info command fnished");
        }
    }

    private void copyCustomerSyncFolderIntoJobRunFolder(Simulation simulation) {
        try {
            LOGGER.info("starting CopyCustomerSyncFolderIntoJobRunFolder()");
            File sourceDirectory
                    = getCustomerSynchronisedFolderSimulationFolderFullPath(simulation).toFile();
            File destinationDirectory = getJobRunningFolderPath(simulation).toFile();
            ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

            LOGGER.info("finished CopyCustomerSyncFolderIntoJobRunFolder()");
        } catch (Exception e) {
            LOGGER.error("Error while copyCustomerSyncFolderIntoJobRunFolder", e);
            error(simulation, null, "Error while copyCustomerSyncFolderIntoJobRunFolder");
        }
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
    public void ConditionalMoveFiles(File source, File destination, String string)
            throws IOException {
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)
                        && child.getName().toLowerCase().contains(string.toLowerCase())) {
                    LOGGER.info("directoryListing child.getName = " + child.getName());
                    try {
                        Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                        // FileUtils.runChmod(destination.toString());
                        // FileUtils.logFilePermissions(destination.toPath());
                    } catch (Exception e) {
                        LOGGER.error("ConditionalMoveFiles failed to move the child to = "
                                + destination.toPath().resolve(child.getName()), e);
                    }
                    LOGGER.info(" directoryListing child moved to  = "
                            + destination.toPath().resolve(child.getName()));
                }
            }
        }
    }

    public void ConditionalMoveFiles(File source, File destination) throws IOException {
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                    LOGGER.info("directoryListing child.getName = " + child.getName());
                    try {
                        Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                        // FileUtils.runChmod(destination.toString());
                        // FileUtils.logFilePermissions(destination.toPath());
                    } catch (Exception e) {
                        LOGGER.error("ConditionalMoveFiles failed to move the child to = "
                                + destination.toPath().resolve(child.getName()), e);
                    }
                    LOGGER.info(" directoryListing child moved to  = "
                            + destination.toPath().resolve(child.getName()));
                }
            }
        }
    }

    public void ConditionalCopyFiles(File source, File destination) throws IOException {
        File[] directoryListing = source.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                    LOGGER.info("directoryListing child.getName = " + child.getName());
                    try {
                        Files.copy(child.toPath(), destination.toPath().resolve(child.getName()));
                        // FileUtils.runChmod(destination.toString());
                        // FileUtils.logFilePermissions(destination.toPath());
                    } catch (Exception e) {
                        LOGGER.error("ConditionalCopyFiles failed to move the child to = "
                                + destination.toPath().resolve(child.getName()), e);
                    }
                    LOGGER.info(" directoryListing child moved to  = "
                            + destination.toPath().resolve(child.getName()));
                }
            }
        }
    }

    private void createLogHeader(Simulation simulation) {
        LOGGER.info(
                "HEADER START----------------------------------------------------------------------------------------------------------------");
        LOGGER.info("simulationNumber = " + simulation.getId());
        LOGGER.info("customerNumber = " + simulation.getCustomerId());
        LOGGER.info("submissionDate = " + simulation.getDateCreated());
        LOGGER.info("maxMinutes = " + simulation.getMaxWalltime());
        LOGGER.info("Simulation = " + simulation.getFolderName());
        LOGGER.info("simulationRunningFolder = " + getJobRunningFolderPath(simulation));
        LOGGER.info(
                "HEADER END-------------------------------------------------------------------------------------------------------------------");
    }

    private void createLogAndBackupDirectories(Simulation simulation) {
        try {
            Files.createDirectories(getJobLogsPath(simulation), fileAttributes);
            boolean JobLogs = Files.isDirectory(getJobLogsPath(simulation), LinkOption.NOFOLLOW_LINKS);
            LOGGER.info("JobLogs isDirectory " + JobLogs);
            LOGGER.info(" getJobLogsPath Folder created " + getJobLogsPath(simulation));
            FileUtils.logFilePermissions(getJobLogsPath(simulation));

            Files.createDirectories(getJobBackupPath(simulation), fileAttributes);
            LOGGER.info(" getJobBackupPath Folder created " + getJobBackupPath(simulation));
            FileUtils.logFilePermissions(getJobBackupPath(simulation));

            Files.createDirectories(getJobSynchronisedFolderBackupPath(simulation), fileAttributes);
            LOGGER.info(" getJobSynchronisedFolderBackupPath Folder created "
                    + getJobSynchronisedFolderBackupPath(simulation));
            FileUtils.logFilePermissions(getJobSynchronisedFolderBackupPath(simulation));

            File log = getJobLogsPath(simulation).resolve("job_" + simulation.getId() + ".log").toFile();
            LOGGER.info("HERE writing job_" + simulation.getId() + ".log at path= " + log.toString());
            try {
                Files.createFile(log.toPath(), fileAttributes); // TODO check if already exist
                LOGGER.info("job_" + simulation.getId() + ".log File is created!");
            } catch (IOException ex) {
                LOGGER.info("job_" + simulation.getId()
                        + ".log : File already exists or the operation failed for some reason", ex);
            }
        } catch (Exception e) {
            LOGGER.error("Error while createLogAndBackupDirectories", e);
            error(simulation, null, "Error while createLogAndBackupDirectories");
        }
    }

    private Path getJobBackupPath(Simulation simulation) {
        return Paths.get(dataRoot, getCustomerFolderRelativePath(simulation),
                "Job_" + simulation.getId(), "backup");
    }

    private Path getJobLogsPath(Simulation simulation) {
        return Paths.get(dataRoot, getCustomerFolderRelativePath(simulation),
                "Job_" + simulation.getId(), "logs");
    }

    private Path getCustomerDataRoot(Simulation simulation) {
        return Paths.get(dataRoot, getCustomerFolderRelativePath(simulation),
                "Job_" + simulation.getId());
    }

    private Path getJobLogsPathGadi(Simulation simulation) {
        return Paths.get(dataRootGadi, getCustomerFolderRelativePath(simulation),
                "Job_" + simulation.getId(), "logs");
    }

    private void createJobRunAndSyncFolders(Simulation simulation) {
        try {
            if (Files.isDirectory(getJobRunningFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getJobRunningFolderPath(simulation), fileAttributes);
                // Files.createDirectories(getJobRunningFolderPath(simulation));
                LOGGER.info("JobRunningFolder created " + getJobRunningFolderPath(simulation));
                FileUtils.logFilePermissions(getJobRunningFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: JOBRUNNINGFOLDER EXISTING !!! with Path: "
                        + getJobRunningFolderPath(simulation));
                error(simulation, null, "Job Run Folder Preexisting");
            }

            if (Files.isDirectory(getJobSynchronisedFolderPath(simulation),
                    LinkOption.NOFOLLOW_LINKS) == false) {
                Files.createDirectories(getJobSynchronisedFolderPath(simulation), fileAttributes);
                LOGGER.debug("getJobSynchronisedFolderPath Folder created "
                        + getJobSynchronisedFolderPath(simulation));
                FileUtils.runChmod(getJobSynchronisedFolderPath(simulation).toString());
                FileUtils.logFilePermissions(getJobSynchronisedFolderPath(simulation));
            } else {
                LOGGER.error("ERROR: getJobSynchronisedFolderPath EXISTING !!! with Path: "
                        + getJobSynchronisedFolderPath(simulation));
                // this needs to make the simulation exist the queue as it indicates a major problem
                error(simulation, null, "Job Sync Folder Preexisting");
            }
        } catch (Exception e) {
            LOGGER.error("Error while createJobRunAndSyncFolders", e);
            error(simulation, null, "Error while createJobRunAndSyncFolders");
        }
    }

    private Path getJobSynchronisedFolderPath(Simulation simulation) {// the Job synchronised folder
        // where trampo send the results
        // back live.
        return Paths.get(dataRoot, getCustomerFolderRelativePath(simulation), "Synchronised_Folder",
                "Job_" + simulation.getId());
    }

    private Path getJobSynchronisedFolderBackupPath(Simulation simulation) {// the Job synchronised
        // folder
        // where trampo send the results
        // back live.
        return Paths.get(dataRoot, getCustomerFolderRelativePath(simulation), "Synchronised_Folder",
                "Job_" + simulation.getId() + "/backup");
    }

    private Path getJobRunningFolderPath(Simulation simulation) {// the Job running folder
        return Paths.get(runRoot, getCustomerFolderRelativePath(simulation),
                "Job_" + simulation.getId());
    }

    private Path getJobRunningFolderPathGadi(Simulation simulation) {// the Job running folder from
        // Raijin
        return Paths.get(runRootGadi, getCustomerFolderRelativePath(simulation),
                "Job_" + simulation.getId());
    }

    private String getCustomerFolderRelativePath(Simulation simulation) {
        return "customer_" + simulation.getCustomerId();
    }

    private Path getCustomerSynchronisedFolderSimulationFolderFullPath(Simulation simulation) {
        return getCustomerSynchronisedFolderPath(simulation.getCustomerId())
                .resolve(simulation.getFolderName());
    }

    private Path getRunSimulationFolderFullPath(Simulation simulation) {
        return getCustomerSynchronisedFolderPath(simulation.getCustomerId())
                .resolve(simulation.getFolderName());
    }

    private boolean areFilesAvailable(Simulation simulation) {
        if (!Files.isDirectory(getCustomerSynchronisedFolderSimulationFolderFullPath(simulation))) {
            LOGGER.info("Folder is not available. Path: "
                    + getCustomerSynchronisedFolderSimulationFolderFullPath(simulation));
            return false;
        }
        int count = 0;
        int simCount = 0;
        try {
            Iterator<Path> fileIt
                    = Files.list(getCustomerSynchronisedFolderSimulationFolderFullPath(simulation)).iterator();
            while (fileIt.hasNext()) {
                Path file = fileIt.next();
                LOGGER.info("Going through file " + file);
                if (Files.isRegularFile(file)) {
                    count++;
                }
                if (file.toString().endsWith(".sim")) {
                    simCount++;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to count files in "
                    + getCustomerSynchronisedFolderSimulationFolderFullPath(simulation), e);
            count = 0;
        }
        return (count >= simulation.getFileCount() && simCount == 1);
    }

    private String getCustomerFolderRelativePath(long customerId) {
        return "customer_" + customerId;
    }

    private Path getCustomerSynchronisedFolderPath(long customerId) {
        return Paths.get(dataRoot, getCustomerFolderRelativePath(customerId), "Synchronised_Folder");
    }

    public boolean isFinishedWithError(Simulation simulation) {
        String errorLogFilePath = getJobLogsPath(simulation).toString() + "/error.txt";
        String command = "chmod -R 770 " + getJobLogsPathGadi(simulation);
        sshService.execCommand(command);
        // sshService.copyRemoteFile(getJobLogsPathGadi(simulation)+ "/out.err", errorLogFilePath);
        try {
            List<String> lines = Files.readAllLines(Paths.get(errorLogFilePath));
            if (lines != null && lines.size() > 0 && StringUtils.hasText(lines.get(0))) {
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("error while reading error log: " + e);
        }
        return false;
    }
}
