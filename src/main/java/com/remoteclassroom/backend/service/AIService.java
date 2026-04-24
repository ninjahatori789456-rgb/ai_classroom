package com.remoteclassroom.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    private final RestTemplate restTemplate = createRestTemplate();

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20000); // 20 seconds
        factory.setReadTimeout(90000);    // 90 seconds (increased from 30s)
        return new RestTemplate(factory);
    }

    private static final String FALLBACK_QUIZ = """
[
  {
    "question": "An error occurred generating an adaptive quiz. This is a fallback dummy question.",
    "options": ["A", "B", "C", "D"],
    "correctAnswer": "A",
    "topic": "Fallback"
  }
]
""";

    // =========================
    // 📘 EXPLANATION API (DOUBTS)
    // =========================
    public String getAnswer(String question, String language) {
        String requestId = UUID.randomUUID().toString();
        log.info("[{}] Requesting AI explanation for doubt.", requestId);

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
""".formatted(language, question);

        try {
            // Because this is natural text, we do NOT run cleanJson! 
            // We want to preserve \n so paragraphs render correctly on the UI.
            return callGeminiCoreWithRetry(prompt, 2, requestId);
        } catch (Exception e) {
            log.error("[{}] Failed to get AI doubt explanation: {}", requestId, e.getMessage(), e);
            return "I'm sorry, I am experiencing a temporary technical glitch connecting to the central mainframe. Please try asking your doubt again in a moment!";
        }
    }

    // =========================
    // 🧠 QUIZ GENERATION (UPGRADED)
    // =========================
    public String generateQuiz(String transcript, String difficulty, String weakTopic) {
        String requestId = UUID.randomUUID().toString();
        log.info("[{}] Generating AI Quiz. Difficulty: {}, WeakTopic: {}", requestId, difficulty, weakTopic);

        String prompt = """
You are an expert teacher.

Generate EXACTLY 10 MCQs in English.

Difficulty: %s
%s

IMPORTANT DISTRIBUTION:
- 4 EASY questions
- 3 MEDIUM questions
- 3 HARD questions

Rules:
- Questions must be clear and concept-based
- EASY → basic definition or direct concept
- MEDIUM → application based
- HARD → tricky or multi-concept
- Avoid repetition

STRICT JSON FORMAT:
[
  {
    "question": "string",
    "options": ["A","B","C","D"],
    "correctAnswer": "A/B/C/D",
    "topic": "short topic",
    "level": "EASY/MEDIUM/HARD"
  }
]

VERY IMPORTANT:
- correctAnswer MUST be exactly one of: A, B, C, D
- It must match options
- DO NOT return full text answer
- DO NOT return explanation
- Return ONLY JSON

Lecture:
%s
""".formatted(
        difficulty,
        (weakTopic != null && !weakTopic.isBlank()
                ? "Focus MORE on weak topic: " + weakTopic
                : ""),
        transcript
);

        try {
            String rawResponse = callGeminiCoreWithRetry(prompt, 2, requestId);
            String cleaned = cleanJson(rawResponse, requestId);
            if (cleaned == null || !cleaned.trim().startsWith("[")) {
                log.warn("[{}] Cleaned response is not a valid JSON array. Using FALLBACK quiz.", requestId);
                return FALLBACK_QUIZ;
            }
            log.info("[{}] Successfully generated AI quiz.", requestId);
            return cleaned;
        } catch (Exception e) {
            log.error("[{}] AI Quiz generation failed completely: {}", requestId, e.getMessage(), e);
            log.warn("[{}] Using FALLBACK quiz.", requestId);
            return FALLBACK_QUIZ;
        }
    }

    // =========================
    // 🔥 CORE GEMINI API CALL WITH RETRY
    // =========================
    private String callGeminiCoreWithRetry(String prompt, int maxRetries, String requestId) throws Exception {
        int attempt = 0;
        long backoff = 1000;
        
        while (true) {
            try {
                return callGeminiCore(prompt);
            } catch (Exception e) {
                attempt++;
                log.error("[{}] Gemini API call failed (attempt {}/{}). Reason: {}", requestId, attempt, maxRetries + 1, e.getMessage());
                if (attempt > maxRetries) {
                    throw e;
                }
                Thread.sleep(backoff);
                backoff *= 2; // exponential backoff
            }
        }
    }

    // =========================
    // 🔥 CORE GEMINI API CALL
    // =========================
    private String callGeminiCore(String prompt) throws Exception {

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                GEMINI_URL + apiKey,
                HttpMethod.POST,
                entity,
                Map.class
        );

        return extractText(response);
    }

    // =========================
    // 🔥 EXTRACT TEXT
    // =========================
    private String extractText(ResponseEntity<Map> response) {

        if (response.getBody() == null) return "[]";

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");

        if (candidates == null || candidates.isEmpty()) return "[]";

        Map<String, Object> first = candidates.get(0);

        Map<String, Object> content =
                (Map<String, Object>) first.get("content");

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

        if (parts == null || parts.isEmpty()) return "[]";

        return parts.get(0).get("text").toString();
    }

    // =========================
    // 🧹 CLEAN JSON (ONLY FOR QUIZZES)
    // =========================
    private String cleanJson(String text, String requestId) {
        if (text == null) {
            log.warn("[{}] Response text is null. Using FALLBACK quiz.", requestId);
            return FALLBACK_QUIZ;
        }
        
        try {
            int startIndex = text.indexOf('[');
            int endIndex = text.lastIndexOf(']');
            
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                return text.substring(startIndex, endIndex + 1);
            }
            log.warn("[{}] Invalid JSON format from AI (missing brackets). Using FALLBACK quiz.", requestId);
            return FALLBACK_QUIZ;
        } catch (Exception e) {
            log.error("[{}] Failed to clean JSON: {}. Using FALLBACK quiz.", requestId, e.getMessage());
            return FALLBACK_QUIZ;
        }
    }
}