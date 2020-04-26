package com.trampo.process.domain;

public class Job {

  String id;
  String queue;
  String simulationId;
  JobStatus status;
  String walltime;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public JobStatus getStatus() {
    return status;
  }

  public void setStatus(JobStatus status) {
    this.status = status;
  }

  public String getWalltime() {
    return walltime;
  }

  public void setWalltime(String walltime) {
    this.walltime = walltime;
  }

  public String getSimulationId() {
    return simulationId;
  }

  public void setSimulationId(String simulationId) {
    this.simulationId = simulationId;
  }
}
