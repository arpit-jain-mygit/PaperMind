package com.example.controller;

import com.example.agent.BatchCreateAgentEnhanced;
import com.example.agent.BatchCreateAgentEnhanced.StudentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private BatchCreateAgentEnhanced batchCreateAgent;

    @PostMapping("/create-students")
    public BatchCreateAgentEnhanced.BatchCreateResult createStudentsInBatch(@RequestBody BatchRequest request) {
        logger.info("API: POST /api/batch/create-students - {} records", request.students.size());

        try {
            BatchCreateAgentEnhanced.BatchCreateResult result = batchCreateAgent.processBatch(request.students);
            logger.info("API: Batch processing complete - {} success, {} errors",
                    result.getSuccesses().size(), result.getErrors().size());
            return result;
        } catch (Exception e) {
            logger.error("API: Batch processing failed: {}", e.getMessage());
            throw new RuntimeException("Batch processing failed: " + e.getMessage());
        }
    }

    public static class BatchRequest {
        public List<StudentData> students;

        public List<StudentData> getStudents() { return students; }
        public void setStudents(List<StudentData> students) { this.students = students; }
    }
}
