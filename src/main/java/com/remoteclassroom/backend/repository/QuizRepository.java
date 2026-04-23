package com.remoteclassroom.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.Video;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByVideoOrderByIdDesc(Video video);

    // 🔥 NEW: oldest first
    List<Quiz> findByVideoOrderByIdAsc(Video video);

    List<Quiz> findByBatch_Id(Long batchId);
}