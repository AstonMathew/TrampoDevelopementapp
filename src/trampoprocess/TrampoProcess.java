package trampoprocess;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import constants.JobStatuses;

public class TrampoProcess {
	
	private static final Logger LOGGER = Logger.getLogger(TrampoProcess.class.getName() );
	
	private static String STOP = "stop.txt";
	private static String STOP_NOW = "stop_now.txt";

	public static void main(String[] args) throws Exception {
		LOGGER.getParent().addHandler(new StreamHandler(System.out, new SimpleFormatter())); // Redirect logger to console
		
		JobQueue simulationQueue = new JobQueue();
		Path currentPath = Paths.get(System.getProperty("user.dir"));
		System.out.println("Start trampo process in directory " + currentPath);
		System.out.println("Create file stop.txt in directory to stop trampo process, currently run simulaiton will finish");
		System.out.println("Create file stop_now.txt in directory to stop trampo process now...");
		
		// Retrieve paused simulations from webapp
		{
		Iterator<Job> simulation = WebAppGate.make().getSimulations(JobStatuses.PAUSED_MAINTENANCE).iterator();
		while (simulation.hasNext()) {simulationQueue.addJob(simulation.next());}
		}
		
		int counter = 0;
		
		// Main loop
		while ((! Files.exists(Paths.get(currentPath.toString(), STOP))) && (! Files.exists(Paths.get(currentPath.toString(), STOP_NOW)))) {
			try {
				System.out.println("Start main loop");
				simulationQueue.trigger(); // Process simulations in the queue
				
				// Add simulations from webapp
				Iterator<Job> simulation = WebAppGate.make().getSimulations().iterator();
				while (simulation.hasNext()) {simulationQueue.addJob(simulation.next());}
				
				// Auto customer folder creation 
                                //Check for new customer every 10 minutes NEEDS TO CREATE CUSTOMER FOLDER WITH SHOPIFY CUSTOMER NUMBER, in the synchronised folder location;
//				if ((counter % 600) == 0) {
//					System.out.println("Check customer list");
//					LinkedList<String> customerIds = WebAppGate.make().getCustomerList();
//					Iterator<String> customerIdsIt = customerIds.iterator();
//					while (customerIdsIt.hasNext()) {
//						String customerId = customerIdsIt.next();
//						System.out.println("Checking customer " + customerId);
//						// Check for directory and create if it does not exists
//						Path path = Paths.get(Job.DATAROOT, customerId);
//						if (Files.notExists(path)) {path.toFile().mkdirs();}
//					}
//				}
//				
//				TimeUnit.SECONDS.sleep(1); // Wait 1 second
//				counter += 1;

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		} // while (...)
		
		// Treat prior to exit
		if (Files.exists(Paths.get(currentPath.toString(), STOP))) {
			// Wait for simulation to complete and then stop
			System.out.println("Stopping trampo process");
			simulationQueue.purgeQueuingJobs(JobStatuses.PAUSED_MAINTENANCE);
			System.out.println("Waiting for current simulation to stop. It could take hours...");
			while (simulationQueue.hasJobRunning()) {
				simulationQueue.trigger();
				TimeUnit.SECONDS.sleep(1);;
			}			
			Files.delete(Paths.get(currentPath.toString(), STOP));
			new SendEmail().send(SendEmail.TO, "TrampoProcess stopped", "TrampoProcess has now stopped...");

		} else if (Files.exists(Paths.get(currentPath.toString(), STOP_NOW))) {
			// Stop now...
			System.out.println("Stopping trampo process NOW");
			simulationQueue.purgeQueuingJobs(JobStatuses.PAUSED_MAINTENANCE);
			System.out.println("Stopping currently running simulation NOW...");
			simulationQueue.stopCurrentJobNow();
			Files.delete(Paths.get(currentPath.toString(), STOP_NOW));
			new SendEmail().send(SendEmail.TO, "TrampoProcess stopped", "TrampoProcess has been forcefully stopped...");
				
		} else {
			throw new Exception("Trampo process should either be stopped or stopped now. Not sure what is going on...");
		}
		
		System.out.println("Trampo process complete");
		
	}
}
