package com.remoteclassroom.backend.repository;

import com.remoteclassroom.backend.model.Batch;
import com.remoteclassroom.backend.model.Enrollment;
import com.remoteclassroom.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);
    Optional<Enrollment> findByStudentAndBatch(User student, Batch batch);
}
