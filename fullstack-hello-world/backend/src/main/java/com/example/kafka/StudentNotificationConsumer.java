package com.example.kafka;

import com.example.event.StudentCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class StudentNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    // @KafkaListener disabled - Kafka persistence corrupted beyond recovery
    public void consumeStudentCreatedEvent(StudentCreatedEvent event) {
        System.out.println("Received StudentCreatedEvent from Kafka: " + event);
        notificationService.sendNotification(event);
        System.out.println("Notification processed for student: " + event.getStudentName());
    }
}
