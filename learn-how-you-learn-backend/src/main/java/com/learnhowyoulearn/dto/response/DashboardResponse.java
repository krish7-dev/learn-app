package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardResponse {
    private LectureSummaryResponse continueLecture;
    private List<RevisionItemResponse> revisionDue;
    private List<TopicSummaryResponse> weakTopics;
    private long totalLectures;
    private long completedLectures;
    private long totalTopics;
    private long masteredTopics;
}
