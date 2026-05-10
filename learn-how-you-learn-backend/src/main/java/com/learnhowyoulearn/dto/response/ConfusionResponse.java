package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConfusionResponse {
    private Long id;
    private String confusionType;
    private String sectionTitle;
    private String note;
    private boolean resolved;
    private LocalDateTime createdAt;
}
