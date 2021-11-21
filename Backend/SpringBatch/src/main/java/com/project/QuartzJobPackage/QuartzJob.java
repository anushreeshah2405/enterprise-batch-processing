package com.project.QuartzJobPackage;

import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class QuartzJob extends QuartzJobBean{
	
	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	private Job job;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
		JobDataMap datamap = context.getJobDetail().getJobDataMap();
		
		Map<String, JobParameter> maps = new HashMap<>();
		maps.put("time", new JobParameter(System.currentTimeMillis()));
		maps.put("inputFiles", new JobParameter(datamap.getString("inputFiles")));
		maps.put("partitionSize",new JobParameter(Integer.toString(datamap.getInt("partitionSize"))));
		maps.put("mailRecipients", new JobParameter(datamap.getString("mailRecipients")));
		maps.put("jobName", new JobParameter(datamap.getString("jobName")));
		maps.put("jobDescription", new JobParameter(datamap.getString("jobDescription")));
		
		JobParameters jobParameters = new JobParameters(maps);
		try {
			jobLauncher.run(job, jobParameters);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
