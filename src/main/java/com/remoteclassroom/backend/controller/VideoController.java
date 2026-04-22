package com.remoteclassroom.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.remoteclassroom.backend.dto.VideoDTO;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.service.S3Service;
import com.remoteclassroom.backend.service.VideoService;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private VideoService videoService;

    // ================= UPLOAD (OPTIONAL - FIXED) =================
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam Long batchId,   // 🔥 FIX REQUIRED
            @RequestParam(value = "transcript", required = false) String transcript,
            Authentication authentication
    ) {
        try {
            String fileUrl = s3Service.uploadFile(file);

            Video savedVideo = videoService.saveVideo(
                    title,
                    fileUrl,
                    authentication.getName(),
                    batchId,   // 🔥 FIX
                    transcript
            );

            return ResponseEntity.ok(savedVideo);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // ================= PRESIGNED =================
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/upload-url")
    public ResponseEntity<?> getUploadUrl(@RequestParam String fileName) {

        String uniqueFileName = "video-" + UUID.randomUUID() + "-" + fileName;

        return ResponseEntity.ok(Map.of(
                "uploadUrl", s3Service.generatePresignedUrl(uniqueFileName),
                "fileUrl", s3Service.getFileUrl(uniqueFileName),
                "fileName", uniqueFileName
        ));
    }

    // ================= SAVE (MAIN FLOW) =================
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/save")
    public ResponseEntity<?> saveVideo(
            @RequestParam String title,
            @RequestParam String url,
            @RequestParam Long batchId,   // 🔥 CRITICAL
            @RequestParam(required = false) String transcript,
            Authentication authentication
    ) {

        Video savedVideo = videoService.saveVideo(
                title,
                url,
                authentication.getName(),
                batchId,
                transcript
        );

        return ResponseEntity.ok(savedVideo);
    }

    // ================= DOWNLOAD =================
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download/{videoId}")
    public ResponseEntity<?> downloadVideo(@PathVariable Long videoId) {

        Video video = videoService.getById(videoId);

        return ResponseEntity.ok(Map.of(
                "url", s3Service.generateDownloadUrl(video.getUrl(), video.getTitle())
        ));
    }

    // ================= LIST =================
    @GetMapping("/all")
    public List<VideoDTO> getAllVideos() {
        return videoService.getAllVideosDTO();
    }

    @GetMapping("/my")
    public List<VideoDTO> getMyVideos(Authentication authentication) {
        return videoService.getMyVideosDTO(authentication.getName());
    }
}