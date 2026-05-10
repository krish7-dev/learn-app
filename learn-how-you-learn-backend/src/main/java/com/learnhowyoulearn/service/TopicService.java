package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.node.TextNode;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.dto.response.TopicDetailResponse;
import com.learnhowyoulearn.dto.response.TopicSummaryResponse;
import com.learnhowyoulearn.entity.LectureTopic;
import com.learnhowyoulearn.entity.Topic;
import com.learnhowyoulearn.entity.TopicNotes;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.mapper.LectureMapper;
import com.learnhowyoulearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private static final long USER_ID = 1L;

    private final TopicRepository topicRepository;
    private final TopicNotesRepository topicNotesRepository;
    private final LectureTopicRepository lectureTopicRepository;
    private final LectureRepository lectureRepository;
    private final LectureNotesRepository lectureNotesRepository;
    private final LectureMapper lectureMapper;

    @Transactional(readOnly = true)
    public PageResponse<TopicSummaryResponse> list(int page, int size) {
        Page<Topic> topicPage = topicRepository
                .findByUserIdOrderByMasteryScoreAscUpdatedAtDesc(USER_ID, PageRequest.of(page, size));
        return PageResponse.<TopicSummaryResponse>builder()
                .content(topicPage.getContent().stream().map(this::toSummary).toList())
                .page(topicPage.getNumber())
                .size(topicPage.getSize())
                .totalElements(topicPage.getTotalElements())
                .totalPages(topicPage.getTotalPages())
                .last(topicPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TopicDetailResponse getById(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));

        Optional<TopicNotes> notes = topicNotesRepository.findByUserIdAndTopicId(USER_ID, id);

        List<LectureSummaryResponse> linkedLectures = lectureTopicRepository
                .findByUserIdAndTopicId(USER_ID, id).stream()
                .map(LectureTopic::getLectureId)
                .map(lectureId -> lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(lecture -> lectureMapper.toSummary(lecture,
                        lectureNotesRepository.existsByUserIdAndLectureId(USER_ID, lecture.getId())))
                .collect(Collectors.toList());

        return TopicDetailResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .category(topic.getCategory())
                .difficulty(topic.getDifficulty() != null ? topic.getDifficulty().name() : null)
                .masteryScore(topic.getMasteryScore())
                .status(topic.getStatus())
                .combinedNotes(notes.map(n -> (com.fasterxml.jackson.databind.JsonNode) TextNode.valueOf(n.getCombinedNotes())).orElse(null))
                .examples(notes.map(TopicNotes::getExamples).orElse(null))
                .edgeCases(notes.map(TopicNotes::getEdgeCases).orElse(null))
                .mistakesToAvoid(notes.map(TopicNotes::getMistakesToAvoid).orElse(null))
                .patterns(notes.map(TopicNotes::getPatterns).orElse(null))
                .revisionNotes(notes.map(TopicNotes::getRevisionNotes).orElse(null))
                .linkedLectures(linkedLectures)
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    @Transactional
    public void delete(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
        topicNotesRepository.deleteByTopicId(id);
        lectureTopicRepository.deleteByTopicId(id);
        topicRepository.delete(topic);
    }

    private TopicSummaryResponse toSummary(Topic topic) {
        return TopicSummaryResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .category(topic.getCategory())
                .difficulty(topic.getDifficulty() != null ? topic.getDifficulty().name() : null)
                .masteryScore(topic.getMasteryScore())
                .status(topic.getStatus())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }
}
