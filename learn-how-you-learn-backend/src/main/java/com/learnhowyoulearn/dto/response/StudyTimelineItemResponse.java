package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyTimelineItemResponse {

    private Long id;
    private Long targetId;
    private Long courseId;
    private Long lectureId;
    private Long topicId;
    private String scheduledDate;
    private String itemType;
    private String title;
    private String description;
    private int estimatedMinutes;
    private String planTier;
    private String status;
    private String aiReasoning;
}
