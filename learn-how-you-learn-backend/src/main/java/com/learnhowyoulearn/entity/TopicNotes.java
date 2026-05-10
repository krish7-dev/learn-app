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
@Table(name = "topic_notes")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TopicNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "combined_notes", columnDefinition = "TEXT")
    private String combinedNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "examples", columnDefinition = "jsonb")
    private JsonNode examples;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edge_cases", columnDefinition = "jsonb")
    private JsonNode edgeCases;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mistakes_to_avoid", columnDefinition = "jsonb")
    private JsonNode mistakesToAvoid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "patterns", columnDefinition = "jsonb")
    private JsonNode patterns;

    @Column(name = "revision_notes", columnDefinition = "TEXT")
    private String revisionNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_lecture_ids", columnDefinition = "jsonb")
    private JsonNode sourceLectureIds;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
