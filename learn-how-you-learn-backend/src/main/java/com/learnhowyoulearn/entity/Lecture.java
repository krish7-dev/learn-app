package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "lectures")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "module_name")
    private String moduleName;

    @Column(nullable = false)
    private String title;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_order")
    private Integer sourceOrder;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LectureStatus status;

    @Builder.Default
    @Column(name = "content_status", nullable = false)
    private String contentStatus = "NOT_ADDED";

    @Builder.Default
    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes = 60;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
