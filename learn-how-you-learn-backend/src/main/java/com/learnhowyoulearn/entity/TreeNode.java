package com.learnhowyoulearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tree_nodes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TreeNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "normalized_label", nullable = false)
    private String normalizedLabel;

    @Column(name = "node_type", nullable = false)
    private String nodeType;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
