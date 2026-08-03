package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validation_rules")
public class ValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String failurePattern;  // "missing_at_symbol", "invalid_gpa", etc.
    private String fieldName;       // "email", "gpa", "phone", etc.

    @Column(columnDefinition = "TEXT")
    private String ruleDescription; // Human-readable: "Email must contain @ symbol"

    @Column(columnDefinition = "TEXT")
    private String ruleLogic;      // Legacy: Java code description

    @Column(columnDefinition = "TEXT")
    private String ruleExpression; // SpEL expression: "email.contains('@') && email.contains('.')"

    private Boolean active;        // Is this rule currently used?
    private Integer priority;      // Order of evaluation (1 = check first)

    @Column(columnDefinition = "TEXT")
    private String recommendedBy;  // Who suggested this rule (e.g., "OpenAI", "Admin")

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;

    // Constructors
    public ValidationRule() {}

    public ValidationRule(String failurePattern, String fieldName, String ruleDescription, String ruleLogic) {
        this.failurePattern = failurePattern;
        this.fieldName = fieldName;
        this.ruleDescription = ruleDescription;
        this.ruleLogic = ruleLogic;
        this.active = false;
        this.priority = 100;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFailurePattern() { return failurePattern; }
    public void setFailurePattern(String failurePattern) { this.failurePattern = failurePattern; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getRuleDescription() { return ruleDescription; }
    public void setRuleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; }

    public String getRuleLogic() { return ruleLogic; }
    public void setRuleLogic(String ruleLogic) { this.ruleLogic = ruleLogic; }

    public String getRuleExpression() { return ruleExpression; }
    public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getRecommendedBy() { return recommendedBy; }
    public void setRecommendedBy(String recommendedBy) { this.recommendedBy = recommendedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
}
