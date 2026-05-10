package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.WeakArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeakAreaRepository extends JpaRepository<WeakArea, Long> {
    List<WeakArea> findByUserIdAndLectureIdAndStatus(Long userId, Long lectureId, String status);
    List<WeakArea> findByUserIdAndTopicIdAndStatus(Long userId, Long topicId, String status);
    List<WeakArea> findByUserIdAndStatusOrderBySeverityDescCreatedAtDesc(Long userId, String status);
}
