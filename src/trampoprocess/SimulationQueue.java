package trampoprocess;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
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
	LocalTime _timeLastRunTimeUpdate = null;

	public SimulationQueue() {
		_simulations = new LinkedList<Simulation>();
		_currentSimulation = null;
		_currentSimulationThread = null;
		_timeLastRunTimeUpdate = null;
	}

	public boolean hasSimulationRunning() {
		return _currentSimulation != null;
	}
	
	public boolean hasSimulation() {
		return (_simulations.size() > 0);
	}
	
	public void purgeQueuingSimulations(String status) {
		Iterator<Simulation> simIt = _simulations.iterator();
		while (simIt.hasNext()) {
			Simulation sim = simIt.next();
			try {
				new WebAppGate().updateSimulationStatus(sim, status);
			} catch (Exception e) {
				System.out.println("Error when updating status for simulation " + sim._simulationNumber + " with error " + e.getMessage());
			}
		}
		_simulations.clear();
	}
	
	public void stopCurrentSimulationNow() {
		if (hasSimulationRunning()) {
		  _currentSimulation.getSimulation().abortNow();
		  _currentSimulationThread.interrupt();
		}
	}
	
	public void addSimulation(Simulation sim) throws Exception {
		try {
			System.out.println("Adding simulation from " + sim._customerNumber + " simulation id: " + sim._simulationNumber + " with file " + sim._simulation);
			sim.checkSim_name_AndFiles_count_extension();
			_simulations.add(sim);
			new WebAppGate().updateSimulationStatus(sim, SimulationStatuses.SIMULATION_QUEUED);
		} catch (Exception e) {
			System.out.println("Error when adding simulation " + sim._simulationNumber + " with error " + e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
	}

	public void trigger() throws Exception {
		if ((_currentSimulation != null) && (_currentSimulationThread.isAlive())) { 
			// Current simulation running
			Simulation cSim = _currentSimulation.getSimulation();
			long rt = cSim.currentRunTimeInSeconds();

			// Update runtime simulation every 60 seconds
			if (ChronoUnit.SECONDS.between(_timeLastRunTimeUpdate, LocalTime.now()) >= 60) {
				cSim.updateSimulationActualRuntime();
				_timeLastRunTimeUpdate = LocalTime.now();
				cSim.updateMaximumClocktimeInSecondsFromWebApp();
			}

			if (new WebAppGate().isSimulationCanceled(cSim)) { 
				// Check if the user has canceled the simulation. If it has canceled mark the simulation as canceled
				cSim.markAsCanceled();
			}
			
			if (rt < cSim.maximumClocktimeInSeconds()) {
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
			_timeLastRunTimeUpdate = null;

		} else if (!_simulations.isEmpty()) { // No simulation running and not
												// empty queue
			_currentSimulation = new SimulationHandler(_simulations.poll());
			if (!new WebAppGate().isSimulationCanceled(_currentSimulation.getSimulation())) {
				System.out.println("Start new simulation " + _currentSimulation._sim._simulationNumber);
				_currentSimulationThread = new Thread(_currentSimulation);
				_currentSimulationThread.start();
			        _timeLastRunTimeUpdate = LocalTime.now();
				_currentSimulationThread.join(1000); // Wait for 1 seconds to
														// allow for the
														// simulation to start
			} else {
				_currentSimulation = null; // If simulation is cancelled reset it.
			        _timeLastRunTimeUpdate = null;
			}
		}
	}
}
