package com.learnhowyoulearn.dto.context;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemoryContext {
    private String courseTitle;
    private String lectureTitle;
    private String rawContent;
    private List<String> preferredStyles;
    private List<String> struggles;
    private String tonePreference;
    private List<String> learningGoals;
    private List<String> activeWeakAreas;
    private List<String> recentConfusions;
    private List<String> existingTopics;
    private List<String> recentChatMessages;
    private String memorySummary;
    private String lectureNotesText;
}
