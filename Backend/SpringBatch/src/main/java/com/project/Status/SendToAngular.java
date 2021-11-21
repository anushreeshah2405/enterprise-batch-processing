package com.project.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.project.Entity.WorkerStatus;

import org.springframework.kafka.annotation.KafkaListener;

@Component
@Profile("!worker")
public class SendToAngular {
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	@KafkaListener(topics = "WorkerStatus", groupId = "Status", containerFactory = "kafkaListenerContainerFactory")
	void sendStatus(WorkerStatus input) {
		System.out.println("message recieved");
		messagingTemplate.convertAndSend("/topic/public",input);
	}
}
