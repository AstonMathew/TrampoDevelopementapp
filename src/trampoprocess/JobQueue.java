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

public class JobQueue {

	public class SimulationHandler implements Runnable {
		Job _sim = null;

		public SimulationHandler(Job sim) {
			_sim = sim;
		}

		public void run() {
			System.out.println("Running in a thread");
			 try {
				_sim.runJobWorkflow();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Job getSimulation() {
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
 
 public void runJobWorkflow() { _status = STARTED; System.out.println(
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
	Queue<Job> _simulations = null;
	Queue<Job> _simulationsWaitingForFiles = null;
	LocalTime _timeLastRunTimeUpdate = null;

	public JobQueue() {
		_simulations = new LinkedList<Job>();
		_simulationsWaitingForFiles = new LinkedList<Job>();
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
		Iterator<Job> simIt = _simulations.iterator();
		while (simIt.hasNext()) {
			Job sim = simIt.next();
			try {
				new WebAppGate().updateSimulationStatus(sim, status);
			} catch (Exception e) {
				System.out.println("Error when updating status for simulation " + sim._jobNumber + " with error " + e.getMessage());
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
	
	public void addSimulation(Job sim) throws Exception {
		if (sim.areFilesAvailable()) {
			addSimulationToQueue(sim);
		} else {
			_simulationsWaitingForFiles.add(sim);
		}
	}
	
	private boolean addSimulationToQueue(Job sim) throws Exception {
 		try {
			System.out.println("Adding simulation from " + sim._customerNumber + " simulation id: " + sim._jobNumber + " with file " + sim._simulation);
			  sim.checkSim_name_AndFiles_count_extension();
			  String c = new WebAppGate().getSimulationStatus(sim); 
			  if ((c.equals(SimulationStatuses.SUBMITED)) || (c.equals(SimulationStatuses.PAUSED_MAINTENANCE))) { 
			    _simulations.add(sim);
			    new WebAppGate().updateSimulationStatus(sim, SimulationStatuses.SIMULATION_QUEUED);
			    return true;
			  }
		} catch (Exception e) {
			System.out.println("Error when adding simulation " + sim._jobNumber + " with error " + e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
		return false;
	}

	public void trigger() throws Exception {
		// Deal with simulations waiting for files
		try {
			Iterator<Job> simIt = _simulationsWaitingForFiles.iterator();
			while (simIt.hasNext()) {
				Job sim = simIt.next();
				if (sim.areFilesAvailable()) {
					if (addSimulationToQueue(sim)) {
						// Job has been added to queue. Remove from waiting for files
						_simulationsWaitingForFiles.remove(sim);						
					} else {
						// Job has not been added. Check current status. If status changed, then remove as 
						// Something is wrong with the setup.
						String c = new WebAppGate().getSimulationStatus(sim);
						if (! ((c == SimulationStatuses.SUBMITED) || (c == SimulationStatuses.PAUSED_MAINTENANCE))) {
							_simulationsWaitingForFiles.remove(sim);
						}
					}
				} else if (ChronoUnit.HOURS.between(sim.getCreationTime(), LocalTime.now()) >= 3*24) {
					// Files are not available after 3 days. Let stuff happen
					_simulationsWaitingForFiles.remove(sim);
					addSimulationToQueue(sim);					
				}
			}
	   } catch (Exception e) {
		  System.out.println("Error with error " + e.getMessage());
	   }
		
		// Handle current process
		if ((_currentSimulation != null) && (_currentSimulationThread.isAlive())) { 
			// Current simulation running
			Job cSim = _currentSimulation.getSimulation();
			long rt = cSim.currentRunTimeInSeconds();

			// Update runtime simulation every 60 seconds
			if (ChronoUnit.SECONDS.between(_timeLastRunTimeUpdate, LocalTime.now()) >= 60) {
				cSim.updateJobActualRuntime();
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
				System.out.println("Start new simulation " + _currentSimulation._sim._jobNumber);
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
