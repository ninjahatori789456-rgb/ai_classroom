package com.remoteclassroom.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    private final RestTemplate restTemplate;

    public AIService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String getAnswer(String question, String language) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is missing");
            return "⚠️ Service temporarily unavailable. Please try again later.";
        }

        if (question == null || question.trim().isEmpty()) {
            return "Please provide a valid doubt question.";
        }

        String prompt = """
You are an expert, encouraging AI teacher.

A student has asked the following doubt. Please explain it clearly in %s.

Guidelines for your response:
1. Structure: Use markdown format. Use bolding to highlight keywords. Draw clear bullet points to make it extremely readable.
2. Clarity: Avoid overly complex jargon. Keep sentences concise.
3. Examples: Always provide exactly one simple real-world or code example to solidify the concept.
4. Tone: Be friendly, professional, and directly address the student.

Question from Student:
%s
""".formatted(language != null ? language : "English", question);

        try {
            return callGeminiCore(prompt);
        } catch (Exception e) {
            log.error("Doubt API Error after retries: {}", e.getMessage());
            return "⚠️ Server busy. Please try again in a few seconds.";
        }
    }

    public String generateQuiz(String transcript, String difficulty, String weakTopic) {
        if (transcript == null || transcript.isBlank()) {
            log.warn("Empty transcript provided for quiz generation");
            return getFallbackQuiz("No transcript available to generate quiz.");
        }

        String prompt = """
You are an expert teacher.

Generate EXACTLY 10 MCQs in English.

Difficulty: %s
%s

Rules:
- Questions must be clear and concept-based
- Avoid random questions

STRICT JSON FORMAT:
[
  {
    "question": "string",
    "options": ["A","B","C","D"],
    "correctAnswer": "exact match",
    "topic": "short topic"
  }
]

Return ONLY JSON.

Lecture:
%s
""".formatted(
                difficulty,
                (weakTopic != null && !weakTopic.isBlank() ? "Focus MORE on weak topic: " + weakTopic : ""),
                transcript
        );

        try {
            String rawResponse = callGeminiCore(prompt);
            return cleanJson(rawResponse);
        } catch (Exception e) {
            log.error("Quiz generation failed after retries: {}", e.getMessage());
            return getFallbackQuiz("An error occurred generating an adaptive quiz.");
        }
    }

    private String callGeminiCore(String prompt) throws Exception {
        int maxRetries = 2;
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                log.info("Calling Gemini API (Attempt {}/{})", attempt + 1, maxRetries + 1);
                
                Map<String, Object> part = new HashMap<>();
                part.put("text", prompt);

                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(part));

                Map<String, Object> request = new HashMap<>();
                request.put("contents", List.of(content));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        GEMINI_URL + apiKey,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                String text = extractText(response);
                if (text == null || text.equals("[]")) {
                    throw new RuntimeException("Empty response from AI");
                }
                return text;

            } catch (Exception e) {
                attempt++;
                lastException = e;
                log.warn("Gemini API attempt {} failed: {}", attempt, e.getMessage());
                if (attempt <= maxRetries) {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
        }
        throw lastException;
    }

    private String extractText(ResponseEntity<Map> response) {
        if (response.getBody() == null) return "[]";
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
        if (candidates == null || candidates.isEmpty()) return "[]";
        Map<String, Object> first = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) first.get("content");
        if (content == null) return "[]";
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "[]";
        return parts.get(0).get("text").toString();
    }

    private String cleanJson(String text) {
        if (text == null) return "[]";
        return text
                .replace("```json", "")
                .replace("```", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim();
    }

    private String getFallbackQuiz(String message) {
        return """
[
  {
    "question": "%s",
    "options": ["A", "B", "C", "D"],
    "correctAnswer": "A",
    "topic": "Fallback"
  }
]
""".formatted(message);
    }
}