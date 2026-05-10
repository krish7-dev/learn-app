package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRevisionRequest {
    @NotBlank
    @Pattern(regexp = "DONE|SKIPPED", message = "Status must be DONE or SKIPPED")
    private String status;
}
