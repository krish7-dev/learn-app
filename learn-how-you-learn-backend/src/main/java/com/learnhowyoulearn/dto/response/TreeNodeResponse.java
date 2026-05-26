package com.learnhowyoulearn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class TreeNodeResponse {

    private Long id;
    private String label;
    private String nodeType;
    private String status;
    private Long topicId;
    private Long lectureId;
    private int position;
    private double progressPercent;
    private int weakAreaCount;
    private int revisionDueCount;
    private int linkedLectureCount;
    private List<TreeNodeResponse> children;
}
