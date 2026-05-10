package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.LectureChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureChatMessageRepository extends JpaRepository<LectureChatMessage, Long> {
    List<LectureChatMessage> findByUserIdAndLectureIdOrderByCreatedAtDesc(Long userId, Long lectureId, Pageable pageable);
}
