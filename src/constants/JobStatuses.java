package constants;

/**
 *
 * @author julien
 *
 */
public class JobStatuses {
    // Status index as per the webapp (hopefully) variable simulationStatuses,  Check String[] simulationStatuses in webapp/Simulation.java and webapp/SimulationAdmin.java

    public static String SUBMITED = "Submited";
    public static String FILESUPLOADED = "Files Uploaded";
    public static String CANCELLED_NOFILEUPLOADED = "CANCELLED: number of files in sync folder not matching";
    public static String CANCELLED_NULL_SIMULATION_NAME = "CANCELLED: Simulation name was not input";
    public static String JOB_QUEUED = "Job Queued";
    public static String SECURITY_SCANNED = "Security Scanned";
    public static String CANCELLED_UNSAFE_FILES_EXTENSION = "CANCELLED: Unsafe uploaded files extension";
    public static String CANCELLED_UNSAFE_JAVA_MACRO = "CANCELLED: Unsafe uploaded java macro";
    public static String RUNNING = "Running";
    public static String PAUSED_MAINTENANCE = "PAUSED: maintenance";
    public static String CANCELLED_JAVA_MACRO_UNSAFEOPERATION = "CANCELLED: Java Macro Unsafe operation";
    public static String STOPPED_MAXRUNTIME_EXCEEDED = "STOPPED: MaxRunTime reached prior completion";
    public static String COMPLETED = "COMPLETED";
    public static String RESULTS_DOWNLOADED = "Results Downloaded";
    public static String CANCELLED_BY_USER = "CANCELLED: by User";
    public static String CANCELLED_SIMULATION_FOLDER_PREEXISTING = "CANCELLED: Simulation Folder Preexisting";
    public static String CANCELLED_JOB_SYNC_FOLDER_PREEXISTING = "CANCELLED: Job Sync Folder Preexisting";
    public static String CANCELLED_JOB_RUN_FOLDER_PREEXISTING = "CANCELLED: Job Run Folder Preexisting";
    public static String CANCELLED_FILES_TOO_LONG_TO_UPLOAD = "CANCELLED: files took too long to upload";
    public static String CANCELLED_SCAN4MACRO_UNSAFE_OPERATION = "CANCELLED: Scan4Macro detected an unsafe operation";
}
