package com.remoteclassroom.backend.dto;

import java.time.LocalDateTime;

public class LiveClassDTO {
    private Long id;
    private String title;
    private boolean isLive;
    private String meetingId;
    private LocalDateTime startTime;

    public LiveClassDTO(Long id, String title, boolean isLive, String meetingId, LocalDateTime startTime) {
        this.id = id;
        this.title = title;
        this.isLive = isLive;
        this.meetingId = meetingId;
        this.startTime = startTime;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public boolean isLive() { return isLive; }
    public String getMeetingId() { return meetingId; }
    public LocalDateTime getStartTime() { return startTime; }
}
