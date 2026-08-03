package com.example.controller;

import com.example.entity.ValidationRule;
import com.example.service.AgentRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/rules")
public class RuleController {

    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    @Autowired
    private AgentRuleService agentRuleService;

    /**
     * Get all active validation rules
     * GET /api/agent/rules/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveRules() {
        logger.info("API: GET /api/agent/rules/active - Fetching active rules");

        List<ValidationRule> rules = agentRuleService.getActiveRules();

        Map<String, Object> response = new HashMap<>();
        response.put("activeRules", rules);
        response.put("count", rules.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all validation rules (active and inactive)
     * GET /api/agent/rules
     */
    @GetMapping
    public ResponseEntity<?> getAllRules() {
        logger.info("API: GET /api/agent/rules - Fetching all rules");

        List<ValidationRule> rules = agentRuleService.getAllRules();

        Map<String, Object> response = new HashMap<>();
        response.put("allRules", rules);
        response.put("totalCount", rules.size());
        response.put("activeCount", rules.stream().filter(r -> r.getActive()).count());
        response.put("inactiveCount", rules.stream().filter(r -> !r.getActive()).count());

        return ResponseEntity.ok(response);
    }

    /**
     * Activate a validation rule
     * POST /api/agent/rules/{ruleId}/activate
     */
    @PostMapping("/{ruleId}/activate")
    public ResponseEntity<?> activateRule(@PathVariable Long ruleId) {
        logger.info("API: POST /api/agent/rules/{}/activate - Activating rule", ruleId);

        try {
            agentRuleService.activateRule(ruleId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rule activated successfully");
            response.put("ruleId", ruleId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("API: Failed to activate rule: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to activate rule: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Deactivate a validation rule
     * POST /api/agent/rules/{ruleId}/deactivate
     */
    @PostMapping("/{ruleId}/deactivate")
    public ResponseEntity<?> deactivateRule(@PathVariable Long ruleId) {
        logger.info("API: POST /api/agent/rules/{}/deactivate - Deactivating rule", ruleId);

        try {
            agentRuleService.deactivateRule(ruleId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rule deactivated successfully");
            response.put("ruleId", ruleId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("API: Failed to deactivate rule: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to deactivate rule: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
