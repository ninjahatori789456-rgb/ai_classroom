package com.remoteclassroom.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.QuizRepository;
import com.remoteclassroom.backend.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private final Map<Long, Boolean> activeGenerations = new ConcurrentHashMap<>();

    @Transactional
    @CacheEvict(value = "quiz", key = "#videoId")
    public com.remoteclassroom.backend.dto.QuizDTO generateAndSaveQuiz(Long videoId) {
        if (videoId == null) {
            throw new IllegalArgumentException("Video ID cannot be null");
        }

        if (activeGenerations.putIfAbsent(videoId, true) != null) {
            log.warn("Quiz generation already in progress for video: {}", videoId);
            throw new RuntimeException("Quiz generation already in progress for this video.");
        }

        long startTime = System.currentTimeMillis();
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            String transcript = video.getTranscript();
            if (transcript == null || transcript.isBlank()) {
                log.warn("Transcript missing for video: {}. Using fallback.", videoId);
                transcript = "This is a lecture about " + video.getTitle();
            }

            log.info("Starting AI quiz generation for video: {}", videoId);
            String questionsJson = aiService.generateQuiz(transcript, "MEDIUM", null);
            
            int questionCount = 0;
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(questionsJson);
                if (root.isArray()) {
                    questionCount = root.size();
                }
            } catch (Exception e) {
                log.error("Failed to parse AI JSON for video {}: {}", videoId, e.getMessage());
                questionCount = 1;
            }

            Quiz quiz = new Quiz();
            quiz.setVideo(video);
            quiz.setTeacher(video.getTeacher());
            quiz.setBatch(video.getBatch());
            quiz.setQuestionsJson(questionsJson);
            quiz.setDifficulty("MEDIUM");
            quiz.setTotalQuestions(questionCount);

            Quiz savedQuiz = quizRepository.save(quiz);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Quiz generated and saved in {}ms for video: {}", duration, videoId);

            return new com.remoteclassroom.backend.dto.QuizDTO(
                    savedQuiz.getId(), video.getId(), video.getBatch().getId(),
                    savedQuiz.getDifficulty(), parseJson(savedQuiz.getQuestionsJson()),
                    savedQuiz.getTotalQuestions(), savedQuiz.getCreatedAt()
            );
        } finally {
            activeGenerations.remove(videoId);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "quiz", key = "#videoId")
    public Optional<com.remoteclassroom.backend.dto.QuizDTO> getQuizByVideo(Long videoId) {
        log.info("Fetching latest quiz for video: {}", videoId);
        List<Quiz> quizzes = quizRepository.findByVideo_IdOrderByCreatedAtDesc(videoId);
        if (quizzes.isEmpty()) {
            return Optional.empty();
        }
        Quiz q = quizzes.get(0);
        return Optional.of(new com.remoteclassroom.backend.dto.QuizDTO(
                q.getId(), q.getVideo().getId(), q.getBatch().getId(),
                q.getDifficulty(), parseJson(q.getQuestionsJson()),
                q.getTotalQuestions(), q.getCreatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public List<com.remoteclassroom.backend.dto.QuizDTO> getQuizzesByBatch(Long batchId) {
        return quizRepository.findByBatchId(batchId).stream()
                .map(q -> new com.remoteclassroom.backend.dto.QuizDTO(
                        q.getId(), q.getVideo().getId(), q.getBatch().getId(),
                        q.getDifficulty(), parseJson(q.getQuestionsJson()),
                        q.getTotalQuestions(), q.getCreatedAt()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("JSON parse failed, returning raw string: {}", e.getMessage());
            return json;
        }
    }

    @Transactional
    public Quiz saveQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }
}