package com.project.Service;

import java.util.List;

import com.project.Entity.BatchJob;
import com.project.Entity.JobParams;
import com.project.Entity.JobScheduleDetails;
import com.project.Entity.WorkerNode;

public interface BatchService {
	public Long runJob(JobParams params) throws Exception;
	
	public Long restartJob(Long jobId) throws Exception;
	
	public Boolean stopJob(Long id) throws Exception;
	
	public List<BatchJob> getAllJobs() throws Exception;
	
	public List<BatchJob> getAllJobExecutions(Long jonInstanceId) throws Exception;
	
	public List<WorkerNode> getAllWorkerNodes(Long jobExecutionId) throws Exception;
	
	public List<String> getWorkerLogs(Long jobExecutionID, String partitionName) throws Exception;
	
	public Boolean scheduleJob(JobScheduleDetails scheduleDetails) throws Exception;
	
	public List<JobScheduleDetails> getScheduledJobs() throws Exception;
	
	public Boolean unscheduleJob(String jobName, String jobGroup) throws Exception;
}
