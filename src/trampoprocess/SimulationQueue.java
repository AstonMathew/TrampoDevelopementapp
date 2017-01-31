package trampoprocess;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Queue;

import constants.SimulationStatuses;

public class SimulationQueue {

	public class SimulationHandler implements Runnable {
		Simulation _sim = null;

		public SimulationHandler(Simulation sim) {
			_sim = sim;
		}

		public void run() {
			System.out.println("Running in a thread");
			 try {
				_sim.runSimulationWorkflow();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Simulation getSimulation() {
			return _sim;
		}
	}

	// This should probably be a recast of TrampoProcess. But the coding is a
	// mess of
	// static and not static functions and members
	/**
	 * public class SimulationRequest{ static final int DEFINED = 1; static
 final int STARTED = 2; static final int COMPLETE = 3;
 
 Integer _status = null;
 
 public SimulationRequest() { _status = DEFINED; }
 
 public int maximumClocktimeInSeconds() { return 60; }
 
 public void runSimulationWorkflow() { _status = STARTED; System.out.println(
 "Running simulation in a thread"); ProcessBuilder pb = new
 ProcessBuilder("c:\\Temp\\test.bat"); try { pb.start(); } catch
 (IOException e) { e.printStackTrace(); } finally { _status = COMPLETE; }
 }
 
 public void abortSimulation() { System.out.println("Aborting simulation"
 ); _status = COMPLETE; }
 
 public void abortNow() { System.out.println("Stop the simulation now..."
 ); _status = COMPLETE; }
 
 public Integer getStatus() { return _status; } }
	 */

	// Store the list of simulation requests
	Thread _currentSimulationThread = null;
	SimulationHandler _currentSimulation = null;
	Queue<Simulation> _simulations = null;

	public SimulationQueue() {
		_simulations = new LinkedList<Simulation>();
		_currentSimulation = null;
		_currentSimulationThread = null;
	}

	public void addSimulation(Simulation sim) throws Exception {
		System.out.println("Adding simulation from " + sim._customerNumber + " simulation id: " + sim._simulationNumber + " with file " + sim._simulation);
		_simulations.add(sim);
		new WebAppGate().updateSimulationStatus(sim, SimulationStatuses.SIMULATION_QUEUED);
	}

	public void trigger() throws Exception {
		if ((_currentSimulation != null) && (_currentSimulationThread.isAlive())) { // Current
																					// simulation
																					// running
			Simulation cSim = _currentSimulation.getSimulation();
			long rt = cSim.currentRunTimeInSeconds();
			if (new WebAppGate().isSimulationCanceled(cSim)) { // Check if the
																// user has
																// canceled the
																// simulation
				cSim.abortNow();
				_currentSimulationThread.interrupt();
			} else if (rt < cSim.maximumClocktimeInSeconds()) {
                            System.out.println("cSim.maximumClocktimeInSeconds()= "+cSim.maximumClocktimeInSeconds());
                            System.out.println("cSim._maxSeconds= "+cSim._maxSeconds);
				_currentSimulationThread.join(1000); // Wait another 1 seconds
														// before checking again
			} else if (rt < cSim.maximumClocktimeInSeconds() + 120) {
				// The simulation has expired his clocktime request allow 100
				// seconds for graceful stop
				cSim.abort();
				_currentSimulationThread.join(1000); // Wait for 1 second
			} else {
				// Past the 100 seconds grace period. Force the simulation to
				// end
				cSim.abortNow();
				_currentSimulationThread.interrupt();
			}
		} else if (_currentSimulation != null) { // Current simulation no longer
													// running, but not cleared
			_currentSimulation = null;

		} else if (!_simulations.isEmpty()) { // No simulation running and not
												// empty queue
			_currentSimulation = new SimulationHandler(_simulations.poll());
			if (!new WebAppGate().isSimulationCanceled(_currentSimulation.getSimulation())) {
				System.out.println("Start new simulation " + _currentSimulation._sim._simulationNumber);
				_currentSimulationThread = new Thread(_currentSimulation);
				_currentSimulationThread.start();
				_currentSimulationThread.join(1000); // Wait for 1 seconds to
														// allow for the
														// simulation to start
			} else {
				_currentSimulation = null; // If simulation is cancelled reset it.
			}
		}
	}
}
