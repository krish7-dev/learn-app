package com.learnhowyoulearn.service.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiResponse {
    private String content;
    private String model;
    private long latencyMs;
    private String rawResponse;
}
