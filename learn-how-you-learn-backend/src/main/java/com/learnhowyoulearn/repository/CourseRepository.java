package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Course> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
