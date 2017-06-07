package trampoprocess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import constants.JobStatuses;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class TrampoProcess {

    static final Logger LOG = LoggerFactory.getLogger(TrampoProcess.class);

    private static String STOP = "stop.txt";
    private static String STOP_NOW = "stop_now.txt";
    private static int sleeptime =3000; //1000=1s

    public static void main(String[] args) throws Exception {
        

        JobQueue simulationQueue = new JobQueue();
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        LOG.info("Start trampo process in directory " + currentPath);
        LOG.info("Create file stop.txt in directory to stop trampo process, currently run simulaiton will finish");
        LOG.info("Create file stop_now.txt in directory to stop trampo process now...");

        // Retrieve paused simulations from webapp
        {
            Iterator<Job> simulation = WebAppGate.make().getSimulations(JobStatuses.PAUSED_MAINTENANCE).iterator();
            while (simulation.hasNext()) {
                simulationQueue.addJob(simulation.next());
            }
        }

        int counter = 0;

        // Main loop
        while ((!Files.exists(Paths.get(currentPath.toString(), STOP))) && (!Files.exists(Paths.get(currentPath.toString(), STOP_NOW)))) {
            try {
                Thread.sleep(sleeptime);
                LOG.debug("Start main loop");
                simulationQueue.trigger(); // Process simulations in the queue

                // Add simulations from webapp
                Iterator<Job> simulation = WebAppGate.make().getSimulations().iterator();
                while (simulation.hasNext()) {
                    simulationQueue.addJob(simulation.next());
                }

                // Auto customer folder creation 
                //Check for new customer every 10 minutes NEEDS TO CREATE CUSTOMER FOLDER WITH SHOPIFY CUSTOMER NUMBER, in the synchronised folder location;
//				if ((counter % 600) == 0) {
//					LOG.debug("Check customer list");
//					LinkedList<String> customerIds = WebAppGate.make().getCustomerList();
//					Iterator<String> customerIdsIt = customerIds.iterator();
//					while (customerIdsIt.hasNext()) {
//						String customerId = customerIdsIt.next();
//						LOG.debug("Checking customer " + customerId);
//						// Check for directory and create if it does not exists
//						Path path = Paths.get(Job.DATAROOT, customerId);
//						if (Files.notExists(path)) {path.toFile().mkdirs();}
//					}
//				}
//				
                TimeUnit.SECONDS.sleep(1); // Wait 1 second
//				counter += 1;

            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        } // while (...)

        // Treat prior to exit
        if (Files.exists(Paths.get(currentPath.toString(), STOP))) {
            // Wait for simulation to complete and then stop
            LOG.info("Stopping trampo process");
            simulationQueue.purgeQueuingJobs(JobStatuses.PAUSED_MAINTENANCE);
            LOG.info("Waiting for current simulation to stop. It could take hours...");
            while (simulationQueue.hasJobRunning()) {
                simulationQueue.trigger();
                TimeUnit.SECONDS.sleep(1);;
            }
            Files.delete(Paths.get(currentPath.toString(), STOP));
            new SendEmail().send(SendEmail.TO, "TrampoProcess stopped", "TrampoProcess has now stopped...");

        } else if (Files.exists(Paths.get(currentPath.toString(), STOP_NOW))) {
            // Stop now...
            LOG.info("Stopping trampo process NOW");
            simulationQueue.purgeQueuingJobs(JobStatuses.PAUSED_MAINTENANCE);
            LOG.info("Stopping currently running simulation NOW...");
            simulationQueue.stopCurrentJobNow();
            Files.delete(Paths.get(currentPath.toString(), STOP_NOW));
            new SendEmail().send(SendEmail.TO, "TrampoProcess stopped", "TrampoProcess has been forcefully stopped...");

        } else {
            throw new Exception("Trampo process should either be stopped or stopped now. Not sure what is going on...");
        }

        LOG.info("Trampo process complete");

    }
}
