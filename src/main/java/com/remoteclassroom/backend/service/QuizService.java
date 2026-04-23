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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ================= GENERATE =================
    public QuizDTO generateAndSaveQuiz(Long videoId) {
        Quiz quiz = getOrGenerateQuiz(videoId, null);
        return mapToDTO(quiz);
    }

    // ================= GET BY VIDEO =================
    @Transactional(readOnly = true)
    public Optional<QuizDTO> getQuizByVideo(Long videoId) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        List<Quiz> quizzes = quizRepository.findByVideoOrderByIdDesc(video);

        if (quizzes == null || quizzes.isEmpty()) {
            log.warn("No quiz found for videoId: {}", videoId);
            return Optional.empty();
        }

        // pick latest valid quiz
        Quiz latestQuiz = quizzes.stream()
                .filter(q -> q.getQuestionsJson() != null && !q.getQuestionsJson().isBlank())
                .findFirst()
                .orElse(quizzes.get(0));

        return Optional.of(mapToDTO(latestQuiz));
    }

    // ================= GET BY BATCH =================
    @Transactional(readOnly = true)
    public List<QuizDTO> getQuizzesByBatch(Long batchId) {
        return quizRepository.findByBatch_Id(batchId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ================= DTO MAPPING =================
    private QuizDTO mapToDTO(Quiz quiz) {

        Object questions = new java.util.ArrayList<>();

        try {
            String json = quiz.getQuestionsJson();

            if (json != null && !json.isBlank()) {

                json = json.trim();

                if (!json.startsWith("[") || !json.endsWith("]")) {
                    log.warn("Invalid JSON format for quiz ID: {}", quiz.getId());
                    json = "[]";
                }

                questions = objectMapper.readValue(json, Object.class);
            }

        } catch (Exception e) {
            log.error("JSON PARSE FAILED for quiz ID: {}", quiz.getId(), e);
            questions = new java.util.ArrayList<>();
        }

        Long videoId = null;
        Long batchId = null;

        try {
            videoId = (quiz.getVideo() != null) ? quiz.getVideo().getId() : null;
        } catch (Exception e) {
            log.warn("Video lazy load failed for quiz {}", quiz.getId());
        }

        try {
            batchId = (quiz.getBatch() != null) ? quiz.getBatch().getId() : null;
        } catch (Exception e) {
            log.warn("Batch lazy load failed for quiz {}", quiz.getId());
        }

        return new QuizDTO(
                quiz.getId(),
                videoId,
                batchId,
                quiz.getDifficulty(),
                questions,
                quiz.getTotalQuestions(),
                quiz.getCreatedAt()
        );
    }

    // ================= GENERATE CORE =================
    @Transactional
    public Quiz getOrGenerateQuiz(Long videoId, String studentEmail) {

        log.info("Generating NEW quiz for videoId: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (video.getBatch() == null) {
            throw new RuntimeException("Video batch missing");
        }

        if (video.getTeacher() == null) {
            throw new RuntimeException("Video teacher missing");
        }

        String transcript = video.getTranscript();

        if (transcript == null || transcript.isBlank()) {
            throw new RuntimeException("Transcript not available");
        }

        String questionsJson = aiService.generateQuiz(transcript, "MEDIUM", null);

        if (questionsJson == null || !questionsJson.trim().startsWith("[")) {
            questionsJson = "[]";
        }

        Quiz quiz = new Quiz();
        quiz.setVideo(video);
        quiz.setTeacher(video.getTeacher());
        quiz.setBatch(video.getBatch());
        quiz.setQuestionsJson(questionsJson);
        quiz.setDifficulty("MEDIUM");
        quiz.setTotalQuestions(10);

        return quizRepository.save(quiz);
    }
}