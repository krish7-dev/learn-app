package com.learnhowyoulearn.dto.request;

import com.learnhowyoulearn.entity.CourseStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateCourseRequest {

    @Size(max = 255, message = "Title must be under 255 characters")
    private String title;

    private String description;

    private String goal;

    private CourseStatus status;
}
