package com.remoteclassroom.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VideoSaveRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Video URL is required")
    private String videoUrl;
    
    @NotNull(message = "Batch ID is required")
    private Long batchId;
    
    private String transcript;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
}
