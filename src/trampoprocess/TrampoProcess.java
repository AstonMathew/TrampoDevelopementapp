package trampoprocess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import constants.SimulationStatuses;

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
		Iterator<Job> simulation = new WebAppGate().getSimulations(SimulationStatuses.PAUSED_MAINTENANCE).iterator();
		while (simulation.hasNext()) {simulationQueue.addSimulation(simulation.next());}
		}
		
		// Main loop
		while ((! Files.exists(Paths.get(currentPath.toString(), STOP))) && (! Files.exists(Paths.get(currentPath.toString(), STOP_NOW)))) {
			try {
				System.out.println("Start main loop");
				simulationQueue.trigger(); // Process simulations in the queue
				
				// Add simulations from webapp
				Iterator<Job> simulation = new WebAppGate().getSimulations().iterator();
				while (simulation.hasNext()) {simulationQueue.addSimulation(simulation.next());}
				
				TimeUnit.SECONDS.sleep(1); // Wait 1 second

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		} // while (...)
		
		// Treat prior to exit
		if (Files.exists(Paths.get(currentPath.toString(), STOP))) {
			// Wait for simulation to complete and then stop
			System.out.println("Stopping trampo process");
			simulationQueue.purgeQueuingSimulations(SimulationStatuses.PAUSED_MAINTENANCE);
			System.out.println("Waiting for current simulation to stop. It could take hours...");
			while (simulationQueue.hasSimulationRunning()) {
				simulationQueue.trigger();
				TimeUnit.SECONDS.sleep(1);;
			}			
			Files.delete(Paths.get(currentPath.toString(), STOP));
			new SendEmail().send(SendEmail.TO, "TrampoProcess stopped", "TrampoProcess has now stopped...");

		} else if (Files.exists(Paths.get(currentPath.toString(), STOP_NOW))) {
			// Stop now...
			System.out.println("Stopping trampo process NOW");
			simulationQueue.purgeQueuingSimulations(SimulationStatuses.PAUSED_MAINTENANCE);
			System.out.println("Stopping currently running simulation NOW...");
			simulationQueue.stopCurrentSimulationNow();
			Files.delete(Paths.get(currentPath.toString(), STOP_NOW));
			new SendEmail().send(SendEmail.TO, "TrampoProcess stopped", "TrampoProcess has been forcefully stopped...");
				
		} else {
			throw new Exception("Trampo process should either be stopped or stopped now. Not sure what is going on...");
		}
		
		System.out.println("Trampo process complete");
		
	}
}
