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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private S3Service s3Service;

    public com.remoteclassroom.backend.dto.VideoDTO saveVideo(String title, String url, String email, Long batchId, String transcript) {
        log.info("Saving video: {} for batch: {} by teacher: {}", title, batchId, email);

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
        log.info("Video saved with ID: {}", savedVideo.getId());

        // 🔥 Trigger AI Transcription in background
        transcriptionService.transcribeVideoAsync(savedVideo);

        return mapToDTO(savedVideo);
    }

    public List<com.remoteclassroom.backend.dto.VideoDTO> getVideosByBatch(Long batchId) {
        return videoRepository.findByBatchId(batchId).stream()
                .filter(v -> v.getBatch() != null) // Filter out bad data
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<com.remoteclassroom.backend.dto.VideoDTO> getAllVideosDTO() {
        return videoRepository.findAll().stream()
                .filter(v -> v.getBatch() != null) // Filter out bad data
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    public List<com.remoteclassroom.backend.dto.VideoDTO> getMyVideos(String email) {
        return videoRepository.findByTeacher_Email(email).stream()
                .filter(v -> v.getBatch() != null) // Filter out bad data
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.remoteclassroom.backend.dto.VideoDTO mapToDTO(Video v) {
        return new com.remoteclassroom.backend.dto.VideoDTO(
                v.getId(),
                v.getTitle(),
                s3Service.generatePlaybackUrl(v.getUrl()),
                v.getTeacher() != null ? v.getTeacher().getName() : "Unknown",
                v.getBatch() != null ? v.getBatch().getId() : null,
                v.getUploadedAt(),
                v.getTranscript()
        );
    }

    public Video getById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }
}