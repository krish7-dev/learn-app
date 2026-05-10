package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeekPlanResponse {

    private LearningTargetResponse target;
    private List<DayPlanResponse> days;
}
