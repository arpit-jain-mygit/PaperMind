package com.example.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Bean
    public OpenAiService openAiService() {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            throw new IllegalStateException(
                "OPENAI_API_KEY environment variable is not set. " +
                "Please set: export OPENAI_API_KEY='sk-your-key'"
            );
        }
        return new OpenAiService(openaiApiKey);
    }
}
