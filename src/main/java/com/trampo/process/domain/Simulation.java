package com.trampo.process.domain;

import java.time.LocalDateTime;

public class Simulation {

  private String id;
  private long customerId;
  private String folderName;
  private long maxWalltime;
  private long actualWalltime;
  private int fileCount;
  private Integer numberOfCoresStandardLowPriority;
  private Integer numberOfCoresInstantFast;
  private String errorMessage;
  private SimulationStatus status;
  private SimulationFileTransferMethod fileTransferMethod;
  private String processorType;
  private StarCcmPrecision starCcmPrecision;
  private ByoLicensingType byoLicensingType;
  private String podKey;
  private LocalDateTime dateCreated;
  private LocalDateTime dateUpdated;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(long customerId) {
    this.customerId = customerId;
  }

  public int getFileCount() {
    return fileCount;
  }

  public void setFileCount(int fileCount) {
    this.fileCount = fileCount;
  }

  public SimulationStatus getStatus() {
    return status;
  }

  public void setStatus(SimulationStatus status) {
    this.status = status;
  }

  public LocalDateTime getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(LocalDateTime dateCreated) {
    this.dateCreated = dateCreated;
  }

  public LocalDateTime getDateUpdated() {
    return dateUpdated;
  }

  public void setDateUpdated(LocalDateTime dateUpdated) {
    this.dateUpdated = dateUpdated;
  }

  public Integer getNumberOfCoresStandardLowPriority() {
    return numberOfCoresStandardLowPriority;
  }

  public void setNumberOfCoresStandardLowPriority(Integer numberOfCoresStandardLowPriority) {
    this.numberOfCoresStandardLowPriority = numberOfCoresStandardLowPriority;
  }

  public Integer getNumberOfCoresInstantFast() {
    return numberOfCoresInstantFast;
  }

  public void setNumberOfCoresInstantFast(Integer numberOfCoresInstantFast) {
    this.numberOfCoresInstantFast = numberOfCoresInstantFast;
  }

  public SimulationFileTransferMethod getFileTransferMethod() {
    return fileTransferMethod;
  }

  public void setFileTransferMethod(SimulationFileTransferMethod fileTransferMethod) {
    this.fileTransferMethod = fileTransferMethod;
  }

  public String getProcessorType() {
    return processorType;
  }

  public void setProcessorType(String processorType) {
    this.processorType = processorType;
  }

  public StarCcmPrecision getStarCcmPrecision() {
    return starCcmPrecision;
  }

  public void setStarCcmPrecision(StarCcmPrecision starCcmPrecision) {
    this.starCcmPrecision = starCcmPrecision;
  }

  public ByoLicensingType getByoLicensingType() {
    return byoLicensingType;
  }

  public void setByoLicensingType(ByoLicensingType byoLicensingType) {
    this.byoLicensingType = byoLicensingType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public long getMaxWalltime() {
    return maxWalltime;
  }

  public void setMaxWalltime(long maxWalltime) {
    this.maxWalltime = maxWalltime;
  }

  public long getActualWalltime() {
    return actualWalltime;
  }

  public void setActualWalltime(long actualWalltime) {
    this.actualWalltime = actualWalltime;
  }

  public String getFolderName() {
    return folderName;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }

  public String getPodKey() {
    return podKey;
  }

  public void setPodKey(String podKey) {
    this.podKey = podKey;
  }
}
