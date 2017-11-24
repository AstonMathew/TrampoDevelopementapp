package com.trampo.process.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trampo.process.TrampoConfig;
import com.trampo.process.domain.Simulation;
import com.trampo.process.domain.SimulationStatus;
import com.trampo.process.exception.RestException;
import com.trampo.process.util.FileFunctions;
import com.trampo.process.util.MoveTask;
import com.trampo.process.util.MyResponseErrorHandler;
import com.trampo.process.util.Scan;
import com.trampo.process.util.SendEmail;
import com.trampo.process.util.ValidExtensions;

@Component
public class SimulationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationService.class);
  
  private JobService jobService;

  String starCcmPlusVersion = null;
  String starCcmPlusVersionPath = null;
  MoveTask moveTaskScenes;
  MoveTask moveTaskPlots;
  MoveTask moveTaskMesh;
  MoveTask moveTaskBackUp;

  private RestTemplate restTemplate;
  private ObjectMapper mapper;
  private String scheduleMovePeriod;
  private String dataRoot;
  private String runRoot;
  private String ccmplusversionforinfoflagrunpath;
  private TrampoConfig config;
  private Integer maxWaitForFilesInDays;
  private String defaultStartCcmPlusPath;
  private String backendScriptPath;
  private String podKey;
  private String macroPath;

  @Autowired
  public SimulationService(RestTemplateBuilder builder, TrampoConfig config, JobService jobService,
      @Value("${webapp.api.root}") String apiRoot,
      @Value("${trampo.simulation.scheduleMovePeriod}") String scheduleMovePeriod,
      @Value("${trampo.simulation.dataRoot}") String dataRoot,
      @Value("${trampo.simulation.runRoot}") String runRoot,
      @Value("${trampo.simulation.ccmplusversionforinfoflagrunpath}") String ccmplusversionforinfoflagrunpath,
      @Value("${trampo.simulation.maxWaitForFilesInDays}") Integer maxWaitForFilesInDays,
      @Value("${trampo.simulation.defaultStartCcmPlusPath}") String defaultStartCcmPlusPath,
      @Value("${trampo.simulation.backendScriptPath}") String backendScriptPath,
      @Value("${trampo.simulation.podKey}") String podKey,
      @Value("${trampo.simulation.macroPath}") String macroPath) {
    restTemplate = builder.rootUri(apiRoot).build();
    MyResponseErrorHandler errorHandler = new MyResponseErrorHandler();
    restTemplate.setErrorHandler(errorHandler);
    mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.scheduleMovePeriod = scheduleMovePeriod;
    this.dataRoot = dataRoot;
    this.runRoot = runRoot;
    this.ccmplusversionforinfoflagrunpath = ccmplusversionforinfoflagrunpath;
    this.config = config;
    this.maxWaitForFilesInDays = maxWaitForFilesInDays;
    this.defaultStartCcmPlusPath = defaultStartCcmPlusPath;
    this.jobService = jobService;
    this.backendScriptPath = backendScriptPath;
    this.podKey = podKey;
    this.macroPath = macroPath;
  }

  public Simulation getSimulation(String simulationId) throws RestException, IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response =
        restTemplate.exchange("/" + simulationId, HttpMethod.GET, postEntity, String.class);
    checkError(response);
    return mapper.readValue(response.getBody(), Simulation.class);
  }

  public List<Simulation> getByStatus(SimulationStatus status) throws IOException, RestException {
    LOGGER.info("getting simulatios in status: " + status);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response = restTemplate.exchange("/getByStatus/?status=" + status,
        HttpMethod.GET, postEntity, String.class);
    checkError(response);
    List<Simulation> list =
        mapper.readValue(response.getBody(), new TypeReference<List<Simulation>>() {});
    LOGGER.info("found " + list.size() + " simulatios in status: " + status);
    return list;
  }

  public void updateStatus(String simulationId, SimulationStatus status)
      throws RestException, JsonProcessingException {
    LOGGER.info("Updating simulation: " + simulationId + " to status: " + status);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response =
        restTemplate.exchange("/updateStatus/?status=" + status + "&id=" + simulationId,
            HttpMethod.GET, postEntity, String.class);
    checkError(response);
    LOGGER
        .info("Updating simulation: " + simulationId + " to status: " + status + "is successfull");
  }

  public void updateWalltime(String simulationId, long walltime)
      throws RestException, JsonProcessingException {
    LOGGER.info("Updating simulation: " + simulationId + " walltime: " + walltime);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response =
        restTemplate.exchange("/updateWalltime/?actualWalltime=" + walltime + "&id=" + simulationId,
            HttpMethod.GET, postEntity, String.class);
    checkError(response);
    LOGGER.info("Updated simulation: " + simulationId + " walltime: " + walltime);
  }

  public void error(String simulationId, String errorMessage)
      throws RestException, JsonProcessingException {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> request = new HashMap<>();
    HttpEntity<String> postEntity =
        new HttpEntity<String>(mapper.writeValueAsString(request), headers);
    ResponseEntity<String> response =
        restTemplate.exchange("/error/?errorMessage=" + errorMessage + "&id=" + simulationId,
            HttpMethod.GET, postEntity, String.class);
    checkError(response);
  }

  private void checkError(ResponseEntity<String> response) throws RestException {
    if (!response.getStatusCode().is2xxSuccessful()) {
      LOGGER.error("Rest Exception: Status Code: " + response.getStatusCode() + " body: "
          + response.getBody());
      throw new RestException();
    }
  }

  public void startSimulation(Simulation simulation) throws Exception {
    if (areFilesAvailable(simulation)) {

      checkSimName_FileCount_FileExtension_Scan4Macro(simulation);

      createJobRunAndSyncFolders(simulation);
      if (simulation.getStatus().equals(SimulationStatus.ERROR)) {
        return;
      }
      createLogAndBackupDirectories(simulation);
      createLogHeader(simulation);
      copyCustomerSyncFolderIntoJobRunFolder(simulation);
      // getCustomerStarCCMPlusVersion(simulation); //TODO
      selectStarCCMPlusRunVersion(simulation);
      createPostprocessingRunFolders(simulation);
      if (simulation.getStatus().equals(SimulationStatus.ERROR)) {
        return;
      }
      RunJob(simulation);
    } else {
      if (simulation.getStatus().equals(SimulationStatus.NEW)) {
        updateStatus(simulation.getId(), SimulationStatus.WAITING_FOR_FILES);
      } else {
        LOGGER.info("simulation date created: " + simulation.getDateCreated());
        if (simulation.getDateCreated().plusDays(maxWaitForFilesInDays)
            .compareTo(LocalDateTime.now()) < 0) {
          LOGGER.info("simulation date created plus days: " + simulation.getDateCreated().plusDays(maxWaitForFilesInDays));
          LOGGER.info("now: " + LocalDateTime.now());
          LOGGER.error("files took too long to upload. simulation id: " + simulation.getId());
          error(simulation.getId(), "files took too long to upload");
        }
      }
    }
  }

  private void finishSimulation(Simulation simulation) {
    File pbWorkingDirectory = getJobRunningFolderPath(simulation).toFile(); // (new File)?
    // pb.directory(pbWorkingDirectory);
    try {
      // updateJobStatus(JobStatuses.RUNNING);
      // _startSimulationTime = LocalTime.now();
      // _printStreamToLogFile.println("Starting simulation time: " + _startSimulationTime);
      // _simulationProcess = pb.start();
      // LOG.info("simulation run process started");

      // Redirection of stream and loop extremely important;
      // http://baxincc.cc/questions/216451/windows-process-execed-from-java-not-terminating
      // if not redirected, Star-CCM+ processes hang and -batch*-report doesn't print
      // InputStream stdout = _simulationProcess.getInputStream();
      // while (stdout.read() >= 0) {
      // }

      // _simulationProcess.waitFor();
      // Stop the automatic move of Scenes and Plots from run folder to sync folder
      // moveTaskScenes.cancelPurgeTimer();
      // moveTaskPlots.cancelPurgeTimer();
      // moveTaskMesh.cancelPurgeTimer();

      // REMOVE POD key FROM HTML report
      File[] directoryListing = pbWorkingDirectory.listFiles();
      if (directoryListing != null) {
        for (File child : directoryListing) {
          if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS)
              && child.getName().toLowerCase().contains(".html")) {
            LOGGER.info("HTML report file name = " + child.getName());
            String content =
                new String(Files.readAllBytes(child.toPath()), Charset.forName("UTF-8"));
            int index = content.indexOf("-podkey");
            LOGGER.info("indexOf -podkey in HTML report = " + index);
            LOGGER.info("OLD content.substring(index, index + 25) in HTML report = "
                + content.substring(index, index + 10));
            content =
                content.replace(content.substring(index, index + 25), "-podkey XXXXXXXXXXXXXXXXX");
            LOGGER.info("NEW content.substring(index, index + 25) in HTML report = "
                + content.substring(index, index + 10));
            try (PrintWriter out =
                new PrintWriter(new FileOutputStream(child.toPath().toString(), false))) {
              out.println(content);
            }
          }
        }
      }
      // end of the run file move
      File sourceDirectory = pbWorkingDirectory;

      File destinationDirectory = getJobLogsPath(simulation).toFile();
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, "log");
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[1]);
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[2]);
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, ValidExtensions.EXTENSIONS[3]);

      destinationDirectory = getJobSynchronisedFolderPath(simulation).toFile();
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Trampo");

      sourceDirectory = getTablesRunFolderPath(simulation).toFile();
      destinationDirectory = getTablesSyncFolderPath(simulation).toFile();
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

      sourceDirectory = getStarViewRunFolderPath(simulation).toFile();
      destinationDirectory = getStarViewSyncFolderPath(simulation).toFile();
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

      sourceDirectory = getPowerPointRunFolderPath(simulation).toFile();
      destinationDirectory = getPowerPointSyncFolderPath(simulation).toFile();
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

      sourceDirectory = getJobSynchronisedFolderPath(simulation).toFile();
      destinationDirectory = getJobBackupPath(simulation).toFile();
      ConditionalMoveFiles(sourceDirectory, destinationDirectory, "Backup");

      // delete anything left in the run/customer directory. for symlink handling see
      // http://stackoverflow.com/questions/779519/delete-directories-recursively-in-java/27917071#27917071
      Files.walkFileTree(getJobRunningFolderPath(simulation), new SimpleFileVisitor<Path>() {
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

      Files.walkFileTree(Paths.get(runRoot, getCustomerFolderRelativePath(simulation)),
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              // Files.delete(Paths.get(RUNROOT, getCustomerFolderRelativePath()));
              return FileVisitResult.CONTINUE;
            }
          });

      // All below
      // _printStreamToLogFile.println("End simulation time: " + LocalTime.now()); // this doesn't
      // seem to be done at the end of the process.
      // _printStreamToLogFile.println("Simulation/Total processing time: " + (int)
      // timeInSeconds(_startSimulationTime) + "s/" + (int) timeInSeconds(_startTime) + "s");
      // updateJobActualRuntime();
      // updateJobStatus(JobStatuses.COMPLETED);
      // _printStreamToLogFile.println("Simulation complete...");

    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
    LOGGER.info("ending run job");
  }

  private void checkSimName_FileCount_FileExtension_Scan4Macro(Simulation simulation)
      throws JsonProcessingException, RestException {

    String fileName = simulation.getFileName().replaceAll("\\s+", "");
    if (fileName.isEmpty()) {
      error(simulation.getId(), "Simulation name was not input");
      LOGGER.error(
          "Simulation cancelled. id: " + simulation.getId() + " Simulation name was not input");
      return;
    }

    // Check for .sim file
    fileName = (fileName.toLowerCase().endsWith(".sim")) ? fileName : (fileName + ".sim");
    if (!FileFunctions.fileIsAvailable(
        getCustomerSynchronisedFolder(simulation.getCustomerId()).resolve(fileName))) {
      LOGGER.error("Simulation cancelled. id: " + simulation.getId() + " Simulation file "
          + fileName + " is NOT available");
      error(simulation.getId(), "Simulation file " + fileName + " is NOT available");
      return;
    }

    LOGGER.info("Simulation file " + fileName + " is available");

    // check file extensions.
    File dir = getCustomerSynchronisedFolder(simulation.getCustomerId()).toFile();
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
            error(simulation.getId(), "Unsafe uploaded files extension");
          } else {
            if (child.getName().toLowerCase().endsWith(ValidExtensions.EXTENSIONS[3])) { //
              Scan scan = new Scan(child);
              if (!scan.scan()) {
                LOGGER.error("!scan.scan()=" + !scan.scan());
                LOGGER.error("scan4maco returned unsafe operation" + " customer = "
                    + simulation.getCustomerId() + " job = " + simulation.getId() + " File = "
                    + child.getName());
                new SendEmail().send(SendEmail.TO, "scan4maco returned unsafe operation",
                    "customer = " + simulation.getCustomerId() + " job = " + simulation.getId());
                error(simulation.getId(), "Scan4Macro detected an unsafe operation");
                return;
              }
            }
          }
        }
      }
    }

    // Check files count
    LOGGER.info("FileFunctions.countFiles(getCustomerSynchronisedFolder()) = "
        + FileFunctions.countFiles(getCustomerSynchronisedFolder(simulation.getCustomerId())));
    LOGGER.info("_fileCount = " + simulation.getFileCount());

    if (FileFunctions.countFiles(
        getCustomerSynchronisedFolder(simulation.getCustomerId())) != simulation.getFileCount()) {
      LOGGER.error("!!! Actual file count does not match nominated file count !!!");
      error(simulation.getId(), "number of files in sync folder not matching");
      return;
    } else {
      LOGGER.info("Actual file count matches nominated file count !!!");
    }
  }

  private void RunJob(Simulation simulation) throws Exception {
    LOGGER.info("starting run job");
    moveTaskScenes = new MoveTask(getScenesRunFolderPath(simulation).toFile(),
        getScenesSyncFolderPath(simulation).toFile());
    LOGGER.info("moveTaskScenes = new MoveTask done");

    moveTaskScenes.scheduleFileMove("Scene", Integer.parseInt(scheduleMovePeriod)); // non-blocking
    LOGGER.info("moveTaskScenes running");

    // move plots
    moveTaskPlots = new MoveTask(getPlotsRunFolderPath(simulation).toFile(),
        getPlotsSyncFolderPath(simulation).toFile());

    moveTaskPlots.scheduleFileMove("Plot", Integer.parseInt(scheduleMovePeriod)); // non-blocking
    LOGGER.info("moveTaskPlots running");
    // move plots
    moveTaskMesh = new MoveTask(getJobRunningFolderPath(simulation).toFile(),
        getJobSynchronisedFolderPath(simulation).toFile());

    moveTaskMesh.scheduleFileMove("Meshed", Integer.parseInt(scheduleMovePeriod)); // non-blocking
    LOGGER.info("moveTaskMesh running");

    int cpuCount = 0;
    String queueType;
    int memory;
    if(simulation.getProcessorType().equals("INSTANT")){
      cpuCount = simulation.getNumberOfCoresInstantFast();
      queueType = "expressbw";
      memory = 125 * (cpuCount / 8);
    }else if(simulation.getProcessorType().equals("FAST")){
      cpuCount = simulation.getNumberOfCoresInstantFast();
      queueType = "normalbw";
      memory = 125 * (cpuCount / 8);
    }else{
      cpuCount = simulation.getNumberOfCoresStandardLowPriority();
      queueType = "normal";
      memory = 30 * (cpuCount / 16);
    }
    String walltime = "000:00:00";
    long hours = 0;
    long minutes = 0;
    if(simulation.getMaxWalltime() > 60){
      hours = simulation.getMaxWalltime() / 60;
      minutes = simulation.getMaxWalltime() - hours * 60;
    }else{
      minutes = simulation.getMaxWalltime();
    }
    walltime = String.format("%03d", hours) + ":" + String.format("%02d", minutes) + ":00" ;
    String fileName = simulation.getFileName().replaceAll("\\s+", "");
    jobService.submitJob(simulation.getId(), "" + cpuCount, memory + "", queueType, 
        backendScriptPath, walltime, getJobRunningFolderPath(simulation).toString(), macroPath, 
        getCustomerSynchronisedFolder(simulation.getCustomerId()).resolve(fileName).toString(), podKey);
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

  private void createPostprocessingRunFolders(Simulation simulation) throws IOException, Exception {
    // scenes
    if (Files.isDirectory(getScenesSyncFolderPath(simulation),
        LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getScenesSyncFolderPath(simulation));
      LOGGER.info("getRunScenesFolderPath created " + getScenesSyncFolderPath(simulation));
    } else {
      LOGGER.error("ERROR: getRunScenesFolderPath EXISTING !!! with Path: "
          + getScenesSyncFolderPath(simulation));
      error(simulation.getId(),
          "getRunScenesFolderPath EXISTING !!!" + getScenesSyncFolderPath(simulation));
    }
    // plots
    if (Files.isDirectory(getPlotsSyncFolderPath(simulation), LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getPlotsSyncFolderPath(simulation));
      LOGGER.info("getRunPlotsFolderPath created " + getPlotsSyncFolderPath(simulation));
    } else {
      LOGGER.error("ERROR: getRunPlotsFolderPath EXISTING !!! with Path: "
          + getPlotsSyncFolderPath(simulation));
      error(simulation.getId(),
          "getRunPlotsFolderPath EXISTING !!!" + getPlotsSyncFolderPath(simulation));
    }

    // tables
    if (Files.isDirectory(getTablesSyncFolderPath(simulation),
        LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getTablesSyncFolderPath(simulation));
      LOGGER.info("getRunTablesFolderPath created " + getTablesSyncFolderPath(simulation));
    } else {
      LOGGER.error("ERROR: getRunTablesFolderPath EXISTING !!! with Path: "
          + getTablesSyncFolderPath(simulation));
      error(simulation.getId(), "ERROR: getRunTablesFolderPath EXISTING !!! with Path: "
          + getTablesSyncFolderPath(simulation));
    }

    // Starview
    if (Files.isDirectory(getStarViewSyncFolderPath(simulation),
        LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getStarViewSyncFolderPath(simulation));
      LOGGER.info("getRunStarViewFolderPath created " + getStarViewSyncFolderPath(simulation));
    } else {
      LOGGER.error("ERROR: getRunStarViewFolderPath EXISTING !!! with Path: "
          + getStarViewSyncFolderPath(simulation));
      error(simulation.getId(), "ERROR: getRunStarViewFolderPath EXISTING !!! with Path: "
          + getStarViewSyncFolderPath(simulation));
    }

    // PowerPoint
    if (Files.isDirectory(getPowerPointSyncFolderPath(simulation),
        LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getPowerPointSyncFolderPath(simulation));
      LOGGER.info("getRunPowerPointFolderPath created " + getPowerPointSyncFolderPath(simulation));
    } else {
      LOGGER.error("ERROR: getRunPowerPointFolderPath EXISTING !!! with Path: "
          + getPowerPointSyncFolderPath(simulation));
      error(simulation.getId(), "ERROR: getRunPowerPointFolderPath EXISTING !!! with Path: "
          + getPowerPointSyncFolderPath(simulation));
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

  private void selectStarCCMPlusRunVersion(Simulation simulation)
      throws IOException, InterruptedException { // oldest install is the default version
    // for (int i = 0; i < config.getCcmpluses().size(); i++) {
    // if (config.getCcmpluses().get(i).getVersion().equals(starCcmPlusVersion)
    // && config.getCcmpluses().get(i).getPrecision().equals(simulation.getStarCcmPrecision())) {
    // starCcmPlusVersionPath = config.getCcmpluses().get(i).getPath();
    // LOGGER.info("simulationCcmPlusVersionPath= " + starCcmPlusVersionPath);
    // }
    // }
    if (starCcmPlusVersionPath == null) {
      LOGGER.info("simulationCcmPlusVersion is NOT installed on compute node");
      LOGGER.info("using DEFAULT VERSION");
      // starCcmPlusVersion = defaultStartCcmPlusPath;
      // LOGGER.info("starCcmPlusDefaultVersion= " + defaultStartCcmPlusPath);
      starCcmPlusVersionPath = defaultStartCcmPlusPath;
      LOGGER.info("starCcmPlusDefaultVersionPath= " + defaultStartCcmPlusPath);
    } else {
      LOGGER.info("simulationCcmPlusVersion is installed on compute node");
    }
  }

  private void getCustomerStarCCMPlusVersion(Simulation simulation)
      throws IOException, InterruptedException {
    Path InitialVersionLogPath = getJobRunningFolderPath(simulation).resolve("version.log");
    Files.createFile(InitialVersionLogPath);
    ProcessBuilder pb =
        new ProcessBuilder(ccmplusversionforinfoflagrunpath, "-info", simulation.getFileName());
    pb.redirectOutput(InitialVersionLogPath.toFile());
    File pbWorkingDirectory = getJobRunningFolderPath(simulation).toFile();
    pb.directory(pbWorkingDirectory);
    Process p = pb.start();
    p.waitFor();
    LOGGER.info("version.log created");
    String content = new String(Files.readAllBytes(InitialVersionLogPath));
    LOGGER.info("version.log=" + content);
    int index = content.lastIndexOf("STAR-CCM+");
    LOGGER.info("index=" + index);
    int startindex = index + 9;
    LOGGER.info("versionsectionstartindex=" + startindex);
    int stopindex = startindex + 9;
    LOGGER.info("stopindex=" + stopindex);
    String ccmplusversion = content.substring(startindex, stopindex);
    LOGGER.info("CCM+ version = " + ccmplusversion);
    starCcmPlusVersion = ccmplusversion.replace(" ", "");
    Path finalVersionLogPath = getJobLogsPath(simulation).resolve("version.log");
    LOGGER.info("finalVersionLogPath = " + finalVersionLogPath.toString());
    Files.createFile(finalVersionLogPath);
    Files.move(InitialVersionLogPath, finalVersionLogPath, StandardCopyOption.REPLACE_EXISTING);
  }

  // TODO clean run folder before moving if already exist something
  private void copyCustomerSyncFolderIntoJobRunFolder(Simulation simulation)
      throws IOException, InterruptedException {
    LOGGER.info("starting CopyCustomerSyncFolderIntoJobRunFolder()");
    File sourceDirectory = getCustomerSynchronisedFolder(simulation.getCustomerId()).toFile();
    File destinationDirectory = getJobRunningFolderPath(simulation).toFile();
    ConditionalMoveFiles(sourceDirectory, destinationDirectory, "");

    LOGGER.info("finished CopyCustomerSyncFolderIntoJobRunFolder()");
  }

  /**
   *
   * moves all files containing the string from the source directory to the destination directory
   * both directory need to exist!
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
          } catch (Exception e) {
            LOGGER.error("CopyCustomerSyncFolderIntoJobRunFolder failed to move the child to = "
                + destination.toPath().resolve(child.getName()), e);
          }
          LOGGER.info(" directoryListing child moved to  = "
              + destination.toPath().resolve(child.getName()));
        }
      }
    }
  }

  private void createLogHeader(Simulation simulation) throws IOException, InterruptedException {
    LOGGER.info(
        "HEADER START----------------------------------------------------------------------------------------------------------------");
    LOGGER.info("simulationNumber = " + simulation.getId());
    LOGGER.info("customerNumber = " + simulation.getCustomerId());
    LOGGER.info("submissionDate = " + simulation.getDateCreated());
    LOGGER.info("maxSeconds = " + simulation.getMaxWalltime());
    LOGGER.info("Simulation = " + simulation.getFileName());
    LOGGER.info("simulationRunningFolder = " + getJobRunningFolderPath(simulation));
    LOGGER.info(
        "HEADER END-------------------------------------------------------------------------------------------------------------------");
  }

  private void createLogAndBackupDirectories(Simulation simulation)
      throws FileNotFoundException, IOException {
    Files.createDirectories(getJobLogsPath(simulation));
    boolean JobLogs = Files.isDirectory(getJobLogsPath(simulation), LinkOption.NOFOLLOW_LINKS);
    LOGGER.info("JobLogs isDirectory " + JobLogs);
    LOGGER.info(" getJobLogsPath Folder created " + getJobLogsPath(simulation));

    Files.createDirectories(getJobBackupPath(simulation));
    LOGGER.info(" getJobBackupPath Folder created " + getJobBackupPath(simulation));

    File log = getJobLogsPath(simulation).resolve("job_" + simulation.getId() + ".log").toFile();
    LOGGER.info("HERE writing job_" + simulation.getId() + ".log at path= " + log.toString());
    try {
      Files.createFile(log.toPath()); // TODO check if already exist
      LOGGER.info("job_" + simulation.getId() + ".log File is created!");
    } catch (IOException ex) {
      LOGGER.info("job_" + simulation.getId()
          + ".log : File already exists or the operation failed for some reason", ex);
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

  private void createJobRunAndSyncFolders(Simulation simulation) throws IOException, Exception {
    if (Files.isDirectory(getJobRunningFolderPath(simulation),
        LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getJobRunningFolderPath(simulation));
      LOGGER.info("JobRunningFolder created " + getJobRunningFolderPath(simulation));
    } else {
      LOGGER.error(
          "ERROR: JOBRUNNINGFOLDER EXISTING !!! with Path: " + getJobRunningFolderPath(simulation));
      error(simulation.getId(), "Job Run Folder Preexisting");
    }

    if (Files.isDirectory(getJobSynchronisedFolderPath(simulation),
        LinkOption.NOFOLLOW_LINKS) == false) {
      Files.createDirectories(getJobSynchronisedFolderPath(simulation));
      LOGGER.debug("getJobSynchronisedFolderPath Folder created "
          + getJobSynchronisedFolderPath(simulation));
    } else {
      LOGGER.error("ERROR: getJobSynchronisedFolderPath EXISTING !!! with Path: "
          + getJobSynchronisedFolderPath(simulation));
      // this needs to make the simulation exist the queue as it indicates a major problem
      error(simulation.getId(), "Job Sync Folder Preexisting");
    }
  }

  private Path getJobSynchronisedFolderPath(Simulation simulation) {// the Job synchronised folder
                                                                    // where trampo send the results
                                                                    // back live.
    return Paths.get(dataRoot, getCustomerFolderRelativePath(simulation), "Synchronised_Folder",
        "Job_" + simulation.getId());
  }

  private Path getJobRunningFolderPath(Simulation simulation) {// the Job running folder
    return Paths.get(runRoot, getCustomerFolderRelativePath(simulation),
        "Job_" + simulation.getId());
  }

  private String getCustomerFolderRelativePath(Simulation simulation) {
    return "customer_" + simulation.getCustomerId();
  }

  private boolean areFilesAvailable(Simulation simulation) {
    String fileName = simulation.getFileName().replaceAll("\\s+", "");
    fileName = fileName.toLowerCase().endsWith(".sim") ? fileName : (fileName + ".sim");
    if (!FileFunctions.fileIsAvailable(
        getCustomerSynchronisedFolder(simulation.getCustomerId()).resolve(fileName))) {
      LOGGER.info("File is not avaiable. Path: "
          + getCustomerSynchronisedFolder(simulation.getCustomerId()).resolve(fileName));
      return false;
    }
    return (FileFunctions.countFiles(
        getCustomerSynchronisedFolder(simulation.getCustomerId())) >= simulation.getFileCount());
  }

  private String getCustomerFolderRelativePath(long customerId) {
    return "customer_" + customerId;
  }

  private Path getCustomerSynchronisedFolder(long customerId) {
    return Paths.get(dataRoot, getCustomerFolderRelativePath(customerId), "Synchronised_Folder");
  }
  
  public boolean isFinishedWithError(Simulation simulation){
    String errorLogFilePath = getJobRunningFolderPath(simulation).toString()  + simulation.getId() + "/out.err";
    try {
      List<String> lines = Files.readAllLines(Paths.get(errorLogFilePath));
      if(lines != null && lines.size() > 0 && StringUtils.hasText(lines.get(0))){
        return true;
      }
    } catch (IOException e) {
      LOGGER.error("error while reading error log: " + e);
    }
    return false;
  }
}
