package com.learnhowyoulearn.service.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiRequest {
    private String systemPrompt;
    private String userPrompt;
    private String model;
    private Double temperature;
    private String purpose;
    private Integer timeoutSeconds;
    private Integer maxTokens;
}
