package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.StudyTimelineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudyTimelineItemRepository extends JpaRepository<StudyTimelineItem, Long> {

    List<StudyTimelineItem> findByTargetIdAndScheduledDate(Long targetId, LocalDate date);

    List<StudyTimelineItem> findByTargetIdAndScheduledDateBetween(Long targetId, LocalDate from, LocalDate to);

    List<StudyTimelineItem> findByUserIdAndScheduledDateAndStatusIn(Long userId, LocalDate date, List<String> statuses);

    long countByTargetIdAndStatus(Long targetId, String status);

    @Modifying
    @Query("DELETE FROM StudyTimelineItem i WHERE i.targetId = :targetId " +
           "AND i.scheduledDate >= :fromDate " +
           "AND i.status IN ('PENDING', 'SKIPPED', 'RESCHEDULED')")
    void deleteRegeneratableItems(@Param("targetId") Long targetId,
                                  @Param("fromDate") LocalDate fromDate);
}
