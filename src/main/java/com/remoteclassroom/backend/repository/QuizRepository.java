package com.remoteclassroom.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.Video;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Quiz findByVideo(Video video);
    List<Quiz> findByVideo_IdOrderByCreatedAtDesc(Long videoId);
    List<Quiz> findByBatchId(Long batchId);
}