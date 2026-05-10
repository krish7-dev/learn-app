package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.AiMemorySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiMemorySummaryRepository extends JpaRepository<AiMemorySummary, Long> {
    Optional<AiMemorySummary> findTopByUserIdAndLectureIdOrderByCreatedAtDesc(Long userId, Long lectureId);
}
