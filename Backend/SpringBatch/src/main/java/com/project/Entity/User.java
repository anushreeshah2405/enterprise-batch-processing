package com.project.Entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
@Document
public class User {
	
	@Id
	private Long ID;
	private String name;
	private String surname;
	private String email;
	private Long mobile;	

}


