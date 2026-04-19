package com.remoteclassroom.backend.repository;

import com.remoteclassroom.backend.model.Batch;
import com.remoteclassroom.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    Optional<Batch> findByBatchCode(String batchCode);
    Optional<Batch> findByBatchCodeIgnoreCase(String batchCode);
    List<Batch> findByTeacher(User teacher);
}
