package com.example.ai;

import com.example.entity.Student;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAIProvider implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);

    @Value("${openai.api-key}")
    private String apiKey;

    @Override
    public String generateSummary(Student student) {
        logger.info("OpenAI: Starting summary generation for student ID: {} ({})", student.getId(), student.getName());

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("${OPENAI_API_KEY}")) {
            logger.error("OpenAI: API key not configured");
            throw new IllegalStateException("OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.");
        }

        OpenAiService service = new OpenAiService(apiKey);

        String prompt = String.format(
            "Very brief (1 sentence) summary: %s, GPA: %.2f",
            student.getName(),
            student.getGpa()
        );
        logger.debug("OpenAI: Prompt created");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", prompt));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .maxTokens(50)
                .temperature(0.3)
                .build();

        logger.info("OpenAI: Calling GPT-3.5-turbo API");
        try {
            String summary = service.createChatCompletion(request).getChoices().get(0).getMessage().getContent();
            logger.info("OpenAI: Summary generated successfully");
            return summary;
        } catch (Exception e) {
            logger.error("OpenAI: Error calling API: {}", e.getMessage());
            throw e;
        } finally {
            service.shutdownExecutor();
        }
    }

    @Override
    public String getProviderName() {
        return "OpenAI (GPT-3.5-turbo)";
    }
}
