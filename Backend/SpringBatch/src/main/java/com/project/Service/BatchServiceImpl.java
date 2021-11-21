package com.project.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.project.Entity.BatchJob;
import com.project.Entity.JobParams;
import com.project.Entity.JobScheduleDetails;
import com.project.Entity.WorkerNode;
import com.project.QuartzJobPackage.QuartzJob;

@Service(value="batchService")
public class BatchServiceImpl implements BatchService{
	
	@Autowired
	private JobExplorer explorer;
	
	@Autowired
	private Job job;
	
	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	private JobOperator jobOperator;
	
	@Autowired
	private JobRepository jobRepository;
	
	
	@Autowired
	private TaskExplorer taskExplorer;
	
		
	@Autowired
	private Scheduler scheduler;
	
	@Value("${spring.cloud.deployer.local.working-directories-root}")
	private String logDirectoryPath;


	@Override
	public Long runJob(JobParams params) throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		Map<String, JobParameter> maps = new HashMap<>();
		maps.put("time", new JobParameter(System.currentTimeMillis()));
		maps.put("inputFiles", new JobParameter(params.getInputSource()));
		maps.put("partitionSize",new JobParameter(Integer.toString(params.getPartitionSize())));
		maps.put("mailRecipients", new JobParameter(params.getMailRecipients()));
		maps.put("jobName", new JobParameter(params.getJobName()));
		maps.put("jobDescription", new JobParameter(params.getJobDescription()));
		
