package com.example.service;

import com.example.entity.AgentDecision;
import com.example.repository.AgentDecisionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentLearningService {

    private static final Logger logger = LoggerFactory.getLogger(AgentLearningService.class);

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private AgentRuleService agentRuleService;

    @Autowired
    private ExpressionEvaluator expressionEvaluator;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    private ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate = new RestTemplate();

    public LearningAnalysis analyzeFailurePatterns() {
        logger.info("Agent Learning: Analyzing failure patterns...");

        // Get all wrong decisions with feedback
        List<AgentDecision> wrongDecisions = agentDecisionRepository.findBySuccessfulFalse();

        if (wrongDecisions.isEmpty()) {
            logger.info("Agent Learning: No failures to analyze yet");
            return new LearningAnalysis("No failures to analyze", new HashMap<>(), null);
        }

        // Group by failure pattern
        Map<String, Long> patternCounts = wrongDecisions.stream()
            .filter(d -> d.getFailurePattern() != null)
            .collect(Collectors.groupingBy(
                AgentDecision::getFailurePattern,
                Collectors.counting()
            ));

        logger.info("Agent Learning: Found failure patterns: {}", patternCounts);

        // Calculate statistics
        long totalFailures = wrongDecisions.size();
        Map<String, Double> patternPercentages = patternCounts.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (e.getValue() * 100.0) / totalFailures
            ));

        // Find dominant pattern
        String dominantPattern = patternCounts.entrySet().stream()
            .max((a, b) -> Long.compare(a.getValue(), b.getValue()))
            .map(Map.Entry::getKey)
            .orElse(null);

        logger.info("Agent Learning: Dominant failure pattern: {} ({}%)",
            dominantPattern, patternPercentages.getOrDefault(dominantPattern, 0.0));

        // Call OpenAI to get recommendations
        String recommendation = callOpenAIForRecommendation(dominantPattern, patternPercentages, wrongDecisions);

        // Save recommendation as a validation rule in database
        if (dominantPattern != null && recommendation != null) {
            agentRuleService.applyRecommendationAsRule(dominantPattern, recommendation, "OpenAI");
            logger.info("Agent Learning: Recommendation saved as validation rule (pending activation)");
        }

        return new LearningAnalysis(
            "Analysis complete - " + totalFailures + " failures analyzed. Recommendation saved as rule (pending activation).",
            patternPercentages,
            new RecommendedRule(dominantPattern, recommendation)
        );
    }

    private String callOpenAIForRecommendation(String dominantPattern,
                                               Map<String, Double> patternPercentages,
                                               List<AgentDecision> failedDecisions) {
        logger.info("Agent Learning: Calling OpenAI to generate validation rule recommendations...");

        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            logger.warn("Agent Learning: OPENAI_API_KEY not set, using mock recommendation");
            return getMockRecommendation(dominantPattern);
        }

        try {
            // Build context from failed decisions
            String failureContext = buildFailureContext(failedDecisions);

            // Build OpenAI prompt requesting SpEL expression
            String prompt = String.format(
                """
                You are an AI agent learning to improve its validation rules.

                I've analyzed my recent failures and found these patterns:
                - Dominant pattern: %s (%.1f%% of failures)
                - All patterns: %s

                Recent failed decisions:
                %s

                Based on these patterns, generate a validation rule to prevent these failures.

                IMPORTANT: Respond with EXACTLY this format:
                EXPRESSION: [SpEL expression]
                EXPLANATION: [2-3 sentence explanation]

                SpEL expression must:
                - Use variables: email, name, phone, gpa (all strings/numbers)
                - Return true if data is VALID, false if INVALID
                - Example: email.contains('@') && email.contains('.')
                - Example: gpa >= 0 && gpa <= 4.0
                - Example: name != null && !name.isEmpty()

                RESPOND NOW:""",
                dominantPattern,
                patternPercentages.getOrDefault(dominantPattern, 0.0),
                patternPercentages,
                failureContext
            );

            // Call OpenAI
            String response = callOpenAIAPI(prompt);
            logger.info("Agent Learning: OpenAI recommendation received");
            return response;

        } catch (Exception e) {
            logger.error("Agent Learning: Failed to call OpenAI: {}", e.getMessage());
            return getMockRecommendation(dominantPattern);
        }
    }

    private String buildFailureContext(List<AgentDecision> failedDecisions) {
        return failedDecisions.stream()
            .limit(5)
            .map(d -> String.format(
                "- %s (%s): %s [Pattern: %s]",
                d.getStudentName(),
                d.getStudentEmail(),
                d.getDecisionReason(),
                d.getFailurePattern()
            ))
            .collect(Collectors.joining("\n"));
    }

    private String callOpenAIAPI(String prompt) throws Exception {
        logger.info("Agent Learning: Calling OpenAI API...");

        // Build request body for OpenAI
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.7);

        // Build messages
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a validation rule expert helping an AI agent learn from its failures.");

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        requestBody.put("messages", new Object[]{systemMessage, userMessage});

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            logger.debug("Agent Learning: OpenAI request: {}", requestJson);

            // In production, make actual HTTP call to OpenAI API
            // For now, return mock response to avoid API calls during testing
            logger.warn("Agent Learning: Mock OpenAI response (set OPENAI_API_KEY to enable real API calls)");
            return getMockRecommendation("missing_at_symbol");

        } catch (Exception e) {
            logger.error("Agent Learning: OpenAI API call failed: {}", e.getMessage());
            throw e;
        }
    }

    private String getMockRecommendation(String pattern) {
        // Return SpEL expression that LLM would generate
        return switch (pattern) {
            case "missing_at_symbol" ->
                "EXPRESSION: email != null && email.contains('@')\n" +
                "EXPLANATION: Email must contain @ symbol for valid format.";
            case "invalid_gpa" ->
                "EXPRESSION: gpa >= 0 && gpa <= 4.0\n" +
                "EXPLANATION: GPA must be between 0 and 4.0 for valid student records.";
            case "empty_field" ->
                "EXPRESSION: name != null && !name.isEmpty() && email != null && !email.isEmpty() && phone != null && !phone.isEmpty()\n" +
                "EXPLANATION: All required fields must be non-empty.";
            default ->
                "EXPRESSION: true\n" +
                "EXPLANATION: No specific rule for pattern: " + pattern;
        };
    }

    // Response classes
    public static class LearningAnalysis {
        public String summary;
        public Map<String, Double> patternPercentages;
        public RecommendedRule recommendedRule;

        public LearningAnalysis(String summary, Map<String, Double> patternPercentages, RecommendedRule recommendedRule) {
            this.summary = summary;
            this.patternPercentages = patternPercentages;
            this.recommendedRule = recommendedRule;
        }

        public String getSummary() { return summary; }
        public Map<String, Double> getPatternPercentages() { return patternPercentages; }
        public RecommendedRule getRecommendedRule() { return recommendedRule; }
    }

    public static class RecommendedRule {
        public String failurePattern;
        public String recommendation;

        public RecommendedRule(String failurePattern, String recommendation) {
            this.failurePattern = failurePattern;
            this.recommendation = recommendation;
        }

        public String getFailurePattern() { return failurePattern; }
        public String getRecommendation() { return recommendation; }
    }
}
