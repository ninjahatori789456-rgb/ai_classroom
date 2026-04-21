package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @PostMapping("/create")
    public ResponseEntity<?> createBatch(@RequestBody Map<String, String> request, Authentication auth) {
        return ResponseEntity.ok(
                batchService.createBatch(
                        request.get("name"),
                        request.get("subject"),
                        auth.getName()
                )
        );
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinBatch(@RequestBody Map<String, String> request, Authentication auth) {
        return ResponseEntity.ok(
                batchService.joinBatch(
                        request.get("code"),
                        auth.getName()
                )
        );
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBatches(Authentication auth) {
        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (isTeacher) {
            return ResponseEntity.ok(batchService.getTeacherBatches(auth.getName()));
        } else {
            return ResponseEntity.ok(batchService.getStudentBatches(auth.getName()));
        }
    }

    // ✅🔥 THIS IS THE MISSING API (MAIN FIX)
    @GetMapping("/all")
    public ResponseEntity<?> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }
}