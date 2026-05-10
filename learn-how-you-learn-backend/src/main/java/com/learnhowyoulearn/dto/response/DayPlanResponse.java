package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DayPlanResponse {

    private String date;
    private List<StudyTimelineItemResponse> fullPlan;
    private List<StudyTimelineItemResponse> mediumPlan;
    private List<StudyTimelineItemResponse> minimumPlan;
    private int totalMinutesFull;
    private int totalMinutesMinimum;
    private boolean hasMissedItems;
}
