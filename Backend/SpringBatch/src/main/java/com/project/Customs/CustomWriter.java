package com.project.Customs;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.Entity.User;

@Component
public class CustomWriter implements ItemWriter<User>{
	
	@Autowired
	private MyRepository myRepository;

	@Override
	public void write(List<? extends User> items) throws Exception {
		myRepository.saveAll(items);
		
	}
	
	
	
}
