package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, Object> request) {
        Number videoIdNum = (Number) request.get("videoId");
        if (videoIdNum == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "videoId is required", "status", 400));
        }
        Long videoId = videoIdNum.longValue();
        return ResponseEntity.ok(quizService.generateAndSaveQuiz(videoId));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<?> getQuizByVideo(@PathVariable Long videoId, Authentication authentication) {
        if (videoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "videoId is required", "status", 400));
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        return quizService.getQuizByVideo(videoId)
                .map(quiz -> {
                    if (isStudent) {
                        quiz.setQuestions(scrubAnswers(quiz.getQuestions()));
                    }
                    return ResponseEntity.ok(quiz);
                })
                .orElse(ResponseEntity.status(404).body((com.remoteclassroom.backend.dto.QuizDTO) null)); // Will be handled by client or simple 404
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<java.util.List<com.remoteclassroom.backend.dto.QuizDTO>> getQuizzesByBatch(@PathVariable Long batchId, Authentication authentication) {
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        java.util.List<com.remoteclassroom.backend.dto.QuizDTO> quizzes = quizService.getQuizzesByBatch(batchId);

        if (isStudent) {
            quizzes.forEach(q -> q.setQuestions(scrubAnswers(q.getQuestions())));
        }

        return ResponseEntity.ok(quizzes);
    }

    private Object scrubAnswers(Object questions) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.valueToTree(questions);
            if (root.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : root) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("correctAnswer");
                }
            }
            return root;
        } catch (Exception e) {
            return questions;
        }
    }
}