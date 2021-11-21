package com.project.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.project.Entity.WorkerStatus;

@Configuration
@EnableScheduling
@Profile("worker")
public class StatusScheduler {
	
	@Autowired
	private KafkaTemplate<String, WorkerStatus> kafkaTemplate;
	
	@Value("${spring.cloud.task.name:default}")
	private String taskName;
	
	@Value("${spring.cloud.task.job-execution-id:5}")
	private String jobId;
	
	private static final String TOPIC = "WorkerStatus";
	
	@Scheduled(fixedRate = 10000)
	void sendStatus() {
		String[] tasknameArray = taskName.split(":");
		WorkerStatus status = new WorkerStatus();
		status.setJobExecutionId(Integer.valueOf(jobId));
		status.setMessage("running");
		status.setPartition(tasknameArray[tasknameArray.length-1]);
		kafkaTemplate.send(TOPIC, status);
		System.out.println("message sent");
	}
	
	
}
