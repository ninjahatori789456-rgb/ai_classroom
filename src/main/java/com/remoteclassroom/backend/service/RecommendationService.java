package com.remoteclassroom.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.remoteclassroom.backend.dashboard.dto.response.ProgressTrendResponse;
import com.remoteclassroom.backend.dashboard.dto.response.RecommendationResponse;
import com.remoteclassroom.backend.model.QuizAttempt;
import com.remoteclassroom.backend.model.StudentTopicMastery;
import com.remoteclassroom.backend.repository.QuizAttemptRepository;
import com.remoteclassroom.backend.repository.StudentTopicMasteryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final StudentTopicMasteryRepository masteryRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public List<RecommendationResponse> getRecommendations(Long studentId) {

        List<StudentTopicMastery> masteries =
                masteryRepository.findByStudent_IdOrderByMasteryLevelAsc(studentId);

        List<RecommendationResponse> recommendations = new ArrayList<>();

        if (masteries == null || masteries.isEmpty()) {
            return recommendations;
        }

        // 🔥 PRIORITY BASED RECOMMENDATION
        for (StudentTopicMastery mastery : masteries) {

            double level = mastery.getMasteryLevel();

            if (level < 40) {
                recommendations.add(RecommendationResponse.builder()
                        .type("RETAKE_QUIZ_STRONG")
                        .focus(mastery.getTopicName())
                        .build());
            } else if (level < 70) {
                recommendations.add(RecommendationResponse.builder()
                        .type("REVISE_TOPIC")
                        .focus(mastery.getTopicName())
                        .build());
            }
        }

        // 🔥 IF ALL GOOD → challenge mode
        if (recommendations.isEmpty()) {
            StudentTopicMastery strongest = masteries.get(masteries.size() - 1);

            recommendations.add(RecommendationResponse.builder()
                    .type("TAKE_ADVANCED_QUIZ")
                    .focus(strongest.getTopicName())
                    .build());
        }

        return recommendations;
    }

    public List<ProgressTrendResponse> getOverallTrend(Long studentId) {

        List<QuizAttempt> attempts =
                quizAttemptRepository.findByStudent_Id(studentId);

        if (attempts == null || attempts.isEmpty()) {
            return List.of();
        }

        return attempts.stream()
                // 🔥 FIX: sort by actual time
                .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt))
                .skip(Math.max(0, attempts.size() - 10))
                .map(attempt -> ProgressTrendResponse.builder()
                        .date(attempt.getAttemptedAt() != null
                                ? attempt.getAttemptedAt().toString()
                                : "N/A")
                        .score(attempt.getScore())
                        .build())
                .collect(Collectors.toList());
    }
}