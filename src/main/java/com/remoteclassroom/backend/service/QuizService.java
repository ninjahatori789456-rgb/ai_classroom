package com.remoteclassroom.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ✅ GENERATE QUIZ (SAFE + PRODUCTION)
    @Transactional
    public com.remoteclassroom.backend.dto.QuizDTO generateAndSaveQuiz(Long videoId) {

        if (videoId == null) {
            throw new IllegalArgumentException("Video ID cannot be null");
        }

        // 🔒 prevent duplicate generation (NO CRASH)
        if (activeGenerations.putIfAbsent(videoId, true) != null) {
            log.warn("Quiz generation already running for video {}", videoId);

            // return existing instead of crashing
            return getQuizByVideo(videoId)
                    .orElseThrow(() -> new RuntimeException("Quiz is being generated. Try again."));
        }

        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            // ✅ RETURN EXISTING QUIZ
            List<Quiz> existing = quizRepository.findByVideo_IdOrderByCreatedAtDesc(videoId);
            if (!existing.isEmpty()) {
                return mapToDTO(existing.get(0));
            }

            String transcript = video.getTranscript();
            if (transcript == null || transcript.isBlank()) {
                transcript = "Lecture about " + video.getTitle();
            }

            String questionsJson;

            try {
                questionsJson = aiService.generateQuiz(transcript, "MEDIUM", null);
            } catch (Exception e) {
                log.error("AI generation failed: {}", e.getMessage());

                // 🔥 fallback (VERY IMPORTANT)
                questionsJson = "[]";
            }

            if (questionsJson == null || questionsJson.isBlank()) {
                log.warn("AI returned empty quiz for video {}", videoId);
                questionsJson = "[]";
            }

            int questionCount = countQuestions(questionsJson);

            Quiz quiz = new Quiz();
            quiz.setVideo(video);
            quiz.setTeacher(video.getTeacher());
            quiz.setBatch(video.getBatch());
            quiz.setQuestionsJson(questionsJson);
            quiz.setDifficulty("MEDIUM");
            quiz.setTotalQuestions(questionCount);

            Quiz saved = quizRepository.save(quiz);

            return mapToDTO(saved);

        } finally {
            activeGenerations.remove(videoId);
        }
    }

    // ✅ FETCH QUIZ
    @Transactional(readOnly = true)
    public Optional<com.remoteclassroom.backend.dto.QuizDTO> getQuizByVideo(Long videoId) {

        List<Quiz> quizzes = quizRepository.findByVideo_IdOrderByCreatedAtDesc(videoId);

        if (quizzes.isEmpty()) return Optional.empty();

        return Optional.of(mapToDTO(quizzes.get(0)));
    }

    // ✅ FIXED BATCH FETCH
    @Transactional(readOnly = true)
    public List<com.remoteclassroom.backend.dto.QuizDTO> getQuizzesByBatch(Long batchId) {

        return quizRepository.findByBatchId(batchId) // ✅ FIX HERE
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ✅ SAFE JSON PARSE
    private Object parseJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return java.util.Collections.emptyList();
            }

            var node = objectMapper.readTree(json);

            if (!node.isArray()) {
                log.warn("Quiz JSON is not array");
                return java.util.Collections.emptyList();
            }

            return node;

        } catch (Exception e) {
            log.error("JSON parse failed: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    private int countQuestions(String json) {
        try {
            var node = objectMapper.readTree(json);
            return node.isArray() ? node.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ✅ SAFE DTO
    private com.remoteclassroom.backend.dto.QuizDTO mapToDTO(Quiz q) {

        Long videoId = q.getVideo() != null ? q.getVideo().getId() : null;
        Long batchId = q.getBatch() != null ? q.getBatch().getId() : null;

        return new com.remoteclassroom.backend.dto.QuizDTO(
                q.getId(),
                videoId,
                batchId,
                q.getDifficulty(),
                parseJson(q.getQuestionsJson()),
                q.getTotalQuestions(),
                q.getCreatedAt()
        );
    }
}