		JobParameters jobParameters = new JobParameters(maps);
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);
		
		return jobExecution.getId();
	}


	@Override
	public Long restartJob(Long jobId) throws Exception {
		Long jobExecutionId = jobOperator.getExecutions(jobId).get(0);
		JobExecution execution = explorer.getJobExecution(jobExecutionId);
		
		Date jvmStartTime = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
		if((execution.getStatus().equals(BatchStatus.STARTED) || execution.getStatus().equals(BatchStatus.STARTING)) && execution.getCreateTime().before(jvmStartTime)){
			execution.setEndTime(new Date());
			execution.setStatus(BatchStatus.FAILED);
			execution.setExitStatus(ExitStatus.FAILED);
			
			for(StepExecution se: execution.getStepExecutions()) {
				if(se.getStatus().equals(BatchStatus.STARTED) || se.getStatus().equals(BatchStatus.STARTING)) {
					se.setEndTime(new Date());
					se.setStatus(BatchStatus.FAILED);
					se.setExitStatus(ExitStatus.FAILED);
					jobRepository.update(se);
				}
			}
			jobRepository.update(execution);
		}
		
		Long restartId = jobOperator.restart(jobExecutionId);
		return restartId;
	}


	@Override
	public Boolean stopJob(Long ID) throws Exception {
		Boolean status = false;
		Long jobExecutionId = jobOperator.getExecutions(ID).get(0);
		JobExecution execution = explorer.getJobExecution(jobExecutionId);
		
		Date jvmStartTime = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
		if((execution.getStatus().equals(BatchStatus.STARTED) || execution.getStatus().equals(BatchStatus.STARTING)) && execution.getCreateTime().before(jvmStartTime)){
			int readCount = 0;
			int writeCount = 0;
			StepExecution masterStep = null;
			execution.setEndTime(new Date());
			execution.setStatus(BatchStatus.FAILED);
			execution.setExitStatus(ExitStatus.FAILED);
			
			for(StepExecution se: execution.getStepExecutions()) {
				if(se.getStepName().contains("masterStep")) {
					masterStep = se;
				}else {
					readCount += se.getReadCount();
					writeCount += se.getWriteCount();
				}
				if(se.getStatus().equals(BatchStatus.STARTED) || se.getStatus().equals(BatchStatus.STARTING)) {
					se.setEndTime(new Date());
					se.setStatus(BatchStatus.FAILED);
					se.setExitStatus(ExitStatus.FAILED);
					jobRepository.update(se);
				}
			}
			masterStep.setReadCount(readCount);
			masterStep.setWriteCount(writeCount);
			jobRepository.update(masterStep);
			
			jobRepository.update(execution);
			status = true;
			}else {
				status = jobOperator.stop(jobOperator.getExecutions(ID).get(0));
			}
		return status;
	}


	@Override
	public List<BatchJob> getAllJobs() throws Exception {
		List<JobInstance> instanceIds = explorer.getJobInstances(job.getName(), 0, Integer.MAX_VALUE);
		List<BatchJob> allJobs = new ArrayList<>();
		
		for(JobInstance id: instanceIds) {
			Long executionId = jobOperator.getExecutions(id.getInstanceId()).get(0);
			JobExecution jobExecution = explorer.getJobExecution(executionId);
			BatchJob job = new BatchJob();
			job.setJobID(id.getInstanceId());
			job.setJobName(jobExecution.getJobParameters().getString("jobName"));
			job.setStatus(jobExecution.getStatus());
			job.setCreateTime(jobExecution.getCreateTime());
			job.setEndTime(jobExecution.getEndTime());
			allJobs.add(job);
		}
		return allJobs;
	}


	@Override
	public List<BatchJob> getAllJobExecutions(Long jonInstanceId) throws Exception {
		List<BatchJob> alljobs = new ArrayList<>();
		List<Long> allExecutionIds = jobOperator.getExecutions(jonInstanceId);
		for(Long eecutionID: allExecutionIds) {
			JobExecution execution = explorer.getJobExecution(eecutionID);
			
			BatchJob job = new BatchJob();
			JobParams jobParams = new JobParams();
			
			job.setJobID(execution.getId());
			job.setJobName(execution.getJobParameters().getString("jobName"));
			job.setStatus(execution.getStatus());
			job.setCreateTime(execution.getCreateTime());
			job.setEndTime(execution.getEndTime());
			
			jobParams.setInputSource(execution.getJobParameters().getString("inputFiles"));
			jobParams.setPartitionSize(Integer.parseInt(execution.getJobParameters().getString("partitionSize")));
			jobParams.setMailRecipients(execution.getJobParameters().getString("mailRecipients"));
			jobParams.setJobDescription(execution.getJobParameters().getString("jobDescription"));
			jobParams.setJobName(execution.getJobParameters().getString("jobName"));
			
			job.setJobParams(jobParams);
			alljobs.add(job);
		}
		return alljobs;
	}


	@Override
	public List<WorkerNode> getAllWorkerNodes(Long jobExecutionId) throws Exception {
		List<WorkerNode> allWorkers = new ArrayList<>();
		
		Map<Long, String> stepSummary = jobOperator.getStepExecutionSummaries(jobExecutionId);
		for(var entry : stepSummary.entrySet()) {
			StepExecution stepExecution = explorer.getStepExecution(jobExecutionId, entry.getKey());
			WorkerNode workerNode = new WorkerNode();
			workerNode.setStepName(stepExecution.getStepName());
			workerNode.setStatus(stepExecution.getStatus());
			workerNode.setStartTime(stepExecution.getStartTime());
			workerNode.setEndTime(stepExecution.getEndTime());
			workerNode.setReadCount(stepExecution.getReadCount());
			workerNode.setWriteCount(stepExecution.getWriteCount());
			
			allWorkers.add(workerNode);
		}
		return allWorkers;
	}


	@Override
	public List<String> getWorkerLogs(Long jobExecutionID, String partitionName) throws Exception {
		List<String> list = new ArrayList<>();
		Long parentTaskExecutionId = taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionID);
		Pageable page = PageRequest.of(0,Integer.MAX_VALUE);
		Page<TaskExecution> tasks = taskExplorer.findAll(page); 
		List<TaskExecution> taskList = tasks.getContent();
		
		for(TaskExecution task : taskList) {
			if(task.getParentExecutionId() != null && task.getParentExecutionId().equals(parentTaskExecutionId)) {
				String name = task.getTaskName();
				String[] na = partitionName.split(":");
				if(name.endsWith(na[na.length-1])) {
					String fileName = task.getExternalExecutionId();
					String fullName = logDirectoryPath + "/" + fileName + "/stdout.log";
					
					Scanner scanner;
					scanner = new Scanner(new File(fullName));
					while(scanner.hasNextLine()) {
						list.add(scanner.nextLine());
					}
					scanner.close();
				}
			}
		}
		return list;
	}


	@Override
	public Boolean scheduleJob(JobScheduleDetails scheduleDetails) throws Exception {
		JobDetail detail = buildJobDetail(scheduleDetails.getJobName(), scheduleDetails.getJobGroup(), scheduleDetails.getJobDescription());
		
		detail.getJobDataMap().put("inputFiles", scheduleDetails.getInputSource());
		detail.getJobDataMap().put("partitionSize", scheduleDetails.getPartitionSize());
		detail.getJobDataMap().put("mailRecipients", scheduleDetails.getMailRecipients());
		detail.getJobDataMap().put("jobName", scheduleDetails.getJobName());
		detail.getJobDataMap().put("jobDesciption", scheduleDetails.getJobDescription());
		
		Trigger trigger = buildJobTrigger(detail, scheduleDetails.getCronExpression());
		
		try {
			scheduler.scheduleJob(detail, trigger);
			return true;
		} catch (SchedulerException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	
	
	
	private JobDetail buildJobDetail(String jobName, String JobGroup, String jobDesciption) {
		return JobBuilder.newJob(QuartzJob.class).withIdentity(jobName + ":" + UUID.randomUUID().toString(), JobGroup)
				.withDescription(jobDesciption).storeDurably().build();
	}
	
	private Trigger buildJobTrigger(JobDetail jobDetail, String cronExpression) {
		return TriggerBuilder.newTrigger().forJob(jobDetail).withIdentity(jobDetail.getKey().getName(), "job-triggers")
				.withDescription("send Trigger").withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
	}


	@Override
	public List<JobScheduleDetails> getScheduledJobs() throws Exception {
		List<JobScheduleDetails> listOfScheduledJobs = new ArrayList<>();
		
		for(String groupName : scheduler.getJobGroupNames()) {
			for(JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
				if(!scheduler.getTriggersOfJob(jobKey).isEmpty()){
					Trigger trigger = scheduler.getTriggersOfJob(jobKey).get(0);
					
					String jobName = jobKey.getName();
					String jobGroup = jobKey.getGroup();
					
					JobDetail detail = scheduler.getJobDetail(jobKey);
					
					JobDataMap dataMap = detail.getJobDataMap();
					String description = detail.getDescription();
					
					JobScheduleDetails job = new JobScheduleDetails();
					job.setInputSource(dataMap.getString("inputFiles"));
					job.setMailRecipients(dataMap.getString("mailRecipients"));
					job.setPartitionSize(dataMap.getInt("partitionSize"));
					job.setCronExpression(trigger.getNextFireTime().toString());
					job.setJobName(jobName);
					job.setJobGroup(jobGroup);
					job.setJobDescription(description);
					
					listOfScheduledJobs.add(job);
					
				}else {
					scheduler.deleteJob(jobKey);
				}
			}
		}
		return listOfScheduledJobs;
	}


	@Override
	public Boolean unscheduleJob(String jobName, String jobGroup) throws Exception {
		JobKey jobKey = new JobKey(jobName, jobGroup);
		Boolean status = scheduler.deleteJob(jobKey);
		return status;
	}
	
	
}
