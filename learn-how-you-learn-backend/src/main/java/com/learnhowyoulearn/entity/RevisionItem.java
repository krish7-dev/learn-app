package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "revision_items")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RevisionItem {

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

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "revision_type", nullable = false, length = 20)
    private String revisionType;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "priority", nullable = false, length = 10)
    private String priority;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
