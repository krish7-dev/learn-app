package com.learnhowyoulearn.client.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OpenAiRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer maxTokens;

    @Getter
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
