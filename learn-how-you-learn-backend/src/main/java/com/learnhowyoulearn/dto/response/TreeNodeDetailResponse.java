package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class TreeNodeDetailResponse {

    private Long id;
    private String label;
    private String nodeType;
    private String status;
    private double progressPercent;
    private int weakAreaCount;
    private int revisionDueCount;

    // Topic detail (when topicId set)
    private Long topicId;
    private String topicName;
    private String topicStatus;
    private int masteryScore;
    private LocalDate nextRevisionDue;
    private String weakAreaSeverity;

    // Lecture detail (when lectureId set)
    private Long lectureId;
    private String lectureTitle;
    private String contentStatus;
    private Integer estimatedMinutes;
}
