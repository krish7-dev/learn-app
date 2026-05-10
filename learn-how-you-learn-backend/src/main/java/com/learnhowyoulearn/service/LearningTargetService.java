package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.CreateTargetRequest;
import com.learnhowyoulearn.dto.request.UpdateTargetRequest;
import com.learnhowyoulearn.dto.response.LearningTargetResponse;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.LearningTargetRepository;
import com.learnhowyoulearn.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningTargetService {

    private static final long USER_ID = 1L;

    private final LearningTargetRepository learningTargetRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public LearningTargetResponse create(CreateTargetRequest request) {
        LearningTarget target = LearningTarget.builder()
                .userId(USER_ID)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetScope(request.getTargetScope())
                .courseId(request.getCourseId())
                .moduleName(request.getModuleName())
                .topicId(request.getTopicId())
                .targetDate(request.getTargetDate())
                .dailyMinutes(request.getDailyMinutes())
                .weeklyMinutes(request.getWeeklyMinutes())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .build();
        return enrich(learningTargetRepository.save(target));
    }

    @Transactional(readOnly = true)
    public List<LearningTargetResponse> listActive() {
        return learningTargetRepository.findByUserIdAndStatus(USER_ID, "ACTIVE")
                .stream()
                .map(this::enrich)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningTargetResponse getById(Long id) {
        return enrich(findOrThrow(id));
    }

    @Transactional
    public LearningTargetResponse update(Long id, UpdateTargetRequest request) {
        LearningTarget target = findOrThrow(id);
        if (request.getTitle() != null)        target.setTitle(request.getTitle());
        if (request.getDescription() != null)  target.setDescription(request.getDescription());
        if (request.getTargetDate() != null)   target.setTargetDate(request.getTargetDate());
        if (request.getDailyMinutes() != null) target.setDailyMinutes(request.getDailyMinutes());
        if (request.getWeeklyMinutes() != null) target.setWeeklyMinutes(request.getWeeklyMinutes());
        if (request.getPriority() != null)     target.setPriority(request.getPriority());
        if (request.getStatus() != null)       target.setStatus(request.getStatus());
        return enrich(learningTargetRepository.save(target));
    }

    @Transactional
    public void delete(Long id) {
        LearningTarget target = findOrThrow(id);
        target.setStatus("ABANDONED");
        learningTargetRepository.save(target);
    }

    public LearningTargetResponse enrich(LearningTarget target) {
        List<Lecture> lectures = fetchScopedLectures(target);

        double progressPercent = 0.0;
        int totalLectures = lectures.size();
        int completedLectures = 0;
        Boolean isOnTrack = null;

        if (!lectures.isEmpty()) {
            progressPercent = lectures.stream()
                    .mapToDouble(this::lectureProgress)
                    .average()
                    .orElse(0.0);
            completedLectures = (int) lectures.stream()
                    .filter(l -> "COMPLETED".equals(l.getStatus().name()))
                    .count();
            long daysPassed = ChronoUnit.DAYS.between(target.getCreatedAt().toLocalDate(), LocalDate.now());
            long totalDays  = ChronoUnit.DAYS.between(target.getCreatedAt().toLocalDate(), target.getTargetDate());
            if (totalDays > 0) {
                isOnTrack = progressPercent >= ((double) daysPassed / totalDays * 100);
            }
        }

        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), target.getTargetDate());

        return LearningTargetResponse.builder()
                .id(target.getId())
                .title(target.getTitle())
                .description(target.getDescription())
                .targetScope(target.getTargetScope())
                .courseId(target.getCourseId())
                .moduleName(target.getModuleName())
                .topicId(target.getTopicId())
                .targetDate(target.getTargetDate())
                .dailyMinutes(target.getDailyMinutes())
                .weeklyMinutes(target.getWeeklyMinutes())
                .priority(target.getPriority())
                .status(target.getStatus())
                .progressPercent(progressPercent)
                .isOnTrack(isOnTrack)
                .totalLectures(totalLectures)
                .completedLectures(completedLectures)
                .daysRemaining(Math.max(0, daysRemaining))
                .createdAt(target.getCreatedAt())
                .updatedAt(target.getUpdatedAt())
                .build();
    }

    public List<Lecture> fetchScopedLectures(LearningTarget target) {
        return switch (target.getTargetScope()) {
            case "COURSE"  -> target.getCourseId() != null
                    ? lectureRepository.findByCourseAndUserOrdered(USER_ID, target.getCourseId())
                    : List.of();
            case "MODULE"  -> (target.getCourseId() != null && target.getModuleName() != null)
                    ? lectureRepository.findByModuleAndUser(USER_ID, target.getCourseId(), target.getModuleName())
                    : List.of();
            case "TOPIC"   -> target.getTopicId() != null
                    ? lectureRepository.findByTopicAndUser(USER_ID, target.getTopicId())
                    : List.of();
            default        -> lectureRepository.findAllByUser(USER_ID);
        };
    }

    private double lectureProgress(Lecture l) {
        if ("COMPLETED".equals(l.getStatus().name()))              return 100.0;
        if ("NOTES_GENERATED".equals(l.getContentStatus()))        return 60.0;
        if ("TRANSCRIPT_ADDED".equals(l.getContentStatus()))       return 30.0;
        return 0.0;
    }

    private LearningTarget findOrThrow(Long id) {
        return learningTargetRepository.findByIdAndUserId(id, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + id));
    }
}
