package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateLectureRequest {

    private String moduleName;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be under 255 characters")
    private String title;

    private String sourceName;

    private Integer sourceOrder;

    @Size(max = 50000, message = "Content too large. Please split into multiple lectures.")
    private String rawContent;
}
