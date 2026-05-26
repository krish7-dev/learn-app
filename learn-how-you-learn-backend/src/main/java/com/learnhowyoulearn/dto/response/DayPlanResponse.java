package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DayPlanResponse {

    private String date;
    private List<StudyTimelineItemResponse> items;
    private int totalMinutes;
    private boolean hasMissedItems;
}
