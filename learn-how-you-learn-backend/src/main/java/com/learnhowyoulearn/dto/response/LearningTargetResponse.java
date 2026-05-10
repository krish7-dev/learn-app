package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class LearningTargetResponse {

    private Long id;
    private String title;
    private String description;
    private String targetScope;
    private Long courseId;
    private String moduleName;
    private Long topicId;
    private LocalDate targetDate;
    private Integer dailyMinutes;
    private Integer weeklyMinutes;
    private String priority;
    private String status;
    private double progressPercent;
    private Boolean isOnTrack;
    private int totalLectures;
    private int completedLectures;
    private int daysRemaining;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
