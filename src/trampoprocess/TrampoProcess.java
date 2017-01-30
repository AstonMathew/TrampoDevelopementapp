package trampoprocess;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class TrampoProcess {

	public static void main(String[] args) {
		SimulationQueue simulationQueue = new SimulationQueue();
		while (true) {
			try {
				System.out.println("Start main loop");
				simulationQueue.trigger(); // Process simulations in the queue
				
				// Add simulations from webapp
				Iterator<Simulation> simulation = new WebAppGate().getSimulations().iterator();
				while (simulation.hasNext()) {simulationQueue.addSimulation(simulation.next());}
				
				TimeUnit.SECONDS.sleep(1); // Wait 1 second

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		} // while (true)
	}
}
