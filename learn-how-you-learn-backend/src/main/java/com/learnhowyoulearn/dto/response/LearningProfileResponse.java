package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class LearningProfileResponse {
    private Long id;
    private Long userId;
    private List<String> preferredExplanationStyles;
    private List<String> struggles;
    private String tonePreference;
    private List<String> learningGoals;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
