package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TopicSummaryResponse {
    private Long id;
    private String name;
    private String category;
    private String difficulty;
    private int masteryScore;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
