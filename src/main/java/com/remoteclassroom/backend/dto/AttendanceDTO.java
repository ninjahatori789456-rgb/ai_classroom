package com.remoteclassroom.backend.dto;

import java.time.LocalDateTime;

public class AttendanceDTO {
    private String studentName;
    private String studentEmail;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;

    public AttendanceDTO(String studentName, String studentEmail, LocalDateTime joinTime, LocalDateTime leaveTime) {
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.joinTime = joinTime;
        this.leaveTime = leaveTime;
    }

    // Getters and Setters
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }

    public LocalDateTime getLeaveTime() { return leaveTime; }
    public void setLeaveTime(LocalDateTime leaveTime) { this.leaveTime = leaveTime; }
}
