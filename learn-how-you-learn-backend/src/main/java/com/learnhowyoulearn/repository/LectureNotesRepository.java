package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.LectureNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureNotesRepository extends JpaRepository<LectureNotes, Long> {
    Optional<LectureNotes> findByUserIdAndLectureId(Long userId, Long lectureId);
    boolean existsByUserIdAndLectureId(Long userId, Long lectureId);
}
