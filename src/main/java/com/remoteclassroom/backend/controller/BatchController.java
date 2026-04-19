package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.model.Batch;
import com.remoteclassroom.backend.model.Enrollment;
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
    public ResponseEntity<com.remoteclassroom.backend.dto.BatchDTO> createBatch(@RequestBody Map<String, String> request, Authentication authentication) {
        String name = request.get("name");
        String subject = request.get("subject");
        String email = authentication.getName();
        return ResponseEntity.ok(batchService.createBatch(name, subject, email));
    }

    @PostMapping("/join")
    public ResponseEntity<com.remoteclassroom.backend.dto.BatchDTO> joinBatch(@RequestBody Map<String, String> request, Authentication authentication) {
        String batchCode = request.get("code");
        String email = authentication.getName();
        return ResponseEntity.ok(batchService.joinBatch(batchCode, email));
    }

    @GetMapping("/my")
    public ResponseEntity<List<com.remoteclassroom.backend.dto.BatchDTO>> getMyBatches(Authentication authentication) {
        String email = authentication.getName();
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (isTeacher) {
            return ResponseEntity.ok(batchService.getTeacherBatches(email));
        } else {
            return ResponseEntity.ok(batchService.getStudentBatches(email));
        }
    }
}
