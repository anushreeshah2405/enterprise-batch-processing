package com.project.Customs;

import java.util.Date;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.jboss.logging.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.project.Entity.WorkerStatus;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Configuration
@DependsOn("kafkaTemplate")
public class JobExecutionListnerClass implements JobExecutionListener{
	
	@Autowired
	private JavaMailSender javaMailSender;
	
	@Autowired
	private KafkaTemplate<String, WorkerStatus> kafkaTemplate;
	
	private static final String TOPIC = "WorkerStatus";

	@Override
	public void beforeJob(JobExecution jobExecution) {
		System.out.println("Job Started");
		try {
			long jid = jobExecution.getId();
			long instanceId = jobExecution.getJobId();
			MDC.put("jobExecutionId", Long.toString(jid));
			
			WorkerStatus statusworker = new WorkerStatus();
			statusworker.setJobExecutionId((int) jid);
			statusworker.setMessage("started");
			statusworker.setPartition(null);
			kafkaTemplate.send(TOPIC, statusworker);
			System.out.println("message sent");
			
			String name = jobExecution.getJobInstance().getJobName();
			Date date = jobExecution.getStartTime();
			String status = jobExecution.getStatus().toString();
			beforeJobMail(instanceId, jid, name, date, status, jobExecution.getJobParameters().getString("mailRecipients"));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		System.out.println("Job Completed");
		try {
			long jid = jobExecution.getId();
			long instanceId = jobExecution.getJobId();
			
			WorkerStatus statusworker = new WorkerStatus();
			statusworker.setJobExecutionId((int) jid);
			statusworker.setMessage("completed");
			statusworker.setPartition(null);
			kafkaTemplate.send(TOPIC, statusworker);
			System.out.println("message sent");
			
			String description = null;
			
			List<Throwable> list = jobExecution.getAllFailureExceptions();
			
			int size = jobExecution.getStepExecutions().size();
			
			if(size == 1) {
				description = "<h3>Job Completed : No Files Found In The Direcory<h3>";
			}
			for(Throwable exception: list) {
				if(exception.toString().contains("java.lang.NullPointerException")) {
					description = "<h3>Job Failed : Directory Not Found<h3>";
				}
			}
			
			String name = jobExecution.getJobInstance().getJobName();
			Date date = jobExecution.getStartTime();
			String status = jobExecution.getStatus().toString();
			afterJobMail(instanceId, jid, name, date, status, jobExecution.getJobParameters().getString("mailRecipients"), description);
		}catch(Exception e) {
			e.printStackTrace();
		}
		MDC.clear();
	}
	
	
	
	public void beforeJobMail(long instatnceId, long jid, String name, Date date, String status, String mailRecipients) {
		MimeMessage message = javaMailSender.createMimeMessage();
		String[] mail = mailRecipients.split(",");
		
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			
			for(String id : mail) {
				
				helper.setFrom("");
				helper.setTo(id);
				
				String subject = "Hey! Admin. Job Execution Has Started..";
				String jobDescription = "Job Instance ID : " + instatnceId + "\n" + "Job Execution ID : " + jid + "\n" +
										"Job Name : " + name + "\n" + "Job Status : " + status + "\n" + "Job " + status + " time : " + date; 
				
				helper.setSubject(subject);
				helper.setText(jobDescription);
				
				javaMailSender.send(message);
				System.out.println("mail sent");
				
			}
		} catch(Exception e) {
			log.info(e.getMessage());
		}
	}
	
	public void afterJobMail(long instatnceId, long jid, String name, Date date, String status, String mailRecipients, String description) {
		MimeMessage message = javaMailSender.createMimeMessage();
		String[] mail = mailRecipients.split(",");
		
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			
			for(String id : mail) {
				
				helper.setFrom("");
				helper.setTo(id);
				
				String subject = "Hey! Admin. Job Execution Has Ended..";
				
				
				String jobDescription = "Job Instance ID : " + instatnceId + "<br>" + "Job Execution ID : " + jid + "<br>" +
						"Job Name : " + name + "<br>" + "Job Status : " + status + "<br>" + "Job " + status + " time : " + date;
				
				
				String jobDescription1 = "Job Instance ID : " + instatnceId + "\n" + "Job Execution ID : " + jid + "\n" +
										"Job Name : " + name + "\n" + "Job Status : " + status + "\n" + "Job " + status + " time : " + date; 
				
				if(description == null) {
					helper.setText(jobDescription1);
				}else {
					jobDescription = jobDescription + "<br>" + description;
					helper.setText(description);
				}
				
				helper.setSubject(subject);
				
				
				javaMailSender.send(message);
				System.out.println("mail sent");
				
			}
		} catch(Exception e) {
			log.info(e.getMessage());
		}
	}

}
