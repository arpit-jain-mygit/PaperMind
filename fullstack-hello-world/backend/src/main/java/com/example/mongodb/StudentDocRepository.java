package com.example.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentDocRepository extends MongoRepository<StudentDoc, String> {
    StudentDoc findByPostgresId(Long postgresId);
}
