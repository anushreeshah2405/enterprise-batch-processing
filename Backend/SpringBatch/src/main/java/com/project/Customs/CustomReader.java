package com.project.Customs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import com.project.Entity.User;
import com.project.Entity.WorkerStatus;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CustomReader implements ItemStreamReader<User>{
	
	private int count;
	private List<User> users;
	
	@Value("${spring.cloud.task.job-execution-id:5}")
	private String jobId;
	
	@Autowired
	private KafkaTemplate<String, WorkerStatus> kafkaTemplate;
	
	@Value("${spring.cloud.task.name:default}")
	private String taskName;
	
	private static final String TOPIC = "WorkerStatus";
	
	private static final String CURRENT_INDEX = "current.index";
	
	public CustomReader(List<String> payload) {
		System.out.println("reader Started");
		this.users = new ArrayList<User>();
		this.count = 0;
		
		try {
			for(String file : payload) {
				try(FileInputStream fis = new FileInputStream(file);
						InputStreamReader isr = new InputStreamReader(fis);
						BufferedReader br = new BufferedReader(isr)){
						
						String line = null;
						
						while((line = br.readLine()) != null){
							String[] array = line.split(",");
							User user = new User();
							
							try {
								user.setID(Long.valueOf(array[0]));
								user.setEmail(array[1]);
								user.setName(array[2]);
								user.setSurname(array[3]);
								user.setMobile(Long.valueOf(array[4]));
								this.users.add(user);
							}catch(ArrayIndexOutOfBoundsException e) {
								log.info(new StringBuffer(e.getMessage()).append(e.getStackTrace()).toString());
							}
								
						}
				}
			}
		}catch(Exception e) {
			log.info(new StringBuffer(e.getMessage()).append(e.getStackTrace()).toString());
		}
		
		System.out.println("Reader length:" + this.users.size());
		
	}
	

	@SuppressWarnings("removal")
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if(executionContext.containsKey(CURRENT_INDEX)) {
			this.count = new Long(executionContext.getLong(CURRENT_INDEX)).intValue();
		}else {
			this.count = 0;
		}
		
	}

	@SuppressWarnings("removal")
	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.put(CURRENT_INDEX, new Long(this.count).longValue());
		System.out.println("updated count :" + this.count);
	}

	@Override
	public void close() throws ItemStreamException {
		
		
	}

	@Override
	public User read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		User user = null;
		if(this.count < this.users.size()) {
			user = this.users.get(this.count);
			this.count++;
		}else {
			this.count = 0;
		}
		return user;
	}
	
	@AfterStep
	void afterStrep() {
		String[] tasknameArray = taskName.split(":");
		WorkerStatus status = new WorkerStatus();
		status.setJobExecutionId(Integer.valueOf(jobId));
		status.setMessage("completed");
		status.setPartition(tasknameArray[tasknameArray.length-1]);
		kafkaTemplate.send(TOPIC, status);
		System.out.println("message sent");
	}

}
