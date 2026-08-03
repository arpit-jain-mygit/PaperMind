package com.example.service;

import com.example.entity.Student;
import com.example.ai.AIProvider;
import com.example.ai.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(StudentSummaryService.class);

    @Autowired
    private AIProviderFactory aiProviderFactory;

    public String generateSummary(Student student) {
        logger.info("Summary: Starting AI summary generation for student ID: {} ({})", student.getId(), student.getName());

        AIProvider provider = aiProviderFactory.getProvider();
        logger.info("Summary: Using provider: {}", provider.getProviderName());

        try {
            String summary = provider.generateSummary(student);
            logger.info("Summary: Generated successfully using {}", provider.getProviderName());
            return summary;
        } catch (Exception e) {
            logger.error("Summary: Error generating summary: {}", e.getMessage());
            throw e;
        }
    }
}
