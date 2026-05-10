package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.ConfusionRequest;
import com.learnhowyoulearn.dto.response.ConfusionResponse;
import com.learnhowyoulearn.entity.ConfusionLog;
import com.learnhowyoulearn.entity.LearningEvent;
import com.learnhowyoulearn.entity.WeakArea;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.ConfusionLogRepository;
import com.learnhowyoulearn.repository.LearningEventRepository;
import com.learnhowyoulearn.repository.LectureRepository;
import com.learnhowyoulearn.repository.WeakAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfusionService {

    private static final long USER_ID = 1L;

    private final ConfusionLogRepository confusionLogRepository;
    private final WeakAreaRepository weakAreaRepository;
    private final LearningEventRepository learningEventRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public ConfusionResponse logConfusion(Long lectureId, ConfusionRequest request) {
        var lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        ConfusionLog log = confusionLogRepository.save(ConfusionLog.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .courseId(lecture.getCourseId())
                .confusionType(request.getConfusionType())
                .sectionTitle(request.getSectionTitle())
                .note(request.getNote())
                .resolved(false)
                .build());

        // Create or update a weak area entry
        String weakness = buildWeaknessDescription(request);
        weakAreaRepository.save(WeakArea.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .courseId(lecture.getCourseId())
                .weakness(weakness)
                .severity("MEDIUM")
                .status("ACTIVE")
                .build());

        learningEventRepository.save(LearningEvent.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .courseId(lecture.getCourseId())
                .eventType("CONFUSED_CLICKED")
                .build());

        return ConfusionResponse.builder()
                .id(log.getId())
                .confusionType(log.getConfusionType())
                .sectionTitle(log.getSectionTitle())
                .note(log.getNote())
                .resolved(log.isResolved())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String buildWeaknessDescription(ConfusionRequest request) {
        String base = switch (request.getConfusionType()) {
            case "THEORY_UNCLEAR" -> "Theory not clear";
            case "EXAMPLE_UNCLEAR" -> "Examples not understood";
            case "WHEN_TO_USE_UNCLEAR" -> "Not sure when to apply this";
            case "CODE_CONFUSING" -> "Code implementation confusing";
            case "EDGE_CASE_CONFUSING" -> "Edge cases not clear";
            case "LOST_FOCUS" -> "Lost track of the flow";
            default -> "General confusion";
        };
        if (request.getSectionTitle() != null) {
            base += " in section: " + request.getSectionTitle();
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            base += " — " + request.getNote();
        }
        return base;
    }
}
