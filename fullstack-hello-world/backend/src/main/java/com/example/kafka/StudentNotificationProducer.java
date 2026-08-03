package com.example.kafka;

import com.example.event.StudentCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StudentNotificationProducer {

    @Autowired
    private KafkaTemplate<String, StudentCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;

    public void publishStudentCreatedEvent(StudentCreatedEvent event) {
        System.out.println("Publishing StudentCreatedEvent to Kafka topic: " + topic);
        System.out.println("Event: " + event);

        kafkaTemplate.send(topic, event.getStudentId().toString(), event);

        System.out.println("StudentCreatedEvent published successfully!");
    }
}
