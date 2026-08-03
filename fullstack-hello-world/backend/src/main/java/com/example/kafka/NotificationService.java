package com.example.kafka;

import com.example.event.StudentCreatedEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

@Service
public class NotificationService {

    public void sendNotification(StudentCreatedEvent event) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔔 STUDENT CREATED NOTIFICATION");
        System.out.println("=".repeat(70));
        System.out.println("Notification Type: Student Registration");
        System.out.println("Student ID: " + event.getStudentId());
        System.out.println("Student Name: " + event.getStudentName());
        System.out.println("Student Email: " + event.getStudentEmail());
        System.out.println("Phone Number: " + event.getPhoneNumber());
        System.out.println("GPA: " + event.getGpa());

        // Convert timestamp to readable format
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getTimestamp()),
                ZoneId.systemDefault()
        );
        System.out.println("Created At: " + dateTime);
        System.out.println("Status: ✅ NOTIFICATION SENT");
        System.out.println("=".repeat(70) + "\n");

        // Here you could add:
        // - Email sending
        // - SMS notification
        // - Push notification
        // - Database logging
        // - Webhook calls
        // etc.
    }
}
