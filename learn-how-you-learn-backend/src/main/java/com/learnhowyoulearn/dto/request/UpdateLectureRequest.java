package com.learnhowyoulearn.dto.request;

import com.learnhowyoulearn.entity.Difficulty;
import com.learnhowyoulearn.entity.LectureStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateLectureRequest {

    @Size(max = 255, message = "Title must be under 255 characters")
    private String title;

    private String moduleName;

    private String sourceName;

    private Integer sourceOrder;

    @Size(max = 200000, message = "Content too large (max 200,000 characters).")
    private String rawContent;

    private LectureStatus status;

    private Difficulty difficulty;
}
