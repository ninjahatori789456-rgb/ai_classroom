package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.model.LiveClass;
import com.remoteclassroom.backend.service.LiveClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/live")
public class LiveClassController {

    @Autowired
    private LiveClassService liveClassService;

    @PostMapping("/create")
    public ResponseEntity<com.remoteclassroom.backend.dto.LiveClassDTO> createClass(@jakarta.validation.Valid @RequestBody com.remoteclassroom.backend.dto.LiveClassRequest request, Authentication authentication) {
        System.out.println("📡 Received Live Class Create Request - Title: " + request.getTitle() + ", BatchId: " + request.getBatchId());
        String title = request.getTitle();
        Long batchId = request.getBatchId();
        String teacherEmail = authentication.getName();
        return ResponseEntity.ok(liveClassService.createClass(title, teacherEmail, batchId));
    }

    @PostMapping("/start")
    public ResponseEntity<com.remoteclassroom.backend.dto.LiveClassDTO> startClass(@RequestBody Map<String, Long> request) {
        Long classId = request.get("classId");
        return ResponseEntity.ok(liveClassService.startClass(classId));
    }

    @PostMapping("/end")
    public ResponseEntity<com.remoteclassroom.backend.dto.LiveClassDTO> endClass(@RequestBody Map<String, Long> request) {
        Long classId = request.get("classId");
        return ResponseEntity.ok(liveClassService.endClass(classId));
    }

    @GetMapping("/status/{batchId}")
    public ResponseEntity<com.remoteclassroom.backend.dto.LiveClassDTO> getLiveStatus(@PathVariable Long batchId) {
        com.remoteclassroom.backend.dto.LiveClassDTO lc = liveClassService.getLiveStatus(batchId);
        if (lc != null) {
            return ResponseEntity.ok(lc);
        }
        return ResponseEntity.noContent().build();
    }
}