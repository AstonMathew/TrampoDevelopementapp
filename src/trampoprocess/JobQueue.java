package trampoprocess;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import constants.JobStatuses;

public class JobQueue {
    static final Logger LOG = LoggerFactory.getLogger(JobQueue.class);

	public class JobHandler implements Runnable {
		Job _job = null;

		public JobHandler(Job job) {
			_job = job;
		}

		public void run() {
			LOG.debug("Running in a thread");
			 try {
				_job.runJobWorkflow();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Job getJob() {
			return _job;
		}
	}

	// Store the list of simulation requests
	Thread _currentJobThread = null;
	JobHandler _currentJob = null;
	Queue<Job> _jobs = null;
	Queue<Job> _jobsWaitingForFiles = null;
	Map<Integer, Job> _jobProcessed = null;
	LocalTime _timeLastRunTimeUpdate = null;

	public JobQueue() {
		_jobs = new LinkedList<Job>();
		_jobsWaitingForFiles = new LinkedList<Job>();
		_currentJob = null;
		_currentJobThread = null;
		_timeLastRunTimeUpdate = null;
		_jobProcessed = new HashMap<Integer, Job>();
	}

	public boolean hasJobRunning() {
		return _currentJob != null;
	}
	
	public boolean hasJob() {
		return (_jobs.size() > 0);
	}
	
	public void purgeQueuingJobs(String status) {
		Iterator<Job> jobIt = _jobs.iterator();
		while (jobIt.hasNext()) {
			Job job = jobIt.next();
			try {
				WebAppGate.make().updateJobStatus(job, status);
			} catch (Exception e) {
				LOG.error("Error when updating status for job " + job._jobNumber + " with error " + e.getMessage());
			}
		}
		_jobs.clear();
	}
	
	public void stopCurrentJobNow() {
		if (hasJobRunning()) {
		  _currentJob.getJob().abortNow();
		  _currentJobThread.interrupt();
		}
	}
	
	public void addJob(Job job) throws Exception {
		if (_jobProcessed.containsKey(job._jobNumber) == false) {
			LOG.info("Add job " + job._jobNumber + " to queue");
			_jobProcessed.put(job._jobNumber, job);
			if (job.areFilesAvailable()) {
				LOG.info("Files for job " + job._jobNumber + " are available, job will be processed");
				addJobToQueue(job);
			} else {
				_jobsWaitingForFiles.add(job);
			}
		}
	}
	
	private boolean addJobToQueue(Job job) throws Exception {
 		try {
			LOG.info("Adding job from " + job._customerNumber + " job id: " + job._jobNumber + " with file " + job._simulation);
			  job.checkSimName_FileCount_FileExtension_Scan4Macro();
			  String c = WebAppGate.make().getJobStatus(job); 
			  if ((c.equals(JobStatuses.SUBMITED)) || (c.equals(JobStatuses.PAUSED_MAINTENANCE))) { 
				LOG.debug("Job " + job._jobNumber + " will be treated soon ");
			    _jobs.add(job);
			    WebAppGate.make().updateJobStatus(job, JobStatuses.JOB_QUEUED);
			    return true;
			  } else {
			    LOG.warn("Job " + job._jobNumber + " will not be treated as status is " + c);				  
			    return false;
			  }
		} catch (Exception e) {
			LOG.error("Error when adding job " + job._jobNumber + " with error " + e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			LOG.error(sw.toString());
		}
		return false;
	}

	public void trigger() throws Exception {
		// Deal with jobs waiting for files
		try {
			Iterator<Job> simIt = _jobsWaitingForFiles.iterator();
			while (simIt.hasNext()) {
				Job job = simIt.next();
				if (job.areFilesAvailable()) {
					if (addJobToQueue(job)) {
						// Job has been added to queue. Remove from waiting for files
						_jobsWaitingForFiles.remove(job);						
					} else {
						// Job has not been added. Check current status. If status changed, then remove as 
						// Something is wrong with the setup.
						String c = WebAppGate.make().getJobStatus(job);
						if (! ((c == JobStatuses.SUBMITED) || (c == JobStatuses.PAUSED_MAINTENANCE))) {
							_jobsWaitingForFiles.remove(job);
						}
					}
				} else if (ChronoUnit.HOURS.between(job.getSubmissionDate().toInstant(), new GregorianCalendar().getTime().toInstant()) >= 4*24) {
					// Files are not available after 4 days.
					_jobsWaitingForFiles.remove(job);
                                        LOG.error(" job " + job._jobNumber + " canceled: files took too long to upload ");
                                        //update status: CANCELLED: still haven't uploaded in 3 days
                                        WebAppGate.make().updateJobStatus(job, JobStatuses.CANCELLED_FILES_TOO_LONG_TO_UPLOAD); //NOT TESTED
					//addJobToQueue(job);					
				}
			}
	   } catch (Exception e) {
		  LOG.error("Error with error " + e.getMessage());
	   }
		
		// Handle current process
		if ((_currentJob != null) && (_currentJobThread.isAlive())) { 
			// Current job running
			Job cJob = _currentJob.getJob();
			long rt = cJob.currentRunTimeInSeconds();

			// Update runtime job every 60 seconds
			if (ChronoUnit.SECONDS.between(_timeLastRunTimeUpdate, LocalTime.now()) >= 60) {
				cJob.updateJobActualRuntime();
				_timeLastRunTimeUpdate = LocalTime.now();
				cJob.updateMaximumClocktimeInSecondsFromWebApp();
			}

			if (WebAppGate.make().isJobCanceled(cJob)) { 
				// Check if the user has canceled the job. If it has canceled mark the job as canceled
				cJob.markAsCanceled();
			}
			
			if (rt < cJob.maximumClocktimeInSeconds()) {
                            LOG.debug("cJob.maximumClocktimeInSeconds()= "+cJob.maximumClocktimeInSeconds());
                            LOG.debug("cJob._maxSeconds= "+cJob._maxSeconds);
				_currentJobThread.join(60000); // 1000=1sec 60000=10min
													
			} else if (rt < cJob.maximumClocktimeInSeconds() + 120) {
				// The job has expired his clocktime request allow 100 seconds for graceful stop
				cJob.abort();
				_currentJobThread.join(1000); // Wait for 1 second
			} else {
				// Past the 100 seconds grace period. Force the job to end

				cJob.abortNow();
				_currentJobThread.interrupt();
			}

		} else if (_currentJob != null) { // Current job no longer running, but not cleared
													
			_currentJob = null;
			_timeLastRunTimeUpdate = null;

		} else if (!_jobs.isEmpty()) { // No job running and not empty queue
												
			_currentJob = new JobHandler(_jobs.poll());
			if (! WebAppGate.make().isJobCanceled(_currentJob.getJob())) {
				LOG.info("Start new job " + _currentJob._job._jobNumber);
				_currentJobThread = new Thread(_currentJob);
				_currentJobThread.start();
			        _timeLastRunTimeUpdate = LocalTime.now();
				_currentJobThread.join(1000); // Wait for 1 seconds to allow for the job to start
														
			} else {
				_currentJob = null; // If job is cancelled reset it.
			    _timeLastRunTimeUpdate = null;
			}
		}
	}
}
