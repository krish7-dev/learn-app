package com.learnhowyoulearn.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_memory_summary")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AiMemorySummary {

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

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "understood_points", columnDefinition = "jsonb")
    private JsonNode understoodPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "struggled_points", columnDefinition = "jsonb")
    private JsonNode struggledPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommended_next_steps", columnDefinition = "jsonb")
    private JsonNode recommendedNextSteps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_signals", columnDefinition = "jsonb")
    private JsonNode profileSignals;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
