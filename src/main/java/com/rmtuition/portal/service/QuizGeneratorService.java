package com.rmtuition.portal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmtuition.portal.model.QuizQuestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QuizGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(QuizGeneratorService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public List<QuizQuestion> generateQuestions(String classCode, String subject, String topic, int count) {
        if (apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
            logger.warn("Gemini API key is not configured. Falling back to local offline question generator.");
            return generateFallbackQuestions(classCode, subject, topic, count);
        }

        try {
            String prompt = String.format(
                "Generate a JSON array of %d multiple-choice questions for standard/class: %s, subject: %s, and topic: %s. " +
                "The JSON must strictly conform to this schema: [ { \"questionText\": \"string\", \"options\": [\"option A\", \"option B\", \"option C\", \"option D\"], \"correctOptionIndex\": 0, \"explanation\": \"string\" } ]. " +
                "Output ONLY the raw JSON array. Do not include any markdown format tags like ```json or comments.",
                count, classCode, subject, topic
            );

            // Construct payload
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));
            Map<String, Object> contents = Map.of("contents", List.of(parts));
            Map<String, Object> generationConfig = Map.of("responseMimeType", "application/json");
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(parts),
                    "generationConfig", generationConfig
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String requestUrl = apiUrl + "?key=" + apiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String jsonText = root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText()
                        .trim();

                logger.info("Successfully fetched questions from Gemini AI. Parsing JSON...");
                return objectMapper.readValue(jsonText, new TypeReference<List<QuizQuestion>>() {});
            }
        } catch (Exception e) {
            logger.error("Failed to generate questions using Gemini API: {}. Falling back to offline generator.", e.getMessage());
        }

        return generateFallbackQuestions(classCode, subject, topic, count);
    }

    private List<QuizQuestion> generateFallbackQuestions(String classCode, String subject, String topic, int count) {
        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            List<String> options = List.of(
                    "Concept option A (Recommended)",
                    "Concept option B",
                    "Concept option C",
                    "Concept option D"
            );
            questions.add(QuizQuestion.builder()
                    .questionText(String.format("Mock Question %d: What is the core mechanism of %s in %s (Standard %s)?", i, topic, subject, classCode))
                    .options(options)
                    .correctOptionIndex(0)
                    .explanation(String.format("Option A is the correct answer because it represents the fundamental principle of %s.", topic))
                    .build());
        }
        return questions;
    }
}
