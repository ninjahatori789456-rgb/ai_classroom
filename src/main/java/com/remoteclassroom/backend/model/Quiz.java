package com.remoteclassroom.backend.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz", indexes = {
        @Index(name = "idx_quiz_video_id", columnList = "video_id"),
        @Index(name = "idx_quiz_batch_id", columnList = "batch_id"),
        @Index(name = "idx_quiz_created_at", columnList = "created_at")
})
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    private String difficulty;

    // ✅ FIXED LOB ISSUE
    @Column(columnDefinition = "TEXT")
    private String questionsJson;

    private int totalQuestions;
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // GETTERS
    public Long getId() { return id; }
    public String getDifficulty() { return difficulty; }
    public String getQuestionsJson() { return questionsJson; }
    public int getTotalQuestions() { return totalQuestions; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public Video getVideo() { return video; }
    public User getTeacher() { return teacher; }
    public Batch getBatch() { return batch; }

    // SETTERS
    public void setVideo(Video video) { this.video = video; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setQuestionsJson(String questionsJson) { this.questionsJson = questionsJson; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}