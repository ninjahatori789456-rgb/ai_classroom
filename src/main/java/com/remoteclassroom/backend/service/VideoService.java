package com.remoteclassroom.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remoteclassroom.backend.dto.VideoDTO;
import com.remoteclassroom.backend.model.*;
import com.remoteclassroom.backend.repository.*;

@Service
public class VideoService {

    @Autowired private VideoRepository videoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BatchRepository batchRepository;

    // ================= SAVE =================
    public Video saveVideo(String title, String url, String username,Long batchId, String transcript) {

        User teacher = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        // 🔥 FIX: attach first teacher batch (safe fallback)
        Batch batch = batchRepository.findByTeacher(teacher)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No batch found for teacher"));

        Video video = new Video();
        video.setTitle(title);
        video.setUrl(url);
        video.setTranscript(transcript);
        video.setTeacher(teacher);   // ✅ FIX
        video.setBatch(batch);       // ✅ FIX

        return videoRepository.save(video);
    }

    // ================= GET ALL =================
    @Transactional(readOnly = true)
    public List<VideoDTO> getAllVideosDTO() {
        return videoRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ================= GET MY =================
    @Transactional(readOnly = true)
    public List<VideoDTO> getMyVideosDTO(String username) {
        return videoRepository.findByTeacherEmail(username)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public Video getById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }

    private VideoDTO mapToDTO(Video v) {
        return new VideoDTO(
                v.getId(),
                v.getTitle(),
                v.getUrl(),
                v.getTeacher() != null ? v.getTeacher().getName() : null,
                v.getBatch() != null ? v.getBatch().getId() : null,
                v.getBatch() != null ? v.getBatch().getName() : null,
                v.getBatch() != null ? v.getBatch().getBatchCode() : null,
                v.getUploadedAt(),
                v.getTranscript()
        );
    }
}