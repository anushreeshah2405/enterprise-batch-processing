package com.project.Customs;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.project.Entity.User;

@Repository
public interface MyRepository extends MongoRepository<User, String>{
	
}
