package com.example.event;

import java.io.Serializable;

public class StudentCreatedEvent implements Serializable {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String phoneNumber;
    private Double gpa;
    private Long timestamp;

    public StudentCreatedEvent() {}

    public StudentCreatedEvent(Long studentId, String studentName, String studentEmail,
                              String phoneNumber, Double gpa) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.phoneNumber = phoneNumber;
        this.gpa = gpa;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StudentCreatedEvent{" +
                "studentId=" + studentId +
                ", studentName='" + studentName + '\'' +
                ", studentEmail='" + studentEmail + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", gpa=" + gpa +
                ", timestamp=" + timestamp +
                '}';
    }
}
