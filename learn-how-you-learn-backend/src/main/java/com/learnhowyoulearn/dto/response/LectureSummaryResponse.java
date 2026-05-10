package com.learnhowyoulearn.dto.response;

import com.learnhowyoulearn.entity.Difficulty;
import com.learnhowyoulearn.entity.LectureStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// No rawContent — safe for list endpoints
@Getter @Builder
public class LectureSummaryResponse {
    private Long id;
    private Long courseId;
    private String moduleName;
    private String title;
    private String sourceName;
    private Integer sourceOrder;
    private LectureStatus status;
    private Difficulty difficulty;
    private boolean notesGenerated;
    private String contentStatus;
    private Integer estimatedMinutes;
    private LocalDateTime lastStudiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
