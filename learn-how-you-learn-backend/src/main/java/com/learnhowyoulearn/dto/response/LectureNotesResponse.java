package com.learnhowyoulearn.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class LectureNotesResponse {
    private Long id;
    private String title;
    private String fullCleanNotes;
    private String simpleExplanation;
    private String practicalUsage;
    private JsonNode examples;
    private JsonNode mistakesToAvoid;
    private JsonNode edgeCases;
    private String revisionNotes;
    private JsonNode interviewQuestions;
    private JsonNode flashcards;
    private JsonNode practiceQuestions;
    private JsonNode weakAreaChecks;
    private String model;
    private String chatAdditions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
