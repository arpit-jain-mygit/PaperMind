package com.example.ai;

import com.example.entity.Student;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Component
public class GoogleVertexAIProvider implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleVertexAIProvider.class);
    private static final String[] AVAILABLE_MODELS = {
        "gemini-3.5-flash",
        "gemini-omni-flash",
        "gemini-2.0-flash",
        "gemini-1.5-pro",
        "gemini-1.5-flash"
    };

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.location:us-central1}")
    private String location;

    @Override
    public String generateSummary(Student student) {
        logger.info("VertexAI: Starting summary generation for student ID: {} ({})", student.getId(), student.getName());

        if (projectId == null || projectId.isEmpty()) {
            logger.error("VertexAI: Project ID not configured");
            throw new IllegalStateException("GCP_PROJECT_ID not configured. Please set GCP_PROJECT_ID environment variable.");
        }

        String prompt = String.format(
            "Very brief (1 sentence) summary: %s, GPA: %.2f",
            student.getName(),
            student.getGpa()
        );

        for (String model : AVAILABLE_MODELS) {
            try {
                logger.info("VertexAI: Trying model: {}", model);
                String summary = callVertexAI(prompt, model);
                logger.info("VertexAI: Success with model: {}", model);
                return summary;
            } catch (Exception e) {
                logger.warn("VertexAI: Model {} failed: {}", model, e.getMessage());
            }
        }

        throw new RuntimeException("All Vertex AI models failed. No available model found.");
    }

    private String callVertexAI(String prompt, String model) throws Exception {
        String accessToken = getAccessToken();
        String endpoint = String.format(
            "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
            location, projectId, location, model
        );

        String requestBody = buildRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        logger.info("VertexAI: Calling {} at {}", model, endpoint);
        logger.debug("VertexAI: Request body: {}", requestBody);

        try {
            String response = restTemplate.postForObject(endpoint, request, String.class);
            logger.debug("VertexAI: Response: {}", response);
            return parseResponse(response);
        } catch (HttpClientErrorException e) {
            logger.error("VertexAI: HTTP Client Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(String.format("HTTP %s: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        } catch (HttpServerErrorException e) {
            logger.error("VertexAI: HTTP Server Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(String.format("HTTP %s: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        }
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refresh();
        return credentials.getAccessToken().getTokenValue();
    }

    private String buildRequestBody(String prompt) {
        return String.format("""
            {
              "contents": [
                {
                  "role": "user",
                  "parts": [
                    {
                      "text": "%s"
                    }
                  ]
                }
              ],
              "generationConfig": {
                "maxOutputTokens": 50,
                "temperature": 0.3
              }
            }
            """, prompt.replace("\"", "\\\""));
    }

    private String parseResponse(String response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();
    }

    @Override
    public String getProviderName() {
        return "Google Vertex AI (Auto-detect model)";
    }
}
