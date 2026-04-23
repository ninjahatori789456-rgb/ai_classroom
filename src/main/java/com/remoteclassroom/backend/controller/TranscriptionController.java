package com.remoteclassroom.backend.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.VideoRepository;
import com.remoteclassroom.backend.service.TranscriptionService;

@RestController
@RequestMapping("/api/transcribe")
public class TranscriptionController {

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private VideoRepository videoRepository;

    @PostMapping("/{videoId}")
    public ResponseEntity<?> transcribe(
            @PathVariable Long videoId,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force
    ) {

        try {
            System.out.println("🔥 API: Validating Transcribe Request for Video " + videoId);

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            // ✅ FIXED CACHE CHECK (production safe)
            String transcript = video.getTranscript();

            if (!force && transcript != null && transcript.length() > 100) {
                System.out.println("🔥 API: Transcript already exists (VALID CACHE HIT)");
                return ResponseEntity.ok(java.util.Map.of(
                        "status", "cached",
                        "message", "Transcript already exists",
                        "data", transcript
                ));
            }

            // FIRE AND FORGET (ASYNC)
            transcriptionService.transcribeVideoAsync(video.getId());

            return ResponseEntity.accepted().body(java.util.Map.of(
                    "status", "processing",
                    "message", "Video is being transcribed in the background."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }
}