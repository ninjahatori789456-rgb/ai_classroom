package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.dto.DoubtRequest;
import com.remoteclassroom.backend.service.AIService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/doubt")
public Map<String, Object> solveDoubt(
        @RequestBody DoubtRequest request,
        Authentication auth) {

    try {
        String raw = aiService.getAnswer(
                request.getQuestion(),
                request.getLanguage());

        // 🔥 CLEAN TEXT (remove \n mess)
        String cleaned = raw
                .replace("\\n", "\n")
                .replace("###", "")
                .trim();

        return Map.of(
                "success", true,
                "data", Map.of(
                        "answer", cleaned
                )
        );

    } catch (Exception e) {
        e.printStackTrace();

        return Map.of(
                "success", false,
                "message", "Failed to get answer",
                "data", null
        );
    }
}
}