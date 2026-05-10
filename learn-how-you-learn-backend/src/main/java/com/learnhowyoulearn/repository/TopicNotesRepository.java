package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.TopicNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicNotesRepository extends JpaRepository<TopicNotes, Long> {
    Optional<TopicNotes> findByUserIdAndTopicId(Long userId, Long topicId);
    void deleteByTopicId(Long topicId);
}
