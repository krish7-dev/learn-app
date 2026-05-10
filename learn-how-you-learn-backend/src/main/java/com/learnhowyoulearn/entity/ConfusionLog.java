package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "confusion_logs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ConfusionLog {

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

    @Column(name = "section_title", length = 255)
    private String sectionTitle;

    @Column(name = "confusion_type", length = 30, nullable = false)
    private String confusionType;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
