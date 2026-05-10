package com.learnhowyoulearn.dto.context;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class ParsedTimeline {

    private List<ParsedDay> days;

    @Getter @Setter @NoArgsConstructor
    public static class ParsedDay {
        private String date;
        private List<ParsedItem> items;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ParsedItem {
        private String itemType;
        private String title;
        private String description;
        private int estimatedMinutes;
        private String planTier;
        private Long lectureId;
        private Long topicId;
        private String aiReasoning;
    }
}
