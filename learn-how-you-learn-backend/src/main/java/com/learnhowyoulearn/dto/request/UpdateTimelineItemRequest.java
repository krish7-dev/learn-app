package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateTimelineItemRequest {

    @NotBlank
    private String status;

    private LocalDate rescheduledDate;
}
