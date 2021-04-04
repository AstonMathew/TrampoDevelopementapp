package com.trampo.process.job;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import com.trampo.process.service.MailService;
import com.trampo.process.service.SimulationService;

@Component
public class SimulationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationJob.class);

  private ExecutorService threadPool = Executors.newCachedThreadPool();

  @Autowired
  JobService jobService;

  @Autowired
  SimulationService simulationService;

  @Autowired
  MailService mailService;

  Set<String> cancelled = new HashSet<>();
  Map<String, LocalDateTime> heldMap = new HashMap<>();

  @Scheduled(initialDelay=60000, fixedDelay = 30000)
  public void runSimulations() {
    LOGGER.info("Run Simulations job starting");

    Map<String, Simulation> runningSimulations = new HashMap<>();

    try {
      List<Simulation> simulations = simulationService.getByStatus(SimulationStatus.RUNNING);
      LOGGER.info("Found " + simulations.size() + " simulations in running state");
      for (Simulation simulation : simulations) {
        runningSimulations.put(simulation.getId(), simulation);
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
        LOGGER.info("Walltime= "+job.getWalltime());
        
                    Simulation simulation1 = simulationService.getSimulation(job.getSimulationId());
                      
                         if (job.getWalltime().contains(":")) {
                        String[] times = job.getWalltime().split(":");
                        int walltime = (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]);
                        if (times.length > 2 && Integer.parseInt(times[2]) > 0) {
                        walltime = walltime + 1;
                                     }
                   if(walltime >= (simulation1.getMaxWalltime()-8)){
                   simulationService.endSave(simulation1, job);
                   LOGGER.info("Walltime= "+job.getWalltime() +"Checkpoint created ");
                       
                   }
                   if(walltime >= (simulation1.getMaxWalltime()-3)){
                   simulationService.endAbort(simulation1, job);
                   LOGGER.info("Walltime= "+job.getWalltime() +"Abort Command Success ");
                   }
                   if(walltime >= (simulation1.getMaxWalltime())){
                   simulationService.endCancel(simulation1, job);
                   LOGGER.info("Walltime= "+job.getWalltime() +"qdel Command Success ");
                       
                   }
                   
                      }
                
        try {
          Long.parseLong(job.getSimulationId());
        } catch (Exception e) {
          continue;
        }

        Runnable r = () -> {
          try {
            if (job.getStatus().equals(JobStatus.R)) {
              if (!runningSimulations.containsKey(job.getSimulationId())) {
                Simulation simulation = simulationService.getSimulation(job.getSimulationId());
               
                
                if (simulation.getStatus().equals(SimulationStatus.CANCELLED)) {
                  if (!cancelled.contains(simulation.getId())) {
                    simulationService.cancelSimulation(simulation, job);
                    cancelled.add(simulation.getId());
                  }
                } else {
                  simulationService.updateStatus(job.getSimulationId(), SimulationStatus.RUNNING);
                  mailService.sendSimulationStartedEmails(simulation, job);
                }
              }
            } else if (job.getStatus().equals(JobStatus.S)) {
              if (runningSimulations.containsKey(job.getSimulationId())) {
                simulationService.updateStatus(job.getSimulationId(), SimulationStatus.SUSPENDED);
              } else {
                Simulation simulation = simulationService.getSimulation(job.getSimulationId());
                if (!simulation.getStatus().equals(SimulationStatus.SUSPENDED)) {
                  simulationService.updateStatus(job.getSimulationId(), SimulationStatus.SUSPENDED);
                }
              }
            } else if (job.getStatus().equals(JobStatus.F)) {
              if (runningSimulations.containsKey(job.getSimulationId())) {
                Simulation simulation = runningSimulations.get(job.getSimulationId());
                if (simulationService.isFinishedWithError(simulation)) {
                  simulationService.error(simulation, job, "Failed During Execution");
                  if (job.getWalltime().contains(":")) {
                    String[] times = job.getWalltime().split(":");
                    int walltime = (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]);
                    if (times.length > 2 && Integer.parseInt(times[2]) > 0) {
                      walltime = walltime + 1;
                    }
                    simulationService.updateWalltime(job.getSimulationId(), walltime);
                  }
                } else {
                  simulationService.updateStatus(job.getSimulationId(), SimulationStatus.COMPLETED);
                  mailService.sendSimulationCompletedEmails(simulation, job);
                }
                if (job.getWalltime().contains(":")) {
                  String[] times = job.getWalltime().split(":");
                  int walltime = (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]);
                  if (times.length > 2 && Integer.parseInt(times[2]) > 0) {
                    walltime = walltime + 1;
                  }
                  simulationService.updateWalltime(job.getSimulationId(), walltime);
                }
                simulationService.finishSimulation(simulation);
              } else {
                Simulation simulation = simulationService.getSimulation(job.getSimulationId());
                if (!simulation.getStatus().equals(SimulationStatus.COMPLETED)
                    && !simulation.getStatus().equals(SimulationStatus.ERROR)
                    && !simulation.getStatus().equals(SimulationStatus.CANCELLED)
                    && !simulation.getStatus().equals(SimulationStatus.CANCELREFUNDED)) {
                  if (simulationService.isFinishedWithError(simulation)) {
                    simulationService.error(simulation, job, "Failed During Execution");
                    
                    if (job.getWalltime().contains(":")) {
                    String[] times = job.getWalltime().split(":");
                    int walltime = (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]);
                    if (times.length > 2 && Integer.parseInt(times[2]) > 0) {
                      walltime = walltime + 1;
                    }
                    simulationService.updateWalltime(job.getSimulationId(), walltime);
                    }
                  } else {
                    simulationService.updateStatus(job.getSimulationId(), SimulationStatus.COMPLETED);
                    mailService.sendSimulationCompletedEmails(simulation, job);
                  }
                } else if (simulation.getStatus().equals(SimulationStatus.CANCELLED)) {
                  if (job.getWalltime().contains(":")) {
                    String[] times = job.getWalltime().split(":");
                    int walltime = (Integer.parseInt(times[0]) * 60) + Integer.parseInt(times[1]);
                    if (times.length > 2 && Integer.parseInt(times[2]) > 0) {
                      walltime = walltime + 1;
                    }
                    simulationService.updateWalltime(job.getSimulationId(), walltime);
                    simulationService.updateStatus(job.getSimulationId(), SimulationStatus.CANCELREFUNDED);
                  }
                  simulationService.finishSimulation(simulation); 
                }
                simulationService.finishSimulation(simulation);
              }
            } else if (job.getStatus().equals(JobStatus.H)) {
              LOGGER.warn("Job is in held state. job id: " + job.getId());
//              LocalDateTime lastEmailSentDate = heldMap.getOrDefault(job.getId(), LocalDateTime.MIN);
//              if(lastEmailSentDate.plusHours(1).compareTo(LocalDateTime.now()) == 1){
                Simulation simulation = simulationService.getSimulation(job.getSimulationId());
                mailService.sendJobHeldEmails(simulation, job);
//                heldMap.put(job.getId(), LocalDateTime.now());
//              }              
            } else if (job.getStatus().equals(JobStatus.E)) {
              // there is nothing to do
            } else if (job.getStatus().equals(JobStatus.Q)) {
              Simulation simulation = simulationService.getSimulation(job.getSimulationId());
              if (simulation.getStatus().equals(SimulationStatus.CANCELLED)) {
                jobService.cancelJob(job.getId());
                int walltime=0;
                simulationService.updateWalltime(job.getSimulationId(), walltime);
                    simulationService.updateStatus(job.getSimulationId(), SimulationStatus.CANCELREFUNDED);
              } else {
                simulationService.updateStatus(job.getSimulationId(), SimulationStatus.QUEUED);
              }
            }
          } catch (Exception e) {
            LOGGER.error("error while trying to get job status", e);
          }
        };
        threadPool.execute(r);
      }
    } catch (Exception e) {
      LOGGER.error("error while trying to get job status", e);
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

    try {
      for (Simulation simulation : simulationsWaitingForFiles) {
        Runnable r = () -> {
          try {
            simulationService.startSimulation(simulation);
          } catch (Exception e) {
            LOGGER.error("Error while starting simulation", e);
          }
        };
        threadPool.execute(r);
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }

    List<Simulation> simulationsNew = null;
    try {
      simulationsNew = simulationService.getByStatus(SimulationStatus.NEW);
      LOGGER.info("Found " + simulationsNew.size() + " simulations in NEW state");
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }

    try {
      for (Simulation simulation : simulationsNew) {
        Runnable r = () -> {
          try {
            simulationService.startSimulation(simulation);
          } catch (Exception e) {
            LOGGER.error("Error while starting simulation", e);
          }
        };
        threadPool.execute(r);
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    LOGGER.info("Run Simulations job finished");
  }
}
