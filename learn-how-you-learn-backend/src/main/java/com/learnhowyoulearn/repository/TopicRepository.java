package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByUserIdAndNormalizedName(Long userId, String normalizedName);
    Page<Topic> findByUserIdOrderByMasteryScoreAscUpdatedAtDesc(Long userId, Pageable pageable);
    List<Topic> findByUserIdAndStatusOrderByMasteryScoreAsc(Long userId, String status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);
}
