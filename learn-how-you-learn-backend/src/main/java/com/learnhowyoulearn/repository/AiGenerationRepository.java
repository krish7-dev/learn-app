package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.AiGeneration;
import com.learnhowyoulearn.entity.AiGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiGenerationRepository extends JpaRepository<AiGeneration, Long> {

    @Query("SELECT g FROM AiGeneration g WHERE g.lectureId = :lectureId AND g.status = :status ORDER BY g.createdAt DESC LIMIT 1")
    Optional<AiGeneration> findLatestByLectureIdAndStatus(Long lectureId, AiGenerationStatus status);
}
