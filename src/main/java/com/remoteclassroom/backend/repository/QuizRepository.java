package com.remoteclassroom.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.Video;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
    @EntityGraph(attributePaths = {"video", "batch"})
    List<Quiz> findByVideo_IdOrderByCreatedAtDesc(Long videoId);
    
    @EntityGraph(attributePaths = {"video", "batch"})
    List<Quiz> findByBatchId(Long batchId);
}