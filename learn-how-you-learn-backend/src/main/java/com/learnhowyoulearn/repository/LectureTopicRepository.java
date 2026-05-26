package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.LectureTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureTopicRepository extends JpaRepository<LectureTopic, Long> {
    List<LectureTopic> findByUserIdAndLectureId(Long userId, Long lectureId);
    List<LectureTopic> findByUserIdAndTopicId(Long userId, Long topicId);
    boolean existsByUserIdAndLectureIdAndTopicId(Long userId, Long lectureId, Long topicId);
    java.util.Optional<LectureTopic> findByUserIdAndLectureIdAndTopicId(Long userId, Long lectureId, Long topicId);
    void deleteByUserIdAndLectureId(Long userId, Long lectureId);
    void deleteByTopicId(Long topicId);
    List<LectureTopic> findAllByUserId(Long userId);
}
