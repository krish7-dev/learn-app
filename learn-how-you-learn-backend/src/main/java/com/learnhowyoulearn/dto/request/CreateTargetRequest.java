package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateTargetRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private String targetScope;

    private Long courseId;
    private String moduleName;
    private Long topicId;

    @NotNull
    @Future
    private LocalDate targetDate;

    @NotNull
    @Min(15)
    @Max(480)
    private Integer dailyMinutes;

    private Integer weeklyMinutes;

    private String priority;
}
