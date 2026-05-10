package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.LearningProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningProfileRepository extends JpaRepository<LearningProfile, Long> {

    Optional<LearningProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
