package com.learnhowyoulearn.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeachBackResponse {
    private int score;

    @JsonProperty("understood_correctly")
    private List<String> understoodCorrectly;

    private List<String> gaps;
    private List<String> misconceptions;
    private String feedback;

    @JsonProperty("suggested_next")
    private String suggestedNext;
}
