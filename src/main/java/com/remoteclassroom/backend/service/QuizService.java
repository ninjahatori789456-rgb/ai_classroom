package com.remoteclassroom.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remoteclassroom.backend.dto.QuizDTO;
import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.QuizRepository;
import com.remoteclassroom.backend.repository.VideoRepository;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AIService aiService;

    public QuizDTO generateAndSaveQuiz(Long videoId) {
        Quiz quiz = getOrGenerateQuiz(videoId, null);
        return mapToDTO(quiz);
    }

    public Optional<QuizDTO> getQuizByVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        List<Quiz> quizzes = quizRepository.findByVideoOrderByIdDesc(video);
        if (quizzes != null && !quizzes.isEmpty()) {
            return Optional.of(mapToDTO(quizzes.get(0)));
        }
        return Optional.empty();
    }

    public List<QuizDTO> getQuizzesByBatch(Long batchId) {
        return quizRepository.findByBatch_Id(batchId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private QuizDTO mapToDTO(Quiz quiz) {
        Object questions = new java.util.ArrayList<>();
        try {
            questions = new ObjectMapper().readValue(quiz.getQuestionsJson(), Object.class);
        } catch (Exception e) {
            log.error("Failed to parse quiz questions JSON for quiz ID: {}", quiz.getId(), e);
        }
        return new QuizDTO(
                quiz.getId(),
                quiz.getVideo().getId(),
                quiz.getBatch() != null ? quiz.getBatch().getId() : null,
                quiz.getDifficulty(),
                questions,
                quiz.getTotalQuestions(),
                quiz.getCreatedAt()
        );
    }

    @Transactional
    public Quiz getOrGenerateQuiz(Long videoId, String studentEmail) {

        log.info("Request to get or generate quiz for videoId: {}", videoId);

        try {
            // 1️⃣ Get video
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            if (video.getBatch() == null || video.getTeacher() == null) {
                throw new RuntimeException("Video not properly linked to batch/teacher");
            }

            // Check for existing quizzes FIRST
            List<Quiz> existingQuizzes = quizRepository.findByVideoOrderByIdDesc(video);
            if (existingQuizzes != null && !existingQuizzes.isEmpty()) {
                if (existingQuizzes.size() > 1) {
                    log.warn("Multiple quizzes ({}) found for videoId: {}. Using the latest one.", existingQuizzes.size(), videoId);
                } else {
                    log.info("Quiz already exists for videoId: {}", videoId);
                }
                return existingQuizzes.get(0); // Return the latest one (ordered by id desc)
            }

            log.info("Starting new quiz generation for videoId: {}", videoId);

            // 2️⃣ Get transcript
            String transcript = video.getTranscript();

            if (transcript == null || transcript.isBlank()) {
                log.error("Transcript not available for videoId: {}", videoId);
                throw new RuntimeException("Transcript not available");
            }

            // 3️⃣ Adaptive (disabled for now)
            String weakTopic = null;

            // 4️⃣ Generate quiz from AI
            log.info("Calling AI service to generate quiz for videoId: {}", videoId);
            String questionsJson = aiService.generateQuiz(transcript, "MEDIUM", weakTopic);

            if (questionsJson == null || !questionsJson.trim().startsWith("[")) {
                log.warn("Invalid questionsJson returned from AI for videoId: {}, using empty list fallback.", videoId);
                questionsJson = "[]";
            }

            // DOUBLE CHECK BEFORE SAVE in case of race condition
            List<Quiz> concurrentCheck = quizRepository.findByVideoOrderByIdDesc(video);
            if (concurrentCheck != null && !concurrentCheck.isEmpty()) {
                log.warn("Quiz was generated concurrently by another thread for videoId: {}. Returning existing.", videoId);
                return concurrentCheck.get(0);
            }

            // 5️⃣ Save quiz
            Quiz quiz = new Quiz();
            quiz.setVideo(video);
            quiz.setTeacher(video.getTeacher());
            quiz.setBatch(video.getBatch());
            quiz.setQuestionsJson(questionsJson);
            quiz.setDifficulty("MEDIUM");
            quiz.setTotalQuestions(10);

            log.info("Saving generated quiz for videoId: {}", videoId);
            Quiz savedQuiz = quizRepository.save(quiz);
            log.info("Finished quiz generation successfully for videoId: {}", videoId);
            return savedQuiz;

        } catch (Exception e) {
            log.error("Quiz generation failed for videoId: {}. Error: {}", videoId, e.getMessage(), e);
            throw new RuntimeException("Quiz generation failed");
        }
    }
}