package com.learnhowyoulearn.service.persistence;

import com.learnhowyoulearn.dto.context.ParsedTimeline;
import com.learnhowyoulearn.entity.StudyTimelineItem;
import com.learnhowyoulearn.repository.StudyTimelineItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelinePersistenceService {

    private static final long USER_ID = 1L;

    private final StudyTimelineItemRepository timelineItemRepository;

    @Transactional
    public void saveTimeline(Long targetId, ParsedTimeline parsed, LocalDate clearUntil) {
        LocalDate today = LocalDate.now();
        timelineItemRepository.deleteRegeneratableItems(targetId, today, clearUntil);

        List<StudyTimelineItem> items = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (ParsedTimeline.ParsedDay day : parsed.getDays()) {
            if (day.getItems() == null) continue;
            LocalDate date = LocalDate.parse(day.getDate(), fmt);
            for (ParsedTimeline.ParsedItem item : day.getItems()) {
                items.add(StudyTimelineItem.builder()
                        .userId(USER_ID)
                        .targetId(targetId)
                        .lectureId(item.getLectureId())
                        .topicId(item.getTopicId())
                        .scheduledDate(date)
                        .itemType(item.getItemType())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .estimatedMinutes(item.getEstimatedMinutes() > 0 ? item.getEstimatedMinutes() : 30)
                        .planTier(item.getPlanTier() != null ? item.getPlanTier() : "FULL")
                        .aiReasoning(item.getAiReasoning())
                        .build());
            }
        }

        if (!items.isEmpty()) {
            timelineItemRepository.saveAll(items);
        }
        log.info("Saved {} timeline items for targetId={}", items.size(), targetId);
    }
}
