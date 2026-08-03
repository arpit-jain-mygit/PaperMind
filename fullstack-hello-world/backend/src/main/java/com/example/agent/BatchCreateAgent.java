package com.example.agent;

import com.example.entity.Student;
import com.example.mcp.DBHubMCPClient;
import com.example.service.StudentSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BatchCreateAgent {

    private static final Logger logger = LoggerFactory.getLogger(BatchCreateAgent.class);

    @Autowired
    private DBHubMCPClient dbhubMcp;

    @Autowired
    private StudentSummaryService studentSummaryService;

    public BatchCreateResult processBatch(List<StudentData> studentDataList) {
        logger.info("Agent: BATCH_CREATE starting - {} records to process", studentDataList.size());

        BatchCreateResult result = new BatchCreateResult();
        Set<String> existingEmails = new HashSet<>();

        // Load existing emails to detect duplicates (using DBHub MCP)
        logger.info("Agent: Loading existing emails from database via DBHub MCP");
        try {
            List<Map<String, Object>> students = dbhubMcp.queryAll(
                "SELECT email FROM student"
            );
            students.forEach(s -> existingEmails.add((String) s.get("email")));
            logger.info("Agent: Loaded {} existing emails", existingEmails.size());
        } catch (Exception e) {
            logger.error("Agent: Failed to load existing emails: {}", e.getMessage());
            throw new RuntimeException("Failed to load existing emails: " + e.getMessage());
        }

        int processedCount = 0;
        for (int i = 0; i < studentDataList.size(); i++) {
            StudentData data = studentDataList.get(i);
            logger.info("Agent: Processing row {} - {}", i + 1, data.name);

            // DECISION 1: Is data valid?
            ValidationResult validation = validateStudentData(data);
            if (!validation.isValid) {
                logger.warn("Agent: Row {} INVALID - {}", i + 1, validation.error);
                result.addError(i + 1, data.name, validation.error);
                continue;
            }

            // DECISION 2: Does student already exist?
            if (existingEmails.contains(data.email)) {
                logger.warn("Agent: Row {} DUPLICATE - email already exists", i + 1);
                result.addDuplicate(i + 1, data.name, data.email);
                continue;
            }

            // ACTION 1: Create student (using DBHub MCP)
            logger.info("Agent: Creating student via DBHub MCP - {}", data.name);

            try {
                // Insert student using DBHub MCP
                dbhubMcp.execute(
                    "INSERT INTO student (name, email, phone_number, gpa) VALUES (?, ?, ?, ?)",
                    data.name,
                    data.email,
                    data.phoneNumber,
                    data.gpa
                );

                logger.info("Agent: Successfully created student - {}", data.name);
                existingEmails.add(data.email);

                // DECISION 3: Should we generate AI summary for high performers?
                if (data.gpa >= 3.7) {
                    logger.info("Agent: High performer detected (GPA {}), generating AI summary", data.gpa);

                    // Fetch created student for summary generation
                    Map<String, Object> createdRow = dbhubMcp.queryOne(
                        "SELECT id, name, email, phone_number, gpa FROM student WHERE email = ?",
                        data.email
                    );

                    if (createdRow != null) {
                        // Create Student object from DB row for summary service
                        Student studentForSummary = new Student();
                        studentForSummary.setId(((Number) createdRow.get("id")).longValue());
                        studentForSummary.setName((String) createdRow.get("name"));
                        studentForSummary.setEmail((String) createdRow.get("email"));
                        studentForSummary.setPhoneNumber((String) createdRow.get("phone_number"));
                        studentForSummary.setGpa(((Number) createdRow.get("gpa")).doubleValue());

                        try {
                            String summary = studentSummaryService.generateSummary(studentForSummary);
                            logger.info("Agent: AI summary generated - {}", summary.substring(0, Math.min(50, summary.length())));
                            result.addSuccess(((Number) createdRow.get("id")).longValue(), data.name, "High performer summarized");
                        } catch (Exception e) {
                            logger.warn("Agent: AI summary failed for {}: {}", data.name, e.getMessage());
                            result.addSuccess(((Number) createdRow.get("id")).longValue(), data.name, "Created (AI summary failed)");
                        }
                    }
                } else {
                    result.addSuccess(1L, data.name, "Created");
                }
                processedCount++;

            } catch (Exception e) {
                logger.error("Agent: Failed to create student {} via DBHub MCP: {}", data.name, e.getMessage());
                result.addError(i + 1, data.name, "DBHub MCP error: " + e.getMessage());
            }
        }

        logger.info("Agent: BATCH_CREATE complete - {} successful, {} errors, {} duplicates",
                processedCount, result.errors.size(), result.duplicates.size());

        result.summary = String.format(
            "Batch Processing Complete:\n" +
            "✅ Created: %d students\n" +
            "⚠️  Duplicates: %d (skipped)\n" +
            "❌ Errors: %d (invalid data)\n" +
            "📊 Total Rows: %d",
            processedCount,
            result.duplicates.size(),
            result.errors.size(),
            studentDataList.size()
        );

        return result;
    }

    private ValidationResult validateStudentData(StudentData data) {
        if (data.name == null || data.name.trim().isEmpty()) {
            return new ValidationResult(false, "Name is required");
        }
        if (data.email == null || data.email.trim().isEmpty() || !data.email.contains("@")) {
            return new ValidationResult(false, "Valid email is required");
        }
        if (data.phoneNumber == null || data.phoneNumber.trim().isEmpty()) {
            return new ValidationResult(false, "Phone number is required");
        }
        if (data.gpa < 0 || data.gpa > 4.0) {
            return new ValidationResult(false, "GPA must be between 0 and 4.0");
        }
        return new ValidationResult(true, null);
    }

    // Data classes
    public static class StudentData {
        public String name;
        public String email;
        public String phoneNumber;
        public Double gpa;

        public StudentData(String name, String email, String phoneNumber, Double gpa) {
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.gpa = gpa;
        }
    }

    private static class ValidationResult {
        boolean isValid;
        String error;

        ValidationResult(boolean isValid, String error) {
            this.isValid = isValid;
            this.error = error;
        }
    }

    public static class BatchCreateResult {
        public List<SuccessRecord> successes = new ArrayList<>();
        public List<ErrorRecord> errors = new ArrayList<>();
        public List<DuplicateRecord> duplicates = new ArrayList<>();
        public String summary;

        public void addSuccess(Long id, String name, String action) {
            successes.add(new SuccessRecord(id, name, action));
        }

        public void addError(int row, String name, String error) {
            errors.add(new ErrorRecord(row, name, error));
        }

        public void addDuplicate(int row, String name, String email) {
            duplicates.add(new DuplicateRecord(row, name, email));
        }

        public static class SuccessRecord {
            public Long id;
            public String name;
            public String action;

            public SuccessRecord(Long id, String name, String action) {
                this.id = id;
                this.name = name;
                this.action = action;
            }

            public Long getId() { return id; }
            public String getName() { return name; }
            public String getAction() { return action; }
        }

        public static class ErrorRecord {
            public int row;
            public String name;
            public String error;

            public ErrorRecord(int row, String name, String error) {
                this.row = row;
                this.name = name;
                this.error = error;
            }

            public int getRow() { return row; }
            public String getName() { return name; }
            public String getError() { return error; }
        }

        public static class DuplicateRecord {
            public int row;
            public String name;
            public String email;

            public DuplicateRecord(int row, String name, String email) {
                this.row = row;
                this.name = name;
                this.email = email;
            }

            public int getRow() { return row; }
            public String getName() { return name; }
            public String getEmail() { return email; }
        }

        public List<SuccessRecord> getSuccesses() { return successes; }
        public List<ErrorRecord> getErrors() { return errors; }
        public List<DuplicateRecord> getDuplicates() { return duplicates; }
        public String getSummary() { return summary; }
    }
}
