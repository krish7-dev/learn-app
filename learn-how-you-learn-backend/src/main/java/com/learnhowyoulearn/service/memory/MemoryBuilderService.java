package com.learnhowyoulearn.service.memory;

import com.learnhowyoulearn.dto.context.LearningContext;
import com.learnhowyoulearn.dto.context.MemoryContext;
import com.learnhowyoulearn.entity.LearningProfile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class MemoryBuilderService {

    public MemoryContext build(LearningContext context) {
        LearningProfile profile = context.getLearningProfile();

        return MemoryContext.builder()
                .courseTitle(context.getCourse() != null ? context.getCourse().getTitle() : null)
                .lectureTitle(context.getLecture() != null ? context.getLecture().getTitle() : null)
                .rawContent(context.getLecture() != null ? context.getLecture().getRawContent() : null)
                .preferredStyles(profile != null ? profile.getPreferredExplanationStyles() : Collections.emptyList())
                .struggles(profile != null ? profile.getStruggles() : Collections.emptyList())
                .tonePreference(profile != null ? profile.getTonePreference() : null)
                .learningGoals(profile != null ? profile.getLearningGoals() : Collections.emptyList())
                .activeWeakAreas(orEmpty(context.getActiveWeakAreaDescriptions()))
                .recentConfusions(orEmpty(context.getRecentConfusionNotes()))
                .existingTopics(orEmpty(context.getExistingTopicNames()))
                .recentChatMessages(orEmpty(context.getRecentChatMessages()))
                .memorySummary(context.getLatestMemorySummary())
                .lectureNotesText(context.getLectureNotesText())
                .build();
    }

    private List<String> orEmpty(List<String> list) {
        return list != null ? list : Collections.emptyList();
    }
}
