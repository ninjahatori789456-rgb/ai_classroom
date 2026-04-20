package com.remoteclassroom.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.remoteclassroom.backend.model.Batch;
import com.remoteclassroom.backend.model.ClassParticipant;
import com.remoteclassroom.backend.model.LiveClass;
import com.remoteclassroom.backend.model.User;
import com.remoteclassroom.backend.repository.BatchRepository;
import com.remoteclassroom.backend.repository.ClassParticipantRepository;
import com.remoteclassroom.backend.repository.LiveClassRepository;
import com.remoteclassroom.backend.repository.UserRepository;

@Service
public class LiveClassService {

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private ClassParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BatchRepository batchRepository;

    public com.remoteclassroom.backend.dto.LiveClassDTO createClass(String title, String teacherEmail, Long batchId) {

        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // 🔒 SECURITY CHECK: Ensure teacher owns this batch
        if (!batch.getTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Unauthorized: You are not the teacher of this batch");
        }

        LiveClass lc = new LiveClass();
        lc.setTitle(title);
        lc.setTeacher(teacher);
        lc.setBatch(batch);
        lc.setLive(false);
        lc.setMeetingId(UUID.randomUUID().toString());

        LiveClass saved = liveClassRepository.save(lc);
        return mapToDTO(saved);
    }

    public com.remoteclassroom.backend.dto.LiveClassDTO startClass(Long classId) {
        LiveClass lc = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        lc.setLive(true);
        lc.setStartTime(LocalDateTime.now());

        LiveClass saved = liveClassRepository.save(lc);
        return mapToDTO(saved);
    }

    public com.remoteclassroom.backend.dto.LiveClassDTO endClass(Long classId) {
        LiveClass lc = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        lc.setLive(false);
        LiveClass saved = liveClassRepository.save(lc);
        return mapToDTO(saved);
    }

    public List<LiveClass> getLiveClassesByBatch(Long batchId) {
        return liveClassRepository.findByBatchId(batchId);
    }

    public com.remoteclassroom.backend.dto.LiveClassDTO getLiveStatus(Long batchId) {
        return liveClassRepository.findFirstByBatchIdAndIsLiveTrue(batchId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    private com.remoteclassroom.backend.dto.LiveClassDTO mapToDTO(LiveClass lc) {
        return new com.remoteclassroom.backend.dto.LiveClassDTO(
                lc.getId(), lc.getTitle(), lc.isLive(), lc.getMeetingId(), lc.getStartTime()
        );
    }

    // Existing methods (can be kept or updated as needed)
    public ClassParticipant joinClass(Long classId, String studentEmail) {
        LiveClass lc = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (!lc.isLive()) {
            throw new RuntimeException("Class is not live");
        }

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean alreadyJoined = participantRepository.existsByLiveClassIdAndStudentEmail(classId, studentEmail);
        if (alreadyJoined) {
            return participantRepository.findByLiveClassIdAndStudentEmail(classId, studentEmail).get();
        }

        ClassParticipant cp = new ClassParticipant();
        cp.setLiveClass(lc);
        cp.setStudent(student);
        cp.setJoinedAt(LocalDateTime.now());

        return participantRepository.save(cp);
    }

    public ClassParticipant leaveClass(Long classId, String studentEmail) {
        ClassParticipant cp = participantRepository
                .findByLiveClassIdAndStudentEmail(classId, studentEmail)
                .orElseThrow(() -> new RuntimeException("Not joined"));

        if (cp.getLeftAt() != null) {
            return cp;
        }

        cp.setLeftAt(LocalDateTime.now());
        cp.calculateDuration();

        return participantRepository.save(cp);
    }
}