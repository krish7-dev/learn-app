package com.learnhowyoulearn.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateLearningProfileRequest {
    private List<String> preferredExplanationStyles;
    private List<String> struggles;
    private String tonePreference;
    private List<String> learningGoals;
}
