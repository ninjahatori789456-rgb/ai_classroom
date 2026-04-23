package com.remoteclassroom.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.remoteclassroom.backend.model.User;
import com.remoteclassroom.backend.repository.UserRepository;
import com.remoteclassroom.backend.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    // 🔥 GET RECOMMENDATIONS
    @GetMapping
    public ResponseEntity<?> getRecommendations(Authentication authentication) {

        try {
            String email = authentication.getName();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", recommendationService.getRecommendations(user.getId())
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to fetch recommendations",
                    "data", null
            ));
        }
    }

    // 🔥 GET TREND
    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(Authentication authentication) {

        try {
            String email = authentication.getName();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", recommendationService.getOverallTrend(user.getId())
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to fetch trend",
                    "data", null
            ));
        }
    }
}