package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.dto.DoubtRequest;
import com.remoteclassroom.backend.service.AIService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/doubt")
    public String solveDoubt(@RequestBody DoubtRequest request, org.springframework.security.core.Authentication auth) {
        System.out.println("🔥 DOUBT HIT: " + request.getQuestion());
        System.out.println("👤 USER: " + (auth != null ? auth.getName() : "NO AUTH"));
        return aiService.getAnswer(
                request.getQuestion(),
                request.getLanguage());
    }
}