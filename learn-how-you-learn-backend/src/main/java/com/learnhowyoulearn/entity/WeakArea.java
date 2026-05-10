package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weak_areas")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WeakArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "weakness", columnDefinition = "TEXT", nullable = false)
    private String weakness;

    @Column(name = "severity", length = 10, nullable = false)
    private String severity;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (severity == null) severity = "MEDIUM";
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
