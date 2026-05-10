package com.learnhowyoulearn.dto.response;

import com.learnhowyoulearn.entity.CourseStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class CourseSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private CourseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
