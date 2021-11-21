package com.project.Entity;

import lombok.Data;

@Data
public class JobParams {

	
	private String inputSource;
	private int partitionSize;
	private String mailRecipients;
	private String jobName;
	private String jobDescription;
	
}
