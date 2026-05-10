package com.learnhowyoulearn.service.context;

import com.learnhowyoulearn.dto.context.LearningContext;
import com.learnhowyoulearn.entity.*;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningContextLoaderService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final LearningProfileRepository learningProfileRepository;
    private final WeakAreaRepository weakAreaRepository;
    private final ConfusionLogRepository confusionLogRepository;
    private final TopicRepository topicRepository;
    private final LectureChatMessageRepository lectureChatMessageRepository;
    private final AiMemorySummaryRepository aiMemorySummaryRepository;
    private final LectureNotesRepository lectureNotesRepository;

    @Transactional(readOnly = true)
    public LearningContext loadForLectureNoteGeneration(Long userId, Long lectureId) {
        Lecture lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        Course course = courseRepository.findByIdAndUserIdAndDeletedAtIsNull(lecture.getCourseId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + lecture.getCourseId()));

        LearningProfile profile = learningProfileRepository.findByUserId(userId).orElse(null);

        List<String> weakAreas = weakAreaRepository
                .findByUserIdAndLectureIdAndStatus(userId, lectureId, "ACTIVE")
                .stream().map(WeakArea::getWeakness).collect(Collectors.toList());

        List<String> confusions = confusionLogRepository
                .findByUserIdAndLectureIdOrderByCreatedAtDesc(userId, lectureId, PageRequest.of(0, 10))
                .stream()
                .map(c -> c.getConfusionType() + (c.getNote() != null ? ": " + c.getNote() : ""))
                .collect(Collectors.toList());

        List<String> existingTopics = topicRepository
                .findByUserIdAndStatusOrderByMasteryScoreAsc(userId, "LEARNING")
                .stream().map(Topic::getName).collect(Collectors.toList());

        return LearningContext.builder()
                .course(course)
                .lecture(lecture)
                .learningProfile(profile)
                .activeWeakAreaDescriptions(weakAreas)
                .recentConfusionNotes(confusions)
                .existingTopicNames(existingTopics)
                .recentChatMessages(Collections.emptyList())
                .lectureNotesText(null)
                .build();
    }

    @Transactional(readOnly = true)
    public LearningContext loadForTutorChat(Long userId, Long lectureId) {
        Lecture lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        Course course = courseRepository.findByIdAndUserIdAndDeletedAtIsNull(lecture.getCourseId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + lecture.getCourseId()));

        LearningProfile profile = learningProfileRepository.findByUserId(userId).orElse(null);

        List<String> weakAreas = weakAreaRepository
                .findByUserIdAndLectureIdAndStatus(userId, lectureId, "ACTIVE")
                .stream().map(WeakArea::getWeakness).collect(Collectors.toList());

        List<String> recentMessages = lectureChatMessageRepository
                .findByUserIdAndLectureIdOrderByCreatedAtDesc(userId, lectureId, PageRequest.of(0, 15))
                .stream()
                .map(m -> m.getRole() + ": " + m.getMessage())
                .collect(Collectors.toList());
        Collections.reverse(recentMessages);

        String memorySummary = aiMemorySummaryRepository
                .findTopByUserIdAndLectureIdOrderByCreatedAtDesc(userId, lectureId)
                .map(AiMemorySummary::getSummary).orElse(null);

        String notesText = lectureNotesRepository.findByUserIdAndLectureId(userId, lectureId)
                .map(LectureNotes::getFullCleanNotes).orElse(null);

        return LearningContext.builder()
                .course(course)
                .lecture(lecture)
                .learningProfile(profile)
                .activeWeakAreaDescriptions(weakAreas)
                .recentChatMessages(recentMessages)
                .latestMemorySummary(memorySummary)
                .lectureNotesText(notesText)
                .recentConfusionNotes(Collections.emptyList())
                .existingTopicNames(Collections.emptyList())
                .build();
    }
}
