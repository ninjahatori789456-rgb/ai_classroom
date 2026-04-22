package com.remoteclassroom.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remoteclassroom.backend.model.Video;

public interface VideoRepository extends JpaRepository<Video, Long> {

    // ✅ Get videos uploaded by teacher
    List<Video> findByTeacherEmail(String email);

    List<Video> findByBatchId(Long batchId);
    
    List<Video> findByBatchIn(java.util.Collection<com.remoteclassroom.backend.model.Batch> batches);
}
