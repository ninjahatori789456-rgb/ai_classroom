package com.remoteclassroom.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remoteclassroom.backend.model.User;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.UserRepository;
import com.remoteclassroom.backend.repository.VideoRepository;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Video saveVideo(String title, String url, String email, String transcript) {

        log.info("Starting video save for title: '{}', teacher: {}", title, email);

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Failed to save video. User not found for email: {}", email);
                    return new RuntimeException("User not found");
                });

        Video video = new Video();
        video.setTitle(title);
        video.setUrl(url);
        video.setTeacher(teacher);
        video.setTranscript(transcript);
        video.setUploadedAt(LocalDateTime.now());

        Video savedVideo = videoRepository.save(video);
        log.info("Successfully saved video. ID: {}, Title: '{}'", savedVideo.getId(), savedVideo.getTitle());

        return savedVideo;
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    public List<Video> getMyVideos(String email) {
        return videoRepository.findByTeacher_Email(email);
    }

    // 🔥 NEW
    public Video getById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }
}