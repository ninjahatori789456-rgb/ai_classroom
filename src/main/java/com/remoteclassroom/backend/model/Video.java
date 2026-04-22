package com.remoteclassroom.backend.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String url;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    private LocalDateTime uploadedAt;

    // ✅ FIX: Avoid LOB lazy crash
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "TEXT")
    private String transcript;

    @PrePersist
    public void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    // GETTERS
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public String getTranscript() { return transcript; }
    public User getTeacher() { return teacher; }
    public Batch getBatch() { return batch; }

    // SETTERS
    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
}