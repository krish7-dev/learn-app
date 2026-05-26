package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.context.ParsedTimeline;
import com.learnhowyoulearn.dto.request.UpdateTimelineItemRequest;
import com.learnhowyoulearn.dto.response.DayPlanResponse;
import com.learnhowyoulearn.dto.response.StudyTimelineItemResponse;
import com.learnhowyoulearn.dto.response.WeekPlanResponse;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.entity.StudyTimelineItem;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.LearningTargetRepository;
import com.learnhowyoulearn.repository.StudyTimelineItemRepository;
import com.learnhowyoulearn.service.persistence.TimelinePersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyTimelineService {

    private static final long USER_ID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StudyTimelineItemRepository timelineItemRepository;
    private final LearningTargetRepository learningTargetRepository;
    private final LearningTargetService learningTargetService;
    private final TimelinePersistenceService timelinePersistenceService;

    @Transactional(readOnly = true)
    public DayPlanResponse getToday(Long targetId) {
        LocalDate today = LocalDate.now();
        List<StudyTimelineItem> items = timelineItemRepository.findByTargetIdAndScheduledDate(targetId, today);
        boolean hasMissedItems = timelineItemRepository
                .findByTargetIdAndScheduledDateBetween(targetId, today.minusDays(30), today.minusDays(1))
                .stream()
                .anyMatch(i -> "PENDING".equals(i.getStatus()));
        return buildDayPlan(today, items, hasMissedItems);
    }

    @Transactional(readOnly = true)
    public WeekPlanResponse getWeek(Long targetId) {
        LearningTarget target = learningTargetRepository.findByIdAndUserId(targetId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(6);
        List<StudyTimelineItem> allItems = timelineItemRepository
                .findByTargetIdAndScheduledDateBetween(targetId, today, end);

        List<DayPlanResponse> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            List<StudyTimelineItem> dayItems = allItems.stream()
                    .filter(item -> date.equals(item.getScheduledDate()))
                    .toList();
            days.add(buildDayPlan(date, dayItems, false));
        }

        return WeekPlanResponse.builder()
                .target(learningTargetService.enrich(target))
                .days(days)
                .build();
    }

    @Transactional(readOnly = true)
    public WeekPlanResponse getFullTimeline(Long targetId) {
        LearningTarget target = learningTargetRepository.findByIdAndUserId(targetId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));

        LocalDate today = LocalDate.now();
        LocalDate end = target.getTargetDate().isBefore(today) ? today : target.getTargetDate();

        List<StudyTimelineItem> allItems = timelineItemRepository
                .findByTargetIdAndScheduledDateBetween(targetId, today, end);

        List<DayPlanResponse> days = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(end); date = date.plusDays(1)) {
            final LocalDate d = date;
            List<StudyTimelineItem> dayItems = allItems.stream()
                    .filter(i -> d.equals(i.getScheduledDate()))
                    .toList();
            days.add(buildDayPlan(date, dayItems, false));
        }

        return WeekPlanResponse.builder()
                .target(learningTargetService.enrich(target))
                .days(days)
                .build();
    }

    @Transactional
    public void importTimeline(Long targetId, ParsedTimeline timeline) {
        LearningTarget target = learningTargetRepository.findByIdAndUserId(targetId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));
        timelinePersistenceService.saveTimeline(targetId, timeline, target.getTargetDate());
    }

    @Transactional
    public void clearTimeline(Long targetId) {
        learningTargetRepository.findByIdAndUserId(targetId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found: " + targetId));
        LocalDate today = LocalDate.now();
        timelineItemRepository.deleteRegeneratableItems(targetId, today, today.plusDays(13));
    }

    @Transactional
    public StudyTimelineItemResponse markItem(Long itemId, UpdateTimelineItemRequest request) {
        StudyTimelineItem item = timelineItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Timeline item not found: " + itemId));
        item.setStatus(request.getStatus());
        if ("RESCHEDULED".equals(request.getStatus()) && request.getRescheduledDate() != null) {
            // Create a new item on the rescheduled date rather than moving the existing one
            StudyTimelineItem rescheduled = StudyTimelineItem.builder()
                    .userId(item.getUserId())
                    .targetId(item.getTargetId())
                    .courseId(item.getCourseId())
                    .lectureId(item.getLectureId())
                    .topicId(item.getTopicId())
                    .scheduledDate(request.getRescheduledDate())
                    .itemType(item.getItemType())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .estimatedMinutes(item.getEstimatedMinutes())
                    .planTier(item.getPlanTier())
                    .aiReasoning(item.getAiReasoning())
                    .build();
            timelineItemRepository.save(rescheduled);
        }
        return toResponse(timelineItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<StudyTimelineItemResponse> getDashboardMinimumPlan(Long userId) {
        LocalDate today = LocalDate.now();
        return timelineItemRepository
                .findByUserIdAndScheduledDateAndStatusIn(userId, today, List.of("PENDING"))
                .stream()
                .limit(5)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DayPlanResponse buildDayPlan(LocalDate date, List<StudyTimelineItem> items, boolean hasMissedItems) {
        List<StudyTimelineItemResponse> responses = items.stream().map(this::toResponse).toList();
        int totalMinutes = items.stream().mapToInt(StudyTimelineItem::getEstimatedMinutes).sum();

        return DayPlanResponse.builder()
                .date(date.format(DATE_FMT))
                .items(responses)
                .totalMinutes(totalMinutes)
                .hasMissedItems(hasMissedItems)
                .build();
    }

    private StudyTimelineItemResponse toResponse(StudyTimelineItem item) {
        return StudyTimelineItemResponse.builder()
                .id(item.getId())
                .targetId(item.getTargetId())
                .courseId(item.getCourseId())
                .lectureId(item.getLectureId())
                .topicId(item.getTopicId())
                .scheduledDate(item.getScheduledDate().format(DATE_FMT))
                .itemType(item.getItemType())
                .title(item.getTitle())
                .description(item.getDescription())
                .estimatedMinutes(item.getEstimatedMinutes())
                .planTier(item.getPlanTier())
                .status(item.getStatus())
                .aiReasoning(item.getAiReasoning())
                .build();
    }
}
