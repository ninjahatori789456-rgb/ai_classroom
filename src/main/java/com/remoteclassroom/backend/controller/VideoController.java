package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.service.S3Service;
import com.remoteclassroom.backend.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(@RequestBody Map<String, String> request) {
        String originalFilename = request.get("filename");
        String fileName = "video-" + UUID.randomUUID() + "-" + originalFilename;
        String uploadUrl = s3Service.generatePresignedUrl(fileName);
        String finalUrl = s3Service.getFileUrl(fileName);

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl);
        response.put("videoUrl", finalUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<com.remoteclassroom.backend.dto.VideoDTO> saveVideo(
            @jakarta.validation.Valid @RequestBody com.remoteclassroom.backend.dto.VideoSaveRequest request,
            Authentication authentication) {
        String title = request.getTitle();
        String videoUrl = request.getVideoUrl();
        Long batchId = request.getBatchId();
        String transcript = request.getTranscript();
        String email = authentication.getName();

        return ResponseEntity.ok(videoService.saveVideo(title, videoUrl, email, batchId, transcript));
    }

    @GetMapping("/all")
    public ResponseEntity<List<com.remoteclassroom.backend.dto.VideoDTO>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideosDTO());
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<com.remoteclassroom.backend.dto.VideoDTO>> getVideosByBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(videoService.getVideosByBatch(batchId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<com.remoteclassroom.backend.dto.VideoDTO>> getMyVideos(Authentication authentication) {
        String email = authentication.getName();
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("TEACHER"));

        if (isTeacher) {
            return ResponseEntity.ok(videoService.getVideosInTeacherBatches(email));
        } else {
            return ResponseEntity.ok(videoService.getVideosInEnrolledBatches(email));
        }
    }
}