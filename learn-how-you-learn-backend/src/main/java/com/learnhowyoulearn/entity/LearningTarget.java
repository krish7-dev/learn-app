package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_targets")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LearningTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_scope", nullable = false, length = 20)
    private String targetScope;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "daily_minutes", nullable = false)
    private Integer dailyMinutes;

    @Column(name = "weekly_minutes")
    private Integer weeklyMinutes;

    @Column(nullable = false, length = 10)
    private String priority;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (targetScope == null) targetScope = "COURSE";
        if (priority == null)    priority = "MEDIUM";
        if (status == null)      status = "ACTIVE";
        if (dailyMinutes == null) dailyMinutes = 60;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
