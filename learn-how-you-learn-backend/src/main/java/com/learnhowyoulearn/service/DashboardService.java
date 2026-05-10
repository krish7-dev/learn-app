package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.response.DashboardResponse;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.dto.response.RevisionItemResponse;
import com.learnhowyoulearn.dto.response.TopicSummaryResponse;
import com.learnhowyoulearn.entity.LectureStatus;
import com.learnhowyoulearn.mapper.LectureMapper;
import com.learnhowyoulearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final long USER_ID = 1L;

    private final LectureRepository lectureRepository;
    private final LectureNotesRepository lectureNotesRepository;
    private final TopicRepository topicRepository;
    private final RevisionItemRepository revisionItemRepository;
    private final LectureMapper lectureMapper;

    @Transactional(readOnly = true)
    public DashboardResponse getToday() {
        // Continue lecture: most recently studied IN_PROGRESS lecture, or last-updated NOT_STARTED
        LectureSummaryResponse continueLecture = lectureRepository
                .findInProgressByUser(USER_ID, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(l -> lectureMapper.toSummary(l,
                        lectureNotesRepository.existsByUserIdAndLectureId(USER_ID, l.getId())))
                .orElse(null);

        // Revision due (up to 3, due within next 24h)
        List<RevisionItemResponse> revisionDue = revisionItemRepository
                .findByUserIdAndStatusAndDueAtBeforeOrderByPriorityDescDueAtAsc(
                        USER_ID, "PENDING", LocalDateTime.now().plusDays(1))
                .stream()
                .limit(3)
                .map(item -> RevisionItemResponse.builder()
                        .id(item.getId())
                        .lectureId(item.getLectureId())
                        .topicId(item.getTopicId())
                        .title(item.getTitle())
                        .revisionType(item.getRevisionType())
                        .dueAt(item.getDueAt())
                        .status(item.getStatus())
                        .priority(item.getPriority())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();

        // Weak topics: top 5 NOT_STARTED or LEARNING topics by lowest mastery score
        List<TopicSummaryResponse> weakTopics = topicRepository
                .findByUserIdAndStatusOrderByMasteryScoreAsc(USER_ID, "NOT_STARTED")
                .stream()
                .limit(5)
                .map(t -> TopicSummaryResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .category(t.getCategory())
                        .difficulty(t.getDifficulty() != null ? t.getDifficulty().name() : null)
                        .masteryScore(t.getMasteryScore())
                        .status(t.getStatus())
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .build())
                .toList();

        long totalLectures = lectureRepository.countByUserIdAndDeletedAtIsNull(USER_ID);
        long completedLectures = lectureRepository.countByUserIdAndStatusAndDeletedAtIsNull(USER_ID, LectureStatus.COMPLETED);
        long totalTopics = topicRepository.countByUserId(USER_ID);
        long masteredTopics = topicRepository.countByUserIdAndStatus(USER_ID, "MASTERED");

        return DashboardResponse.builder()
                .continueLecture(continueLecture)
                .revisionDue(revisionDue)
                .weakTopics(weakTopics)
                .totalLectures(totalLectures)
                .completedLectures(completedLectures)
                .totalTopics(totalTopics)
                .masteredTopics(masteredTopics)
                .build();
    }
}
