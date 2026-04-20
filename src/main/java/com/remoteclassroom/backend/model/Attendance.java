package com.remoteclassroom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_class_id")
    private LiveClass liveClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;

    private boolean present;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LiveClass getLiveClass() { return liveClass; }
    public void setLiveClass(LiveClass liveClass) { this.liveClass = liveClass; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }

    public LocalDateTime getLeaveTime() { return leaveTime; }
    public void setLeaveTime(LocalDateTime leaveTime) { this.leaveTime = leaveTime; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
