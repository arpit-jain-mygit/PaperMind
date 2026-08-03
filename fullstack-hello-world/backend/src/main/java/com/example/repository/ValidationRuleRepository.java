package com.example.repository;

import com.example.entity.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {
    List<ValidationRule> findByActiveTrue();
    List<ValidationRule> findByActiveTrueOrderByPriorityAsc();
    List<ValidationRule> findByFailurePattern(String failurePattern);
    List<ValidationRule> findByFieldName(String fieldName);
    Optional<ValidationRule> findByFailurePatternAndActive(String failurePattern, Boolean active);
}
