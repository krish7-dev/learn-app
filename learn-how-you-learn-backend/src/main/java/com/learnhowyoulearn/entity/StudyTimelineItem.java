package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_timeline_items")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudyTimelineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Column(name = "plan_tier", nullable = false, length = 10)
    private String planTier;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (estimatedMinutes == null) estimatedMinutes = 30;
        if (planTier == null) planTier = "FULL";
        if (status == null)   status = "PENDING";
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
