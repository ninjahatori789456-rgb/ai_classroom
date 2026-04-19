package com.remoteclassroom.backend.service;

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

    public com.remoteclassroom.backend.dto.BatchDTO createBatch(String name, String subject, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        String batchCode = generateBatchCode(subject);
        Batch batch = new Batch(name, subject, batchCode, teacher);
        Batch saved = batchRepository.save(batch);
        return new com.remoteclassroom.backend.dto.BatchDTO(
            saved.getId(), saved.getName(), saved.getSubject(), saved.getBatchCode(), saved.getTeacher().getName()
        );
    }

    public com.remoteclassroom.backend.dto.BatchDTO joinBatch(String batchCode, String studentEmail) {
        if (batchCode == null) {
            throw new RuntimeException("Batch code is missing in request");
        }
        
        String trimmedCode = batchCode.trim();
        
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Batch batch = batchRepository.findByBatchCodeIgnoreCase(trimmedCode)
                .orElseThrow(() -> new RuntimeException("Invalid Batch Code"));

        if (enrollmentRepository.findByStudentAndBatch(student, batch).isPresent()) {
            throw new RuntimeException("Already enrolled in this batch");
        }

        Enrollment enrollment = new Enrollment(student, batch);
        enrollmentRepository.save(enrollment);
        
        return new com.remoteclassroom.backend.dto.BatchDTO(
            batch.getId(), batch.getName(), batch.getSubject(), batch.getBatchCode(), batch.getTeacher().getName()
        );
    }

    public List<com.remoteclassroom.backend.dto.BatchDTO> getStudentBatches(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return enrollmentRepository.findByStudent(student)
                .stream()
                .map(enrollment -> {
                    Batch b = enrollment.getBatch();
                    return new com.remoteclassroom.backend.dto.BatchDTO(
                        b.getId(), b.getName(), b.getSubject(), b.getBatchCode(), b.getTeacher().getName()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<com.remoteclassroom.backend.dto.BatchDTO> getTeacherBatches(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        return batchRepository.findByTeacher(teacher)
                .stream()
                .map(b -> new com.remoteclassroom.backend.dto.BatchDTO(
                    b.getId(), b.getName(), b.getSubject(), b.getBatchCode(), b.getTeacher().getName()
                ))
                .collect(Collectors.toList());
    }

    public Batch getBatchById(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));
    }

    private String generateBatchCode(String subject) {
        String prefix = subject.substring(0, Math.min(subject.length(), 3)).toUpperCase();
        int randomNum = new Random().nextInt(900) + 100; // 100 to 999
        return prefix + "-" + randomNum;
    }
}
