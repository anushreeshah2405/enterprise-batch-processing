package com.project.Entity;

import lombok.Data;

@Data
public class WorkerStatus {
	private int jobExecutionId;
	private String message;
	private String partition;
}
