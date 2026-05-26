package com.learnhowyoulearn.dto.context;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
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

        @JsonDeserialize(using = LenientLongDeserializer.class)
        private Long lectureId;

        @JsonDeserialize(using = LenientLongDeserializer.class)
        private Long topicId;

        private String aiReasoning;
    }

    /** Accepts a JSON number or numeric string; returns null for anything else. */
    public static class LenientLongDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) return null;
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
