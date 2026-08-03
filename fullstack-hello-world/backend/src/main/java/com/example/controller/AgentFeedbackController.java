package com.example.controller;

import com.example.entity.AgentDecision;
import com.example.repository.AgentDecisionRepository;
import com.example.service.AgentLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Feedback Controller
 * Allows users to provide feedback on agent decisions
 * Agent learns from this feedback to improve future decisions
 */
@RestController
@RequestMapping("/api/agent/feedback")
public class AgentFeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(AgentFeedbackController.class);

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private AgentLearningService agentLearningService;

    /**
     * Analyze failure patterns and get AI recommendations
     * GET /api/agent/feedback/analyze
     */
    @GetMapping("/analyze")
    public ResponseEntity<?> analyzeFailurePatterns() {
        logger.info("API: GET /api/agent/feedback/analyze - Analyzing failure patterns");

        try {
            AgentLearningService.LearningAnalysis analysis = agentLearningService.analyzeFailurePatterns();
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            logger.error("API: Analysis failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Analysis failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get decision history for a student
     * GET /api/agent/feedback/student/{email}
     */
    @GetMapping("/student/{email}")
    public ResponseEntity<?> getStudentDecisions(@PathVariable String email) {
        logger.info("API: GET /api/agent/feedback/student/{} - Fetching decision history", email);

        List<AgentDecision> decisions = agentDecisionRepository.findByStudentEmail(email);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("totalDecisions", decisions.size());
        response.put("decisions", decisions);

        return ResponseEntity.ok(response);
    }

    /**
     * Provide feedback on a specific decision
     * POST /api/agent/feedback/{decisionId}
     *
     * Body:
     * {
     *   "successful": true/false,
     *   "feedback": "This decision was correct/incorrect because...",
     *   "failurePattern": "missing_at_symbol" (optional, for learning)
     * }
     */
    @PostMapping("/{decisionId}")
    public ResponseEntity<?> provideFeedback(
        @PathVariable Long decisionId,
        @RequestBody FeedbackRequest feedbackRequest
    ) {
        logger.info("API: POST /api/agent/feedback/{} - Providing feedback", decisionId);

        Optional<AgentDecision> decisionOpt = agentDecisionRepository.findById(decisionId);

        if (decisionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AgentDecision decision = decisionOpt.get();
        decision.setSuccessful(feedbackRequest.successful);
        decision.setFeedback(feedbackRequest.feedback);
        if (feedbackRequest.failurePattern != null) {
            decision.setFailurePattern(feedbackRequest.failurePattern);
            logger.info("Agent Learning: Recorded failure pattern: {}", feedbackRequest.failurePattern);
        }
        decision.setFeedbackAt(LocalDateTime.now());

        agentDecisionRepository.save(decision);

        logger.info("Agent: Feedback recorded for decision {} - Successful: {}", decisionId, feedbackRequest.successful);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Feedback recorded successfully");
        response.put("decisionId", decisionId);
        response.put("successful", feedbackRequest.successful);

        return ResponseEntity.ok(response);
    }

    /**
     * Get agent analytics / learning insights
     * GET /api/agent/feedback/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        logger.info("API: GET /api/agent/feedback/analytics - Getting agent insights");

        // Total decisions
        long totalDecisions = agentDecisionRepository.count();

        // Decisions with feedback
        List<AgentDecision> successfulDecisions = agentDecisionRepository.findBySuccessfulTrue();
        List<AgentDecision> failedDecisions = agentDecisionRepository.findBySuccessfulFalse();
        List<AgentDecision> pendingFeedback = agentDecisionRepository.findBySuccessfulNull();

        // Calculate accuracy
        long correct = successfulDecisions.size();
        long incorrect = failedDecisions.size();
        long withFeedback = correct + incorrect;
        double accuracy = withFeedback > 0 ? (double) correct / withFeedback * 100 : 0;

        // Failure patterns
        List<AgentDecision> recentMistakes = agentDecisionRepository.findRecentMistakes();

        Map<String, Object> response = new HashMap<>();
        response.put("totalDecisions", totalDecisions);
        response.put("decisionBreakdown", Map.of(
            "successful", correct,
            "failed", incorrect,
            "pendingFeedback", pendingFeedback.size()
        ));
        response.put("accuracy", String.format("%.1f%%", accuracy));
        response.put("recentMistakes", recentMistakes);
        response.put("insight", getInsight(accuracy));

        return ResponseEntity.ok(response);
    }

    /**
     * Get pending decisions (no feedback yet)
     * GET /api/agent/feedback/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingDecisions() {
        logger.info("API: GET /api/agent/feedback/pending - Fetching pending decisions");

        List<AgentDecision> pendingDecisions = agentDecisionRepository.findBySuccessfulNull();

        Map<String, Object> response = new HashMap<>();
        response.put("pendingDecisions", pendingDecisions);
        response.put("count", pendingDecisions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get feedback summary
     * GET /api/agent/feedback/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getFeedbackSummary() {
        logger.info("API: GET /api/agent/feedback/summary - Getting feedback summary");

        long totalWithFeedback = agentDecisionRepository.findBySuccessfulTrue().size() +
                                agentDecisionRepository.findBySuccessfulFalse().size();
        long pendingFeedback = agentDecisionRepository.findBySuccessfulNull().size();

        Map<String, Object> response = new HashMap<>();
        response.put("totalWithFeedback", totalWithFeedback);
        response.put("pendingFeedback", pendingFeedback);
        response.put("message", "Please provide feedback on " + pendingFeedback + " pending decisions to help the agent learn");

        return ResponseEntity.ok(response);
    }

    /**
     * Generate insight based on agent performance
     */
    private String getInsight(double accuracy) {
        if (accuracy >= 95) {
            return "🟢 Excellent! Agent decisions are highly accurate.";
        } else if (accuracy >= 80) {
            return "🟡 Good. Agent is performing well but could improve with more feedback.";
        } else if (accuracy >= 60) {
            return "🟠 Fair. Agent needs more feedback to improve decision quality.";
        } else {
            return "🔴 Poor. Agent needs significant feedback to improve.";
        }
    }

    /**
     * Feedback Request DTO
     */
    public static class FeedbackRequest {
        public Boolean successful;     // true = decision was correct, false = decision was wrong
        public String feedback;        // Explanation of why the decision was right/wrong
        public String failurePattern;  // Structured pattern: "missing_at_symbol", "invalid_gpa", etc.

        // Getters
        public Boolean getSuccessful() { return successful; }
        public String getFeedback() { return feedback; }
        public String getFailurePattern() { return failurePattern; }
    }
}
