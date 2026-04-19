package com.remoteclassroom.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.remoteclassroom.backend.model.Batch;
import com.remoteclassroom.backend.model.User;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.BatchRepository;
import com.remoteclassroom.backend.repository.UserRepository;
import com.remoteclassroom.backend.repository.VideoRepository;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private TranscriptionService transcriptionService;

    public com.remoteclassroom.backend.dto.VideoDTO saveVideo(String title, String url, String email, Long batchId, String transcript) {

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        Video video = new Video();
        video.setTitle(title);
        video.setUrl(url);
        video.setTeacher(teacher);
        video.setBatch(batch);
        video.setTranscript(transcript);
        video.setUploadedAt(LocalDateTime.now());

        Video savedVideo = videoRepository.save(video);

        // 🔥 Trigger AI Transcription in background
        transcriptionService.transcribeVideoAsync(savedVideo);

        return new com.remoteclassroom.backend.dto.VideoDTO(
                savedVideo.getId(), savedVideo.getTitle(), savedVideo.getUrl(),
                teacher.getName(), batch.getId(), savedVideo.getUploadedAt(), savedVideo.getTranscript()
        );
    }

    public List<com.remoteclassroom.backend.dto.VideoDTO> getVideosByBatch(Long batchId) {
        return videoRepository.findByBatchId(batchId).stream()
                .map(v -> new com.remoteclassroom.backend.dto.VideoDTO(
                        v.getId(), v.getTitle(), v.getUrl(),
                        v.getTeacher().getName(), v.getBatch().getId(), v.getUploadedAt(), v.getTranscript()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    public List<Video> getMyVideos(String email) {
        return videoRepository.findByTeacher_Email(email);
    }

    public Video getById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }
}