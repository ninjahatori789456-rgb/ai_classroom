package com.remoteclassroom.backend.controller;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.User;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.UserRepository;
import com.remoteclassroom.backend.repository.VideoRepository;
import com.remoteclassroom.backend.service.AdaptiveQuizService;

@RestController
@RequestMapping("/api/adaptive")
public class AdaptiveQuizController {

    @Autowired
    private AdaptiveQuizService adaptiveService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/quiz/{videoId}")
    public ResponseEntity<?> generateQuiz(
            @PathVariable Long videoId,
            Authentication authentication) {

        try {

            String email = authentication.getName();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            int attempts = adaptiveService.getAttemptCount(user.getId(), videoId);

            // 🔥 SAME LOGIC (no change)
            if (attempts < 3) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Adaptive quiz locked. Complete 3 attempts first.",
                        "data", null
                ));
            }

            Quiz quiz = adaptiveService.generateAdaptiveQuiz(user.getId(), video);

            List<Map<String, Object>> questions;

            try {
                questions = mapper.readValue(
                        quiz.getQuestionsJson(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } catch (Exception e) {
                questions = List.of();
            }

            // 🔒 remove answers
            questions.forEach(q -> q.remove("correctAnswer"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "id", quiz.getId(),
                            "difficulty", quiz.getDifficulty(),
                            "questions", questions
                    )
            ));

        } catch (Exception e) {

            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to generate adaptive quiz",
                    "data", null
            ));
        }
    }
}