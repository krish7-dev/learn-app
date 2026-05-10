package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.Lecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    @Query("""
            SELECT l FROM Lecture l
            WHERE l.userId = :userId AND l.courseId = :courseId AND l.deletedAt IS NULL
            ORDER BY l.moduleName ASC NULLS LAST, l.sourceOrder ASC NULLS LAST, l.createdAt ASC
            """)
    Page<Lecture> findByCourseAndUser(@Param("userId") Long userId,
                                      @Param("courseId") Long courseId,
                                      Pageable pageable);

    Optional<Lecture> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @Query("""
            SELECT l FROM Lecture l
            WHERE l.userId = :userId AND l.deletedAt IS NULL AND l.status = 'IN_PROGRESS'
            ORDER BY l.lastStudiedAt DESC NULLS LAST, l.updatedAt DESC
            """)
    List<Lecture> findInProgressByUser(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    long countByUserIdAndStatusAndDeletedAtIsNull(Long userId, com.learnhowyoulearn.entity.LectureStatus status);

    @Modifying
    @Query("UPDATE Lecture l SET l.deletedAt = :now WHERE l.courseId = :courseId AND l.deletedAt IS NULL")
    void softDeleteByCourseId(@Param("courseId") Long courseId, @Param("now") LocalDateTime now);
}
