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

import io.agora.media.RtcTokenBuilder;
import org.springframework.beans.factory.annotation.Value;

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

    @Value("${AGORA_APP_ID}")
    private String appId;

    @Value("${AGORA_APP_CERTIFICATE}")
    private String appCertificate;

    // ================= CREATE =================
    public com.remoteclassroom.backend.dto.LiveClassDTO createClass(String title, String teacherEmail, Long batchId) {

        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // 🔒 SECURITY CHECK
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

    // ================= START =================
    public com.remoteclassroom.backend.dto.LiveClassDTO startClass(Long classId) {
        LiveClass lc = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        lc.setLive(true);
        lc.setStartTime(LocalDateTime.now());

        LiveClass saved = liveClassRepository.save(lc);
        return mapToDTO(saved);
    }

    // ================= END =================
    public com.remoteclassroom.backend.dto.LiveClassDTO endClass(Long classId) {
        LiveClass lc = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        lc.setLive(false);
        LiveClass saved = liveClassRepository.save(lc);

        // 🚀 AUTO-LEAVE
        List<Attendance> activeAttendance = attendanceRepository.findByLiveClass(lc);
        for (Attendance a : activeAttendance) {
            if (a.getLeaveTime() == null) {
                a.setLeaveTime(LocalDateTime.now());

                java.time.Duration duration = java.time.Duration.between(a.getJoinTime(), a.getLeaveTime());
                if (duration.toMinutes() >= 5) {
                    a.setPresent(true);
                }

                attendanceRepository.save(a);
            }
        }

        return mapToDTO(saved);
    }

    // ================= STATUS =================
    public com.remoteclassroom.backend.dto.LiveClassDTO getLiveStatus(Long batchId) {
        return liveClassRepository.findFirstByBatchIdAndIsLiveTrue(batchId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    // ================= 🔥 FIXED DTO MAPPING =================
    private com.remoteclassroom.backend.dto.LiveClassDTO mapToDTO(LiveClass lc) {

        String teacherName = "Unknown";
        String batchName = "Unknown";

        if (lc.getTeacher() != null) {
            teacherName = lc.getTeacher().getName();
        }

        if (lc.getBatch() != null) {
            batchName = lc.getBatch().getName();
        }

        return new com.remoteclassroom.backend.dto.LiveClassDTO(
                lc.getId(),
                lc.getTitle(),
                lc.isLive(),
                lc.getMeetingId(),
                lc.getStartTime(),
                teacherName,
                batchName
        );
    }

    // ================= JOIN =================
    public void joinClass(Long classId, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!student.getRole().equals("ROLE_STUDENT") && !student.getRole().equals("STUDENT")) {
            throw new RuntimeException("Only students can join");
        }

        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (!liveClass.isLive()) {
            throw new RuntimeException("Class is not live");
        }

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
        attendance.setPresent(false);

        attendanceRepository.save(attendance);
    }

    // ================= LEAVE =================
    public void leaveClass(Long classId, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Attendance attendance = attendanceRepository
                .findByLiveClassAndStudent(liveClass, student)
                .orElse(null);

        if (attendance == null) return;

        attendance.setLeaveTime(LocalDateTime.now());

        java.time.Duration duration = java.time.Duration.between(attendance.getJoinTime(), attendance.getLeaveTime());
        if (duration.toMinutes() >= 5) {
            attendance.setPresent(true);
        }

        attendanceRepository.save(attendance);
    }

    // ================= ATTENDANCE =================
    public List<com.remoteclassroom.backend.dto.AttendanceDTO> getAttendance(Long classId, String teacherEmail) {
        LiveClass liveClass = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (!liveClass.getTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        return attendanceRepository.findByLiveClass(liveClass).stream()
                .map(a -> new com.remoteclassroom.backend.dto.AttendanceDTO(
                        a.getStudent() != null ? a.getStudent().getName() : "Unknown",
                        a.getStudent() != null ? a.getStudent().getEmail() : "Unknown",
                        a.getJoinTime(),
                        a.getLeaveTime()
                ))
                .toList();
    }

    // ================= GET =================
    public LiveClass getById(Long classId) {
        return liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
    }

    // ================= TOKEN =================
    public String getAgoraToken(Long classId, int uid) {
        LiveClass lc = liveClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (appId == null || appId.isEmpty() || appCertificate == null || appCertificate.isEmpty()) {
            throw new RuntimeException("Agora config missing");
        }

        String channelName = lc.getMeetingId();

        int expirationTimeInSeconds = 3600;
        int timestamp = (int) (System.currentTimeMillis() / 1000 + expirationTimeInSeconds);

        RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();

        return tokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                channelName,
                uid,
                RtcTokenBuilder.Role.Role_Publisher,
                timestamp
        );
    }
}