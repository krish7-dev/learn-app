package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {
}
