package com.learnhowyoulearn.dto.context;

import com.learnhowyoulearn.entity.LearningProfile;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.Course;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LearningContext {
    private Course course;
    private Lecture lecture;
    private LearningProfile learningProfile;
    private List<String> activeWeakAreaDescriptions;
    private List<String> recentConfusionNotes;
    private List<String> existingTopicNames;
    private List<String> recentChatMessages;
    private String latestMemorySummary;
    private String lectureNotesText;
}
