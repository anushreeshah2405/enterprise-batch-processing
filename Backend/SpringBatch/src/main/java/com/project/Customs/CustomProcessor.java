package com.project.Customs;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.project.Entity.User;

@Component
public class CustomProcessor implements ItemProcessor<User, User>{

	@Override
	public User process(User item) throws Exception {
		return item;
	}

}
