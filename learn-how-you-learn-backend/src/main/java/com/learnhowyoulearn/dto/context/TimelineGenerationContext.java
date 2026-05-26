package com.learnhowyoulearn.dto.context;

import com.learnhowyoulearn.entity.LearningProfile;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.RevisionItem;
import com.learnhowyoulearn.entity.WeakArea;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TimelineGenerationContext {

    private LearningTarget target;
    private List<Lecture> lectures;
    private List<RevisionItem> pendingRevisions;
    private List<WeakArea> activeWeakAreas;
    private LearningProfile learningProfile;
    /** lectureId → nearest named tree parent (e.g. "Arrays", "Sorting") */
    private Map<Long, String> lectureGroupMap;
}
