package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.UpdateRevisionRequest;
import com.learnhowyoulearn.dto.response.RevisionItemResponse;
import com.learnhowyoulearn.entity.LearningEvent;
import com.learnhowyoulearn.entity.RevisionItem;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.LearningEventRepository;
import com.learnhowyoulearn.repository.RevisionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private static final long USER_ID = 1L;

    private final RevisionItemRepository revisionItemRepository;
    private final LearningEventRepository learningEventRepository;

    @Transactional(readOnly = true)
    public List<RevisionItemResponse> getPending() {
        return revisionItemRepository
                .findByUserIdAndStatusAndDueAtBeforeOrderByPriorityDescDueAtAsc(
                        USER_ID, "PENDING", LocalDateTime.now().plusDays(1))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RevisionItemResponse update(Long id, UpdateRevisionRequest request) {
        RevisionItem item = revisionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revision item not found: " + id));
        item.setStatus(request.getStatus());
        RevisionItem saved = revisionItemRepository.save(item);

        if ("DONE".equals(request.getStatus())) {
            learningEventRepository.save(LearningEvent.builder()
                    .userId(USER_ID)
                    .lectureId(item.getLectureId())
                    .topicId(item.getTopicId())
                    .eventType("REVISION_DONE")
                    .build());
        }

        return toResponse(saved);
    }

    private RevisionItemResponse toResponse(RevisionItem item) {
        return RevisionItemResponse.builder()
                .id(item.getId())
                .lectureId(item.getLectureId())
                .topicId(item.getTopicId())
                .title(item.getTitle())
                .revisionType(item.getRevisionType())
                .dueAt(item.getDueAt())
                .status(item.getStatus())
                .priority(item.getPriority())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
