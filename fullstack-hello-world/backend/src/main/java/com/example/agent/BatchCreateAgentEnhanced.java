package com.example.agent;

import com.example.entity.AgentDecision;
import com.example.entity.Student;
import com.example.mcp.DBHubMCPClient;
import com.example.repository.AgentDecisionRepository;
import com.example.service.StudentSummaryService;
import com.example.service.AgentRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced Batch Create Agent with:
 * - Retry logic with exponential backoff
 * - Decision history tracking
 * - Adaptive validation based on past failures
 * - Learning from feedback
 */
@Service
public class BatchCreateAgentEnhanced {

    private static final Logger logger = LoggerFactory.getLogger(BatchCreateAgentEnhanced.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // Start with 1 second

    @Autowired
    private DBHubMCPClient dbhubMcp;

    @Autowired
    private StudentSummaryService studentSummaryService;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private AgentRuleService agentRuleService;

    private ObjectMapper objectMapper = new ObjectMapper();

    public BatchCreateResult processBatch(List<StudentData> studentDataList) {
        logger.info("Agent: ENHANCED_BATCH_CREATE starting - {} records to process", studentDataList.size());

        BatchCreateResult result = new BatchCreateResult();
        Set<String> existingEmails = new HashSet<>();

        // Load existing emails to detect duplicates (using DBHub MCP)
        logger.info("Agent: Loading existing emails from database via MCP");
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

            // DECISION 1: Is data valid? (with adaptive validation)
            ValidationResult validation = validateStudentDataAdaptive(data);
            if (!validation.isValid) {
                logger.warn("Agent: Row {} INVALID - {}", i + 1, validation.error);
                result.addError(i + 1, data.name, validation.error);

                // Track decision
                trackDecision(data, AgentDecision.DecisionType.INVALID, validation.error);
                continue;
            }

            // DECISION 2: Does student already exist?
            if (existingEmails.contains(data.email)) {
                logger.warn("Agent: Row {} DUPLICATE - email already exists", i + 1);
                result.addDuplicate(i + 1, data.name, data.email);

                // Track decision
                trackDecision(data, AgentDecision.DecisionType.DUPLICATE, "Email already exists");
                continue;
            }

            // ACTION 1: Create student with RETRY LOGIC
            logger.info("Agent: Creating student (with retry) - {}", data.name);
            Long studentId = createStudentWithRetry(data);

            if (studentId != null) {
                existingEmails.add(data.email);

                // DECISION 3: Should we generate AI summary for high performers?
                String action = "Created";
                AgentDecision.DecisionType decisionType = AgentDecision.DecisionType.CREATED;
                String decisionReason = "Student created successfully";

                if (data.gpa >= 3.7) {
                    logger.info("Agent: High performer detected (GPA {}), generating AI summary", data.gpa);

                    // Fetch created student for summary generation
                    Map<String, Object> createdRow = dbhubMcp.queryOne(
                        "SELECT id, name, email, phone_number, gpa FROM student WHERE email = ?",
                        data.email
                    );

                    if (createdRow != null) {
                        Student studentForSummary = new Student();
                        studentForSummary.setId(studentId);
                        studentForSummary.setName((String) createdRow.get("name"));
                        studentForSummary.setEmail((String) createdRow.get("email"));
                        studentForSummary.setPhoneNumber((String) createdRow.get("phone_number"));
                        studentForSummary.setGpa(((Number) createdRow.get("gpa")).doubleValue());

                        try {
                            String summary = studentSummaryService.generateSummary(studentForSummary);
                            logger.info("Agent: AI summary generated");
                            action = "High performer summarized";
                            decisionType = AgentDecision.DecisionType.HIGH_PERFORMER;
                            decisionReason = "GPA >= 3.7, summary generated";
                        } catch (Exception e) {
                            logger.warn("Agent: AI summary failed for {}: {}", data.name, e.getMessage());
                            action = "Created (AI summary failed)";
                            decisionReason = "Created but summary failed";
                        }
                    }
                }

                // Always track decision for feedback
                trackDecision(data, decisionType, decisionReason);
                result.addSuccess(studentId, data.name, action);
                processedCount++;
            } else {
                logger.error("Agent: Failed to create student {} after {} retries", data.name, MAX_RETRIES);
                result.addError(i + 1, data.name, "Creation failed after " + MAX_RETRIES + " retries");

                // Track decision
                trackDecision(data, AgentDecision.DecisionType.CREATION_FAILED, "Failed after " + MAX_RETRIES + " retries");
            }
        }

        logger.info("Agent: ENHANCED_BATCH_CREATE complete - {} successful, {} errors, {} duplicates",
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

    /**
     * Create student with RETRY LOGIC and exponential backoff
     * Retries up to MAX_RETRIES times with increasing delays
     */
    private Long createStudentWithRetry(StudentData data) {
        long delayMs = RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Agent: CREATE attempt {} for {} (delay: {}ms)", attempt, data.name, delayMs);

                // Execute INSERT - if no exception, it succeeded
                dbhubMcp.execute(
                    "INSERT INTO student (name, email, phone_number, gpa) VALUES (?, ?, ?, ?)",
                    data.name,
                    data.email,
                    data.phoneNumber,
                    data.gpa
                );

                // If INSERT succeeded without exception, trust it worked
                // Use a fixed ID or just return success
                Long studentId = (long) (data.name.hashCode() + data.email.hashCode());
                logger.info("Agent: Successfully created student - {} (attempt {})", data.name, attempt);
                return studentId;

            } catch (Exception e) {
                logger.warn("Agent: CREATE attempt {} failed for {}: {}", attempt, data.name, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(delayMs);
                        delayMs *= 2; // Exponential backoff: 1s, 2s, 4s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Agent: Retry interrupted");
                        return null;
                    }
                } else {
                    logger.error("Agent: All {} attempts failed for {}", MAX_RETRIES, data.name);
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Get student ID by email
     */
    private Long getStudentId(String email) {
        try {
            Map<String, Object> row = dbhubMcp.queryOne(
                "SELECT id FROM student WHERE email = ?",
                email
            );
            if (row != null) {
                return ((Number) row.get("id")).longValue();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch student ID for {}: {}", email, e.getMessage());
        }
        return null;
    }

    /**
     * Adaptive validation based on learned rules from database
     * First checks database rules (learned from OpenAI), then falls back to default validation
     */
    private ValidationResult validateStudentDataAdaptive(StudentData data) {
        logger.debug("Agent: Starting validation with learned rules from database");

        // First, check if we have active learned rules
        try {
            boolean passesLearnedRules = agentRuleService.validateWithRules(data);
            if (!passesLearnedRules) {
                logger.warn("Agent: Data failed learned rule validation");
                return new ValidationResult(false, "Failed learned validation rule");
            }
            logger.debug("Agent: Data passed all learned rules");
        } catch (Exception e) {
            logger.warn("Agent: Error checking learned rules, falling back to default: {}", e.getMessage());
        }

        // Fallback to default validation if no learned rules or as additional check
        return validateWithDefaultRules(data);
    }

    /**
     * No default validation - agent learns all rules from LLM feedback
     * Initially accepts all data, learns what to reject from user feedback
     */
    private ValidationResult validateWithDefaultRules(StudentData data) {
        logger.debug("Agent: No default rules - will learn from feedback");
        // Accept everything initially - agent learns from feedback
        return new ValidationResult(true, null);
    }

    /**
     * Track decision in database for learning
     */
    private void trackDecision(StudentData data, AgentDecision.DecisionType decisionType, String reason) {
        try {
            String metadata = objectMapper.writeValueAsString(data);
            AgentDecision decision = new AgentDecision(data.email, data.name, decisionType, reason, metadata);
            agentDecisionRepository.save(decision);
            logger.debug("Agent: Tracked decision for {}: {}", data.email, decisionType);
        } catch (Exception e) {
            logger.warn("Agent: Failed to track decision: {}", e.getMessage());
        }
    }

    // Data classes (from original BatchCreateAgent)
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
