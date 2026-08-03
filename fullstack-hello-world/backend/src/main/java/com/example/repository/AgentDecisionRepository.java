package com.example.repository;

import com.example.entity.AgentDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentDecisionRepository extends JpaRepository<AgentDecision, Long> {

    // Find all decisions for a student
    List<AgentDecision> findByStudentEmail(String studentEmail);

    // Find decisions by type
    List<AgentDecision> findByDecisionType(AgentDecision.DecisionType decisionType);

    // Find failed decisions (to learn from mistakes)
    @Query("SELECT d FROM AgentDecision d WHERE d.successful = false ORDER BY d.createdAt DESC")
    List<AgentDecision> findBySuccessfulFalse();

    // Find decisions that need feedback
    List<AgentDecision> findBySuccessfulNull();

    // Find recent decisions
    @Query("SELECT d FROM AgentDecision d WHERE d.createdAt >= :since ORDER BY d.createdAt DESC")
    List<AgentDecision> findRecentDecisions(@Param("since") LocalDateTime since);

    // Find correct decisions
    List<AgentDecision> findBySuccessfulTrue();

    // Find incorrect decisions (user feedback says decision was wrong)
    @Query("SELECT d FROM AgentDecision d WHERE d.successful = false ORDER BY d.createdAt DESC")
    List<AgentDecision> findRecentMistakes();

    // Analysis: What types of decisions fail most?
    @Query("SELECT d.decisionType, COUNT(d) FROM AgentDecision d WHERE d.successful = false GROUP BY d.decisionType ORDER BY COUNT(d) DESC")
    List<Object[]> findFailurePatterns();
}
