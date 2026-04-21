package com.remoteclassroom.backend.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

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
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    // =========================
    // ✅ DOUBT API (STABLE)
    // =========================
    public String getAnswer(String question, String language) {

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Gemini API key missing");
            return "AI service not configured.";
        }

        if (question == null || question.isBlank()) {
            return "Please ask a valid question.";
        }

        String prompt = """
You are a helpful AI teacher.

Explain clearly in %s.
Give short explanation + 1 example.

Question:
%s
""".formatted(language != null ? language : "English", question);

        try {
            String response = callGeminiWithRetry(prompt);

            if (response == null || response.isBlank()) {
                return "⚠️ AI returned empty response. Try again.";
            }

            return response;

        } catch (Exception e) {
            log.error("AI doubt failed: {}", e.getMessage());
            return "⚠️ AI busy. Try again.";
        }
    }

    // =========================
    // ✅ QUIZ API (STABLE)
    // =========================
    public String generateQuiz(String transcript, String difficulty, String weakTopic) {

        if (transcript == null || transcript.isBlank()) {
            log.warn("Transcript empty → fallback quiz");
            return fallbackQuiz();
        }

        String prompt = """
Generate 10 MCQs in JSON.

Difficulty: %s

Format:
[
 { "question":"", "options":["A","B","C","D"], "correctAnswer":"", "topic":"" }
]

Lecture:
%s
""".formatted(difficulty, transcript);

        try {
            String response = callGeminiWithRetry(prompt);

            if (response == null || response.isBlank()) {
                log.warn("Empty AI quiz response → fallback");
                return fallbackQuiz();
            }

            return clean(response);

        } catch (Exception e) {
            log.error("Quiz generation failed: {}", e.getMessage());
            return fallbackQuiz();
        }
    }

    // =========================
    // 🔥 RETRY LOGIC (CRITICAL FIX)
    // =========================
    private String callGeminiWithRetry(String prompt) throws Exception {

        int attempts = 0;
        int maxAttempts = 2;

        while (attempts < maxAttempts) {
            try {
                return callGemini(prompt);
            } catch (RestClientException e) {
                attempts++;
                log.warn("Gemini retry {} due to error: {}", attempts, e.getMessage());

                if (attempts >= maxAttempts) {
                    throw e;
                }

                Thread.sleep(1000); // small delay
            }
        }

        return null;
    }

    // =========================
    // 🔥 CORE API CALL
    // =========================
    private String callGemini(String prompt) throws Exception {

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                GEMINI_URL + apiKey,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Gemini");
        }

        return extract(response);
    }

    // =========================
    // 🔥 SAFE EXTRACT (CRASH FIX)
    // =========================
    private String extract(ResponseEntity<Map> res) {
        try {
            var body = res.getBody();

            if (body == null || !body.containsKey("candidates")) {
                log.error("Invalid Gemini response structure");
                return "";
            }

            var candidates = (List<Map>) body.get("candidates");

            if (candidates.isEmpty()) return "";

            var content = (Map) candidates.get(0).get("content");
            var parts = (List<Map>) content.get("parts");

            if (parts.isEmpty()) return "";

            return parts.get(0).get("text").toString();

        } catch (Exception e) {
            log.error("Failed to extract Gemini response: {}", e.getMessage());
            return "";
        }
    }

    // =========================
    // 🔥 CLEAN JSON
    // =========================
    private String clean(String text) {
        return text.replace("```json", "")
                .replace("```", "")
                .trim();
    }

    // =========================
    // 🔥 FALLBACK QUIZ
    // =========================
    private String fallbackQuiz() {
        return """
[
 { "question":"Fallback question", "options":["A","B","C","D"], "correctAnswer":"A", "topic":"Fallback" }
]
""";
    }
}