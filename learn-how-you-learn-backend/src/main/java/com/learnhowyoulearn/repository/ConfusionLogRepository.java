package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.ConfusionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfusionLogRepository extends JpaRepository<ConfusionLog, Long> {
    List<ConfusionLog> findByUserIdAndLectureIdOrderByCreatedAtDesc(Long userId, Long lectureId, Pageable pageable);
}
