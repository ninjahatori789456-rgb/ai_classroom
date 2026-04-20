package com.remoteclassroom.backend.service;

import com.remoteclassroom.backend.dto.BatchDTO;
import com.remoteclassroom.backend.model.Batch;
import com.remoteclassroom.backend.model.Enrollment;
import com.remoteclassroom.backend.model.User;
import com.remoteclassroom.backend.repository.BatchRepository;
import com.remoteclassroom.backend.repository.EnrollmentRepository;
import com.remoteclassroom.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ CREATE
    public BatchDTO createBatch(String name, String subject, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        String code = generateBatchCode(subject);
        Batch batch = new Batch(name, subject, code, teacher);

        return map(batchRepository.save(batch));
    }

    // ✅ JOIN
    public BatchDTO joinBatch(String code, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Batch batch = batchRepository.findByBatchCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new RuntimeException("Invalid code"));

        if (enrollmentRepository.findByStudentAndBatch(student, batch).isPresent()) {
            throw new RuntimeException("Already joined");
        }

        enrollmentRepository.save(new Enrollment(student, batch));
        return map(batch);
    }

    // ✅ STUDENT
    public List<BatchDTO> getStudentBatches(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return enrollmentRepository.findByStudent(student)
                .stream()
                .map(e -> map(e.getBatch()))
                .collect(Collectors.toList());
    }

    // ✅ TEACHER
    public List<BatchDTO> getTeacherBatches(String email) {
        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        return batchRepository.findByTeacher(teacher)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    // ✅🔥 EXPLORE COURSES (MAIN FIX)
    public List<BatchDTO> getAllBatches() {
        return batchRepository.findAll()
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    // ✅ DTO MAPPING
    private BatchDTO map(Batch b) {
        return new BatchDTO(
                b.getId(),
                b.getName(),
                b.getSubject(),
                b.getBatchCode(),
                b.getTeacher() != null ? b.getTeacher().getName() : "Unknown"
        );
    }

    private String generateBatchCode(String subject) {
        String prefix = subject.substring(0, Math.min(subject.length(), 3)).toUpperCase();
        return prefix + "-" + (new Random().nextInt(900) + 100);
    }
}