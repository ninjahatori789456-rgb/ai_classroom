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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }

    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
}
