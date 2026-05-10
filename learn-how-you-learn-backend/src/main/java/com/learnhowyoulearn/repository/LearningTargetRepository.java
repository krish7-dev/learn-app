package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.LearningTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningTargetRepository extends JpaRepository<LearningTarget, Long> {

    @Query("SELECT t FROM LearningTarget t WHERE t.userId = :userId AND t.status = :status " +
           "ORDER BY CASE t.priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END")
    List<LearningTarget> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    Optional<LearningTarget> findByIdAndUserId(Long id, Long userId);
}
