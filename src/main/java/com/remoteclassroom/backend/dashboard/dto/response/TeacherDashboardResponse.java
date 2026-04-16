package com.remoteclassroom.backend.dashboard.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardResponse {

    private long totalClassesCreated;
    private long totalStudents;
    private double averageAttendancePerClass;
    private double averageStudentScore;

    private List<String> mostCommonWeakTopics;

    private List<TeacherRecentClassResponse> recentClasses;
    private List<TeacherRecentQuizStatsResponse> recentQuizStats;
}