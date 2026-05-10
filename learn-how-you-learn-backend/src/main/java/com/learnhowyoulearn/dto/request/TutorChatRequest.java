package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorChatRequest {
    @NotBlank
    private String message;
}
