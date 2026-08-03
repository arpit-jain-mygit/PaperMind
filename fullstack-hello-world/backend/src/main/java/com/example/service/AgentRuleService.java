package com.example.service;

import com.example.agent.BatchCreateAgentEnhanced.StudentData;
import com.example.entity.ValidationRule;
import com.example.repository.ValidationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentRuleService {

    private static final Logger logger = LoggerFactory.getLogger(AgentRuleService.class);

    @Autowired
    private ValidationRuleRepository validationRuleRepository;

    @Autowired
    private ExpressionEvaluator expressionEvaluator;

    public void applyRecommendationAsRule(String failurePattern, String ruleDescription, String recommendedBy) {
        logger.info("Agent Learning: Applying recommendation as rule - Pattern: {}", failurePattern);

        // Check if rule already exists
        List<ValidationRule> existingRules = validationRuleRepository.findByFailurePattern(failurePattern);
        if (!existingRules.isEmpty()) {
            logger.warn("Agent Learning: Rule already exists for pattern: {}", failurePattern);
            return;
        }

        // Extract SpEL expression from recommendation (format: "EXPRESSION: ...")
        String expression = extractExpression(ruleDescription);
        String explanation = extractExplanation(ruleDescription);

        // Validate expression before saving
        if (expression == null || !expressionEvaluator.isValidExpression(expression)) {
            logger.warn("Agent Learning: Invalid expression generated: {}", expression);
            expression = null; // Will fall back to legacy logic
        }

        // Create new rule from recommendation
        ValidationRule rule = new ValidationRule(
            failurePattern,
            getFieldNameFromPattern(failurePattern),
            explanation != null ? explanation : ruleDescription,
            generateRuleLogic(failurePattern, ruleDescription)
        );
        rule.setRuleExpression(expression); // Store the SpEL expression
        rule.setRecommendedBy(recommendedBy);
        rule.setActive(false); // Require manual approval
        rule.setUpdatedAt(LocalDateTime.now());

        validationRuleRepository.save(rule);
        logger.info("Agent Learning: Rule saved (inactive, requires approval) - ID: {}, Expression: {}", rule.getId(), expression);
    }

    private String extractExpression(String recommendation) {
        if (recommendation == null) return null;
        int start = recommendation.indexOf("EXPRESSION:");
        if (start == -1) return null;
        start += "EXPRESSION:".length();
        int end = recommendation.indexOf("\n", start);
        if (end == -1) end = recommendation.length();
        return recommendation.substring(start, end).trim();
    }

    private String extractExplanation(String recommendation) {
        if (recommendation == null) return null;
        int start = recommendation.indexOf("EXPLANATION:");
        if (start == -1) return null;
        start += "EXPLANATION:".length();
        return recommendation.substring(start).trim();
    }

    public void activateRule(Long ruleId) {
        logger.info("Agent Learning: Activating rule - ID: {}", ruleId);

        validationRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setActive(true);
            rule.setActivatedAt(LocalDateTime.now());
            validationRuleRepository.save(rule);
            logger.info("Agent Learning: Rule activated - Pattern: {}", rule.getFailurePattern());
        });
    }

    public void deactivateRule(Long ruleId) {
        logger.info("Agent Learning: Deactivating rule - ID: {}", ruleId);

        validationRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setActive(false);
            validationRuleRepository.save(rule);
            logger.info("Agent Learning: Rule deactivated - Pattern: {}", rule.getFailurePattern());
        });
    }

    public List<ValidationRule> getActiveRules() {
        return validationRuleRepository.findByActiveTrueOrderByPriorityAsc();
    }

    public List<ValidationRule> getAllRules() {
        return validationRuleRepository.findAll();
    }

    public boolean validateWithRules(StudentData data) {
        List<ValidationRule> activeRules = getActiveRules();
        logger.debug("Agent Rule: Validating with {} active rules", activeRules.size());

        for (ValidationRule rule : activeRules) {
            if (!checkRule(data, rule)) {
                logger.warn("Agent Rule: Validation failed - Pattern: {}, Description: {}",
                    rule.getFailurePattern(), rule.getRuleDescription());
                return false;
            }
        }

        return true;
    }

    private boolean checkRule(StudentData data, ValidationRule rule) {
        // Only use learned expressions from LLM - no hard-coded fallbacks
        if (rule.getRuleExpression() != null && !rule.getRuleExpression().isEmpty()) {
            logger.debug("Agent Rule: Evaluating learned expression for pattern: {}", rule.getFailurePattern());
            return expressionEvaluator.evaluateRule(data, rule.getRuleExpression());
        }

        // No rule expression means rule not yet fully learned - accept it
        logger.debug("Agent Rule: No expression for pattern {}, accepting data (learning in progress)", rule.getFailurePattern());
        return true;
    }

    private String getFieldNameFromPattern(String pattern) {
        return switch (pattern) {
            case "missing_at_symbol", "invalid_email_format" -> "email";
            case "invalid_gpa" -> "gpa";
            case "empty_field" -> "all";
            default -> "unknown";
        };
    }

    private String generateRuleLogic(String pattern, String description) {
        return switch (pattern) {
            case "missing_at_symbol" -> "email.contains('@')";
            case "invalid_gpa" -> "gpa >= 0 && gpa <= 4.0";
            case "empty_field" -> "!name.isEmpty() && !email.isEmpty() && !phone.isEmpty()";
            case "invalid_email_format" -> "email.contains('@') && email.contains('.')";
            default -> description;
        };
    }
}
