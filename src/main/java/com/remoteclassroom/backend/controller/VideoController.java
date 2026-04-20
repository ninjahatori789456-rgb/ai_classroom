package com.remoteclassroom.backend.controller;

import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.service.S3Service;
import com.remoteclassroom.backend.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private S3Service s3Service;

    @GetMapping("/play/{videoId}")
    public ResponseEntity<Map<String, String>> getPlayUrl(@PathVariable Long videoId) {
        Video video = videoService.getById(videoId);
        String playUrl = s3Service.generatePlaybackUrl(video.getUrl());
        return ResponseEntity.ok(Map.of(
            "url", playUrl,
            "title", video.getTitle()
        ));
    }

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

        return ResponseEntity.ok(
                videoService.saveVideo(
                        request.getTitle(),
                        request.getVideoUrl(),
                        authentication.getName(),
                        request.getBatchId(),
                        request.getTranscript()
                )
        );
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

    // 🔥 NEW DOWNLOAD API
    @GetMapping("/download/{videoId}")
    public ResponseEntity<Map<String, String>> downloadVideo(@PathVariable Long videoId) {

        Video video = videoService.getById(videoId);

        String downloadUrl = s3Service.generateDownloadUrl(
                video.getUrl(),
                video.getTitle()
        );

        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }
}