package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfusionRequest {

    @NotBlank
    @Pattern(regexp = "THEORY_UNCLEAR|EXAMPLE_UNCLEAR|WHEN_TO_USE_UNCLEAR|CODE_CONFUSING|EDGE_CASE_CONFUSING|LOST_FOCUS",
             message = "Invalid confusion type")
    private String confusionType;

    private String sectionTitle;
    private String note;
}
