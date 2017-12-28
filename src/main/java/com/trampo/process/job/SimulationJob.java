package com.trampo.process.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.trampo.process.domain.Job;
import com.trampo.process.domain.JobStatus;
import com.trampo.process.domain.Simulation;
import com.trampo.process.domain.SimulationStatus;
import com.trampo.process.service.JobService;
import com.trampo.process.service.SimulationService;

@Component
public class SimulationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationJob.class);

  @Autowired
  JobService jobService;

  @Autowired
  SimulationService simulationService;

  @Scheduled(fixedDelay = 500)
  public void runSimulations() {
    LOGGER.info("Run Simulations job starting");

    Map<String, Simulation> map = new HashMap<>();

    try {
      List<Simulation> simulations = simulationService.getByStatus(SimulationStatus.RUNNING);
      LOGGER.info("Found " + simulations.size() + " simulations in running state");
      for (Simulation simulation : simulations) {
        map.put(simulation.getId(), simulation);
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }

    try {
      List<Job> currentJobs = jobService.getCurrentJobs();
      LOGGER.info("Found " + currentJobs.size() + " currentjobs");
      for (Job job : currentJobs) {
        LOGGER.info("found job; simulationId: " + job.getSimulationId() + " job status: "
            + job.getStatus());

        try {
          if (job.getStatus().equals(JobStatus.R)) {
            if (!map.containsKey(job.getSimulationId())) {
              Simulation simulation = simulationService.getSimulation(job.getSimulationId());
              if (simulation.getStatus().equals(SimulationStatus.CANCELLED)) {
                simulationService.cancelSimulation(simulation, job);
              } else {
                simulationService.updateStatus(job.getSimulationId(), SimulationStatus.RUNNING);
              }
            }
          } else if (job.getStatus().equals(JobStatus.S)) {
            if (map.containsKey(job.getSimulationId())) {
              simulationService.updateStatus(job.getSimulationId(), SimulationStatus.SUSPENDED);
            } else {
              Simulation simulation = simulationService.getSimulation(job.getSimulationId());
              if (!simulation.getStatus().equals(SimulationStatus.SUSPENDED)) {
                simulationService.updateStatus(job.getSimulationId(), SimulationStatus.SUSPENDED);
              }
            }
          } else if (job.getStatus().equals(JobStatus.F)) {
            if (map.containsKey(job.getSimulationId())) {
              Simulation simulation = map.get(job.getSimulationId());
              if(simulationService.isFinishedWithError(simulation)){
                simulationService.error(simulation.getId(), "Failed During Execution");
              }
              String[] times = job.getWalltime().split(":");
              int walltime = (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]); 
              if(Integer.parseInt(times[2]) > 0){
                walltime = walltime + 1;
              }
              simulationService.updateWalltime(job.getSimulationId(), walltime);
            } else {
              Simulation simulation = simulationService.getSimulation(job.getSimulationId());
              if (!simulation.getStatus().equals(SimulationStatus.COMPLETED) && !simulation.getStatus().equals(SimulationStatus.ERROR)) {
                if(simulationService.isFinishedWithError(simulation)){
                  simulationService.error(simulation.getId(), "Failed During Execution");
                }else{
                  simulationService.updateStatus(job.getSimulationId(), SimulationStatus.COMPLETED);
                }
              }
            }
          } else if (job.getStatus().equals(JobStatus.H)) {
            // TODO what?
          }else if (job.getStatus().equals(JobStatus.E)) {
            // TODO what?
          }
        } catch (Exception e) {
          LOGGER.error(e.getMessage());
        }
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    List<Simulation> simulationsWaitingForFiles = null;
    try {
      simulationsWaitingForFiles =
          simulationService.getByStatus(SimulationStatus.WAITING_FOR_FILES);
      LOGGER.info(
          "Found " + simulationsWaitingForFiles.size() + " simulations in WAITING_FOR_FILES state");
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    
    try{
      for (Simulation simulation : simulationsWaitingForFiles) {
        simulationService.startSimulation(simulation);
      }
    }catch (Exception e) {
      LOGGER.error(e.getMessage());
    }

    List<Simulation> simulationsNew = null;
    try {
      simulationsNew = simulationService.getByStatus(SimulationStatus.NEW);
      LOGGER.info("Found " + simulationsNew.size() + " simulations in NEW state");
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    
    try{
      for (Simulation simulation : simulationsNew) {
        simulationService.startSimulation(simulation);
      }
    }catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    LOGGER.info("Run Simulations job finished");
  }
}
