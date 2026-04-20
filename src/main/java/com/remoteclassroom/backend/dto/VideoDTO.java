package com.remoteclassroom.backend.dto;

import java.time.LocalDateTime;

public class VideoDTO {
    private Long id;
    private String title;
    private String url;
    private String teacherName;
    private Long batchId;
    private String batchName;
    private String batchCode;
    private LocalDateTime uploadedAt;
    private String transcript;

    public VideoDTO() {}

    public VideoDTO(Long id, String title, String url, String teacherName, Long batchId, String batchName, String batchCode, LocalDateTime uploadedAt, String transcript) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.teacherName = teacherName;
        this.batchId = batchId;
        this.batchName = batchName;
        this.batchCode = batchCode;
        this.uploadedAt = uploadedAt;
        this.transcript = transcript;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
}
