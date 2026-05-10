package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TutorChatResponse {
    private Long messageId;
    private String role;
    private String message;
    private LocalDateTime createdAt;
}
