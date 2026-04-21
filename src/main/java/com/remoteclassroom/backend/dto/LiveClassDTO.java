package com.remoteclassroom.backend.dto;

import java.time.LocalDateTime;

public class LiveClassDTO {

    private Long id;
    private String title;
    private boolean isLive;
    private String meetingId;
    private LocalDateTime startTime;

    // ✅ NEW FIELDS
    private String teacherName;
    private String batchName;

    // 🔹 OLD CONSTRUCTOR (DON'T TOUCH)
    public LiveClassDTO(Long id, String title, boolean isLive, String meetingId, LocalDateTime startTime) {
        this.id = id;
        this.title = title;
        this.isLive = isLive;
        this.meetingId = meetingId;
        this.startTime = startTime;
    }

    // 🔥 NEW CONSTRUCTOR (ADD THIS ONLY)
    public LiveClassDTO(Long id, String title, boolean isLive,
                        String meetingId, LocalDateTime startTime,
                        String teacherName, String batchName) {

        this.id = id;
        this.title = title;
        this.isLive = isLive;
        this.meetingId = meetingId;
        this.startTime = startTime;
        this.teacherName = teacherName;
        this.batchName = batchName;
    }

    // getters setters

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public boolean isLive() { return isLive; }
    public String getMeetingId() { return meetingId; }
    public LocalDateTime getStartTime() { return startTime; }

    public String getTeacherName() { return teacherName; }
    public String getBatchName() { return batchName; }

    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
}