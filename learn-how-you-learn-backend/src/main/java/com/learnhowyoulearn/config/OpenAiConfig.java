package com.learnhowyoulearn.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.enabled}")
    private boolean enabled;

    @Value("${openai.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${openai.base-url}")
    private String baseUrl;
}
