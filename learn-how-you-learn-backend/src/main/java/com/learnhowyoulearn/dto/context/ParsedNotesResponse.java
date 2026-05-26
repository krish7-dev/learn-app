package com.learnhowyoulearn.dto.context;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias("fullCleanNotes")
    private String fullCleanNotes;

    @JsonProperty("simple_explanation")
    @JsonAlias("simpleExplanation")
    private String simpleExplanation;

    @JsonProperty("practical_usage")
    @JsonAlias("practicalUsage")
    private String practicalUsage;

    private JsonNode examples;

    @JsonProperty("mistakes_to_avoid")
    @JsonAlias("mistakesToAvoid")
    private JsonNode mistakesToAvoid;

    @JsonProperty("edge_cases")
    @JsonAlias("edgeCases")
    private JsonNode edgeCases;

    @JsonProperty("revision_notes")
    @JsonAlias("revisionNotes")
    private String revisionNotes;

    @JsonProperty("interview_questions")
    @JsonAlias("interviewQuestions")
    private JsonNode interviewQuestions;

    private JsonNode flashcards;

    @JsonProperty("practice_questions")
    @JsonAlias("practiceQuestions")
    private JsonNode practiceQuestions;

    @JsonProperty("weak_area_checks")
    @JsonAlias("weakAreaChecks")
    private JsonNode weakAreaChecks;

    @JsonProperty("extracted_topics")
    @JsonAlias("extractedTopics")
    private List<ExtractedTopic> extractedTopics;

    @JsonProperty("suggested_module")
    @JsonAlias("suggestedModule")
    private String suggestedModule;
}
