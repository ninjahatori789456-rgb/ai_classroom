package com.remoteclassroom.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.remoteclassroom.backend.model.Quiz;
import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.QuizRepository;
import com.remoteclassroom.backend.repository.VideoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public com.remoteclassroom.backend.dto.QuizDTO generateAndSaveQuiz(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        String transcript = video.getTranscript();
        if (transcript == null || transcript.isBlank()) {
            // Mock transcript if needed as per requirement
            transcript = "This is a lecture about " + video.getTitle();
        }

        String questionsJson = aiService.generateQuiz(transcript, "MEDIUM", null);
        int questionCount = 0;
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(questionsJson);
            if (root.isArray()) {
                questionCount = root.size();
            }
        } catch (Exception e) {
            questionCount = 1; // Fallback for dummy question
        }

        Quiz quiz = new Quiz();
        quiz.setVideo(video);
        quiz.setTeacher(video.getTeacher());
        quiz.setBatch(video.getBatch());
        quiz.setQuestionsJson(questionsJson);
        quiz.setDifficulty("MEDIUM");
        quiz.setTotalQuestions(questionCount);

        Quiz savedQuiz = quizRepository.save(quiz);
        return new com.remoteclassroom.backend.dto.QuizDTO(
                savedQuiz.getId(), video.getId(), video.getBatch().getId(),
                savedQuiz.getDifficulty(), parseJson(savedQuiz.getQuestionsJson()),
                savedQuiz.getTotalQuestions(), savedQuiz.getCreatedAt()
        );
    }

    public Optional<com.remoteclassroom.backend.dto.QuizDTO> getQuizByVideo(Long videoId) {
        return quizRepository.findTopByVideo_IdOrderByCreatedAtDesc(videoId)
                .map(q -> new com.remoteclassroom.backend.dto.QuizDTO(
                        q.getId(), q.getVideo().getId(), q.getBatch().getId(),
                        q.getDifficulty(), parseJson(q.getQuestionsJson()),
                        q.getTotalQuestions(), q.getCreatedAt()
                ));
    }

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
            return json; // Fallback to raw string if parsing fails
        }
    }

    public Quiz saveQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }
}