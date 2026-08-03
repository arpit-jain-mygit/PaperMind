package com.example.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "students")
public class StudentDoc {

    @Id
    private String id;
    private Long postgresId;
    private String name;
    private String email;
    private String phoneNumber;
    private Double gpa;

    public StudentDoc() {}

    public StudentDoc(Long postgresId, String name, String email, String phoneNumber, Double gpa) {
        this.postgresId = postgresId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gpa = gpa;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getPostgresId() {
        return postgresId;
    }

    public void setPostgresId(Long postgresId) {
        this.postgresId = postgresId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Double getGpa() {
        return gpa;
    }

    public void setGpa(Double gpa) {
        this.gpa = gpa;
    }
}
