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
import com.remoteclassroom.backend.repository.AttendanceRepository;
import com.remoteclassroom.backend.repository.BatchRepository;
import com.remoteclassroom.backend.repository.ClassParticipantRepository;
import com.remoteclassroom.backend.repository.LiveClassRepository;
import com.remoteclassroom.backend.repository.UserRepository;
import com.remoteclassroom.backend.model.Attendance;

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

    @Autowired
    private AttendanceRepository attendanceRepository;

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

        // 🚀 AUTO-LEAVE: Update all students who haven't left yet
        List<Attendance> activeAttendance = attendanceRepository.findByLiveClass(lc);
        for (Attendance a : activeAttendance) {
            if (a.getLeaveTime() == null) {
                a.setLeaveTime(LocalDateTime.now());
                
                // Calculate presence on auto-leave too
                java.time.Duration duration = java.time.Duration.between(a.getJoinTime(), a.getLeaveTime());
                if (duration.toMinutes() >= 5) {
                    a.setPresent(true);
                }
                
                attendanceRepository.save(a);
            }
        }

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

    public void joinClass(Long classId, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!student.getRole().equals("ROLE_STUDENT") && !student.getRole().equals("STUDENT")) {
            throw new RuntimeException("Only students can join as attendees");
        }

        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (!liveClass.isLive()) {
            throw new RuntimeException("Class is not live");
        }

        // Handle re-join logic
        Attendance existing = attendanceRepository.findByLiveClassAndStudent(liveClass, student).orElse(null);
        if (existing != null) {
            existing.setJoinTime(LocalDateTime.now());
            existing.setLeaveTime(null);
            attendanceRepository.save(existing);
            return;
        }

        Attendance attendance = new Attendance();
        attendance.setLiveClass(liveClass);
        attendance.setStudent(student);
        attendance.setJoinTime(LocalDateTime.now());
        attendance.setPresent(false); // Default to false until duration met

        attendanceRepository.save(attendance);
    }

    public void leaveClass(Long classId, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Attendance attendance = attendanceRepository
                .findByLiveClassAndStudent(liveClass, student)
                .orElse(null);

        if (attendance == null) return; // Silently ignore if no join record exists

        attendance.setLeaveTime(LocalDateTime.now());
        
        // 🎯 Duration Check: Mark present only if stayed >= 5 minutes
        java.time.Duration duration = java.time.Duration.between(attendance.getJoinTime(), attendance.getLeaveTime());
        if (duration.toMinutes() >= 5) {
            attendance.setPresent(true);
        }

        attendanceRepository.save(attendance);
    }

    public List<com.remoteclassroom.backend.dto.AttendanceDTO> getAttendance(Long classId, String teacherEmail) {
        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // 🔒 SECURITY CHECK: Only the teacher of this batch can see attendance
        if (!liveClass.getTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Unauthorized: Only the assigned teacher can view attendance.");
        }

        return attendanceRepository.findByLiveClass(liveClass).stream()
                .map(a -> new com.remoteclassroom.backend.dto.AttendanceDTO(
                        a.getStudent() != null ? a.getStudent().getName() : "Unknown",
                        a.getStudent() != null ? a.getStudent().getEmail() : "Unknown",
                        a.getJoinTime(),
                        a.getLeaveTime()
                ))
                .collect(java.util.stream.Collectors.toList());
    }
}