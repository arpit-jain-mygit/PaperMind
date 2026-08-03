package com.example.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AIProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderFactory.class);

    @Value("${ai.provider:openai}")
    private String provider;

    @Autowired
    private OpenAIProvider openAIProvider;

    @Autowired
    private GoogleVertexAIProvider vertexAIProvider;

    public AIProvider getProvider() {
        logger.info("Factory: Creating AI provider: {}", provider);

        return switch(provider.toLowerCase()) {
            case "vertex" -> {
                logger.info("Factory: Using Google Vertex AI provider");
                yield vertexAIProvider;
            }
            case "openai" -> {
                logger.info("Factory: Using OpenAI provider");
                yield openAIProvider;
            }
            default -> {
                logger.warn("Factory: Unknown provider '{}', defaulting to OpenAI", provider);
                yield openAIProvider;
            }
        };
    }
}
