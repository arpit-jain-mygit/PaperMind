package com.example.ai;

import com.example.entity.Student;

public interface AIProvider {
    String generateSummary(Student student);
    String getProviderName();
}
