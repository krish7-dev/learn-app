package com.learnhowyoulearn.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TopicDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String difficulty;
    private int masteryScore;
    private String status;
    private JsonNode combinedNotes;
    private JsonNode examples;
    private JsonNode edgeCases;
    private JsonNode mistakesToAvoid;
    private JsonNode patterns;
    private String revisionNotes;
    private List<LectureSummaryResponse> linkedLectures;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
