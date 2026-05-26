package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.context.TimelineGenerationContext;
import com.learnhowyoulearn.entity.LearningProfile;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.RevisionItem;
import com.learnhowyoulearn.entity.WeakArea;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.entity.TreeNode;
import com.learnhowyoulearn.repository.LearningProfileRepository;
import com.learnhowyoulearn.repository.LearningTargetRepository;
import com.learnhowyoulearn.repository.RevisionItemRepository;
import com.learnhowyoulearn.repository.TreeNodeRepository;
import com.learnhowyoulearn.repository.WeakAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimelineContextLoaderService {

    private static final long USER_ID = 1L;

    private final LearningTargetRepository learningTargetRepository;
    private final LearningTargetService learningTargetService;
    private final RevisionItemRepository revisionItemRepository;
    private final WeakAreaRepository weakAreaRepository;
    private final LearningProfileRepository learningProfileRepository;
    private final TreeNodeRepository treeNodeRepository;

    @Transactional(readOnly = true)
    public TimelineGenerationContext loadForTimeline(Long targetId) {
        LearningTarget target = learningTargetRepository.findByIdAndUserId(targetId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        List<Lecture> lectures = learningTargetService.fetchScopedLectures(target);

        LocalDateTime deadline = target.getTargetDate().atTime(23, 59);
        List<RevisionItem> pendingRevisions = revisionItemRepository
                .findByUserIdAndStatusAndDueAtBefore(USER_ID, "PENDING", deadline);

        List<WeakArea> activeWeakAreas = weakAreaRepository
                .findByUserIdAndStatusOrderBySeverityDescCreatedAtDesc(USER_ID, "ACTIVE");

        LearningProfile profile = learningProfileRepository.findByUserId(USER_ID).orElse(null);

        Map<Long, String> lectureGroupMap = buildLectureGroupMap(lectures);

        return TimelineGenerationContext.builder()
                .target(target)
                .lectures(lectures)
                .pendingRevisions(pendingRevisions)
                .activeWeakAreas(activeWeakAreas)
                .learningProfile(profile)
                .lectureGroupMap(lectureGroupMap)
                .build();
    }

    private Map<Long, String> buildLectureGroupMap(List<Lecture> lectures) {
        if (lectures.isEmpty()) return Map.of();

        List<Long> lectureIds = lectures.stream().map(Lecture::getId).collect(Collectors.toList());

        List<TreeNode> shellNodes = treeNodeRepository.findByUserIdAndLectureIdIn(USER_ID, lectureIds);
        if (shellNodes.isEmpty()) return Map.of();

        List<Long> parentIds = shellNodes.stream()
                .map(TreeNode::getParentId)
                .filter(pid -> pid != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> parentLabels = treeNodeRepository.findAllByIdIn(parentIds).stream()
                .collect(Collectors.toMap(TreeNode::getId, TreeNode::getLabel));

        Map<Long, String> result = new HashMap<>();
        for (TreeNode shell : shellNodes) {
            if (shell.getLectureId() == null) continue;
            String group = shell.getParentId() != null
                    ? parentLabels.getOrDefault(shell.getParentId(), null)
                    : null;
            if (group != null) result.put(shell.getLectureId(), group);
        }
        return result;
    }
}
