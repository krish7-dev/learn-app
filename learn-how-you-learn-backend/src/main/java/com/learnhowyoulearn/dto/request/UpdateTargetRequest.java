package com.learnhowyoulearn.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateTargetRequest {

    private String title;
    private String description;
    private LocalDate targetDate;
    private Integer dailyMinutes;
    private Integer weeklyMinutes;
    private String priority;
    private String status;
}
