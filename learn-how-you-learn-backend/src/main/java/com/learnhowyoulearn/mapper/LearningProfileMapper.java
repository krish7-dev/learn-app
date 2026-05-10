package com.learnhowyoulearn.mapper;

import com.learnhowyoulearn.dto.response.LearningProfileResponse;
import com.learnhowyoulearn.entity.LearningProfile;
import org.springframework.stereotype.Component;

@Component
public class LearningProfileMapper {

    public LearningProfileResponse toResponse(LearningProfile profile) {
        return LearningProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .preferredExplanationStyles(profile.getPreferredExplanationStyles())
                .struggles(profile.getStruggles())
                .tonePreference(profile.getTonePreference())
                .learningGoals(profile.getLearningGoals())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
