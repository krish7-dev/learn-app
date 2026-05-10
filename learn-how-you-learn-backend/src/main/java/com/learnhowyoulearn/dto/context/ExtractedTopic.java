package com.learnhowyoulearn.dto.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedTopic {
    private String name;
    private String importance;

    @JsonProperty("coverage_level")
    private String coverageLevel;

    private String evidence;
}
