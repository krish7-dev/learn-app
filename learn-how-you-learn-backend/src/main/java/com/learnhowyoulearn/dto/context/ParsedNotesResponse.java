package com.learnhowyoulearn.dto.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedNotesResponse {
    private String title;

    @JsonProperty("full_clean_notes")
    private String fullCleanNotes;

    @JsonProperty("simple_explanation")
    private String simpleExplanation;

    @JsonProperty("practical_usage")
    private String practicalUsage;

    private JsonNode examples;

    @JsonProperty("mistakes_to_avoid")
    private JsonNode mistakesToAvoid;

    @JsonProperty("edge_cases")
    private JsonNode edgeCases;

    @JsonProperty("revision_notes")
    private String revisionNotes;

    @JsonProperty("interview_questions")
    private JsonNode interviewQuestions;

    private JsonNode flashcards;

    @JsonProperty("practice_questions")
    private JsonNode practiceQuestions;

    @JsonProperty("weak_area_checks")
    private JsonNode weakAreaChecks;

    @JsonProperty("extracted_topics")
    private List<ExtractedTopic> extractedTopics;
}
