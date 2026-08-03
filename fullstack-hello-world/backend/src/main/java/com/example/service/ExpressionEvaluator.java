package com.example.service;

import com.example.agent.BatchCreateAgentEnhanced.StudentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Service
public class ExpressionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Evaluate a SpEL expression against student data
     * Example: "email.contains('@') && email.contains('.')"
     */
    public boolean evaluateRule(StudentData data, String expression) {
        try {
            logger.debug("Evaluating expression: {}", expression);

            // Create context with student data
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("data", data);
            context.setVariable("email", data.email);
            context.setVariable("name", data.name);
            context.setVariable("phone", data.phoneNumber);
            context.setVariable("gpa", data.gpa);

            // Parse and evaluate expression
            Expression expr = parser.parseExpression(expression);
            Object result = expr.getValue(context);

            boolean passes = result instanceof Boolean ? (Boolean) result : false;
            logger.debug("Expression result: {} -> {}", expression, passes);

            return passes;

        } catch (Exception e) {
            logger.error("Error evaluating expression '{}': {}", expression, e.getMessage());
            return false; // Fail safely if expression is invalid
        }
    }

    /**
     * Validate expression syntax before saving to DB
     */
    public boolean isValidExpression(String expression) {
        try {
            parser.parseExpression(expression);
            return true;
        } catch (Exception e) {
            logger.warn("Invalid expression syntax: {}", expression);
            return false;
        }
    }
}
