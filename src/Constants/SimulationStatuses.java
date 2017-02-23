package constants;

/**
 * 
 * @author julien
 *
 */
public class SimulationStatuses {
	// Status index as per the webapp (hopefully) variable simulationStatuses
	public static String SUBMITED = "Submited";
	public static String FILESUPLOADED = "Files Uploaded";
	public static String CANCELLED_NOFILEUPLOADED = "CANCELLED: # of files uploaded != # files in sync folder";
        public static String CANCELLED_NULL_SIMULATION_NAME = "CANCELLED: Simulation name was not input";
	public static String SIMULATION_QUEUED = "Simulation Queued";
	public static String SECURITY_SCANNED = "Security Scanned";
	public static String CANCELLED_UNSAFE_FILES_EXTENSION = "CANCELLED: Unsafe uploaded files extension";
	public static String CANCELLED_UNSAFE_JAVA_MACRO = "CANCELLED: Unsafe uploaded java macro";
	public static String RUNNING = "Running";
	public static String PAUSED_MAINTENANCE = "PAUSED: maintenance";
	public static String CANCELLED_JAVA_MACRO_UNSAFEOPERATION = "CANCELLED: Java Macro Unsafe operation";
	public static String STOPPED_MAXRUNTIME_EXCEEDED = "STOPPED: MaxRunTime reached prior completion";
	public static String COMPLETED = "COMPLETED";
	public static String RESULTS_DOWNLOADED ="Results Downloaded";
	public static String CANCELLED_BY_USER = "CANCELLED: by User";
        //public static String CANCELLED_SIMULATION_FOLDER_PREEXISTING = "CANCELLED: Simulation Folder Preexisting";
    public static String CANCELLED_JOB_SYNC_FOLDER_PREEXISTING = "CANCELLED: Job Sync Folder Preexisting" ;
    public static String CANCELLED_JOB_RUN_FOLDER_PREEXISTING = "CANCELLED: Job Run Folder Preexisting";
}
