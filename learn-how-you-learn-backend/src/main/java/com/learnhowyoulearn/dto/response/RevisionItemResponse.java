package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RevisionItemResponse {
    private Long id;
    private Long lectureId;
    private Long topicId;
    private String title;
    private String revisionType;
    private LocalDateTime dueAt;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
}
