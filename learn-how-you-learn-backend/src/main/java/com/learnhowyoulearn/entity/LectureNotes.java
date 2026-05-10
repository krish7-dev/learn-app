package com.learnhowyoulearn.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_notes")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LectureNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "full_clean_notes", columnDefinition = "TEXT")
    private String fullCleanNotes;

    @Column(name = "simple_explanation", columnDefinition = "TEXT")
    private String simpleExplanation;

    @Column(name = "practical_usage", columnDefinition = "TEXT")
    private String practicalUsage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "examples", columnDefinition = "jsonb")
    private JsonNode examples;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mistakes_to_avoid", columnDefinition = "jsonb")
    private JsonNode mistakesToAvoid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edge_cases", columnDefinition = "jsonb")
    private JsonNode edgeCases;

    @Column(name = "revision_notes", columnDefinition = "TEXT")
    private String revisionNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interview_questions", columnDefinition = "jsonb")
    private JsonNode interviewQuestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "flashcards", columnDefinition = "jsonb")
    private JsonNode flashcards;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "practice_questions", columnDefinition = "jsonb")
    private JsonNode practiceQuestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weak_area_checks", columnDefinition = "jsonb")
    private JsonNode weakAreaChecks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_response", columnDefinition = "jsonb")
    private JsonNode rawAiResponse;

    @Column(name = "chat_additions", columnDefinition = "TEXT")
    private String chatAdditions;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
