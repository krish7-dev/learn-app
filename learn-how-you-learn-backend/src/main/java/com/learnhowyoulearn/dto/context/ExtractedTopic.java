package com.learnhowyoulearn.dto.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedTopic {
    private String name;
    private String type;
    private String importance;

    @JsonProperty("coverage_level")
    private String coverageLevel;

    private String summary;
    private String evidence;
    private List<String> prerequisites;

    @JsonProperty("related_topics")
    private List<String> relatedTopics;

    private Integer confidence;

    private String treePath;

    @JsonProperty("tree_path")
    public void setTreePath(Object treePath) {
        if (treePath instanceof String s) {
            this.treePath = s;
        } else if (treePath instanceof List<?> list) {
            this.treePath = list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" > "));
        }
    }
}
