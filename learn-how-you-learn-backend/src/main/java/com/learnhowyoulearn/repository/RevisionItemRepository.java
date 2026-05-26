package com.learnhowyoulearn.repository;

import com.learnhowyoulearn.entity.RevisionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RevisionItemRepository extends JpaRepository<RevisionItem, Long> {
    List<RevisionItem> findByUserIdAndStatusAndDueAtBeforeOrderByPriorityDescDueAtAsc(
            Long userId, String status, LocalDateTime before);

    List<RevisionItem> findByUserIdAndStatusAndDueAtBefore(Long userId, String status, LocalDateTime before);

    List<RevisionItem> findAllByUserIdAndStatus(Long userId, String status);
}
