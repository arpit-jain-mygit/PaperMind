package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks agent decisions for learning and feedback
 * Enables the agent to learn from past decisions and adapt behavior
 */
@Entity
@Table(name = "agent_decisions")
public class AgentDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentEmail;
    private String studentName;

    @Enumerated(EnumType.STRING)
    private DecisionType decisionType; // CREATED, DUPLICATE, INVALID, CREATION_FAILED

    private String decisionReason;
    private Boolean successful; // Was the decision correct? (based on feedback)

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON: {gpa: 3.9, validated: true, ...}

    private LocalDateTime createdAt;
    private LocalDateTime feedbackAt;

    @Column(columnDefinition = "TEXT")
    private String feedback; // User feedback on whether decision was correct

    private String failurePattern; // Structured reason: "missing_at_symbol", "invalid_gpa", "empty_field", etc.

    // Constructors
    public AgentDecision() {}

    public AgentDecision(String studentEmail, String studentName, DecisionType decisionType, String reason, String metadata) {
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.decisionType = decisionType;
        this.decisionReason = reason;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
        this.successful = null; // Unknown until feedback
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public DecisionType getDecisionType() { return decisionType; }
    public void setDecisionType(DecisionType decisionType) { this.decisionType = decisionType; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }

    public Boolean getSuccessful() { return successful; }
    public void setSuccessful(Boolean successful) { this.successful = successful; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(LocalDateTime feedbackAt) { this.feedbackAt = feedbackAt; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getFailurePattern() { return failurePattern; }
    public void setFailurePattern(String failurePattern) { this.failurePattern = failurePattern; }

    // Decision Type Enum
    public enum DecisionType {
        CREATED("Student created successfully"),
        DUPLICATE("Student already exists"),
        INVALID("Student data invalid"),
        CREATION_FAILED("Student creation failed after retries"),
        HIGH_PERFORMER("High performer - AI summary generated");

        private final String description;

        DecisionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
