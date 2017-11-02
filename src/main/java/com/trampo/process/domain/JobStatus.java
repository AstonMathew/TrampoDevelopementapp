package com.trampo.process.domain;

public enum JobStatus {
  E, //Job is exiting after having run
  F, //Job is finished. Job has completed execution, job failed during execution, or job was deleted.
  H, //Job is held. A job is put into a held state by the server or by a user or administrator. A job stays in a held state until it is released by a user or administrator.
     //You can view the reason a job has been held with qstat -s jobid (Job IDs on Raijin look like 1234.r-man2)
  Q, //Job is queued, eligible to run.
  R, //Job is running
  S; //Job is suspended by server. A job is put into the suspended state when a higher priority job needs the resources.
}
