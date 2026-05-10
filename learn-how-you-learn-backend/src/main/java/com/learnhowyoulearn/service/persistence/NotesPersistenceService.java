package com.learnhowyoulearn.service.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.learnhowyoulearn.dto.context.ExtractedTopic;
import com.learnhowyoulearn.dto.context.ParsedNotesResponse;
import com.learnhowyoulearn.entity.*;
import com.learnhowyoulearn.repository.*;
import com.learnhowyoulearn.util.TopicNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotesPersistenceService {

    private static final long USER_ID = 1L;

    private final LectureNotesRepository lectureNotesRepository;
    private final LectureRepository lectureRepository;
    private final TopicRepository topicRepository;
    private final LectureTopicRepository lectureTopicRepository;
    private final LearningEventRepository learningEventRepository;
    private final RevisionItemRepository revisionItemRepository;
    private final TopicNormalizer topicNormalizer;
    private final ObjectMapper objectMapper;

    @Transactional
    public LectureNotes saveAll(Long lectureId, Long courseId, ParsedNotesResponse parsed, JsonNode rawAiResponse) {
        LectureNotes notes = saveOrUpdateNotes(lectureId, courseId, parsed, rawAiResponse);
        markNotesGenerated(lectureId);

        List<Long> topicIds = new ArrayList<>();
        if (parsed.getExtractedTopics() != null) {
            for (ExtractedTopic extracted : parsed.getExtractedTopics()) {
                Topic topic = upsertTopic(extracted);
                topicIds.add(topic.getId());
                upsertLectureTopic(lectureId, topic.getId(), extracted);
            }
        }

        saveRevisionItems(lectureId, courseId, parsed.getTitle());
        saveEvents(lectureId, courseId, topicIds);

        return notes;
    }

    private void markNotesGenerated(Long lectureId) {
        lectureRepository.findById(lectureId).ifPresent(lecture -> {
            lecture.setContentStatus("NOTES_GENERATED");
            lectureRepository.save(lecture);
        });
    }

    private LectureNotes saveOrUpdateNotes(Long lectureId, Long courseId, ParsedNotesResponse parsed, JsonNode rawAiResponse) {
        LectureNotes notes = lectureNotesRepository.findByUserIdAndLectureId(USER_ID, lectureId)
                .orElse(LectureNotes.builder().userId(USER_ID).lectureId(lectureId).courseId(courseId).build());

        notes.setTitle(parsed.getTitle());
        notes.setFullCleanNotes(parsed.getFullCleanNotes());
        notes.setSimpleExplanation(parsed.getSimpleExplanation());
        notes.setPracticalUsage(parsed.getPracticalUsage());
        notes.setExamples(parsed.getExamples());
        notes.setMistakesToAvoid(parsed.getMistakesToAvoid());
        notes.setEdgeCases(parsed.getEdgeCases());
        notes.setRevisionNotes(parsed.getRevisionNotes());
        notes.setInterviewQuestions(parsed.getInterviewQuestions());
        notes.setFlashcards(parsed.getFlashcards());
        notes.setPracticeQuestions(parsed.getPracticeQuestions());
        notes.setWeakAreaChecks(parsed.getWeakAreaChecks());
        notes.setRawAiResponse(rawAiResponse);

        return lectureNotesRepository.save(notes);
    }

    private Topic upsertTopic(ExtractedTopic extracted) {
        String normalized = topicNormalizer.normalize(extracted.getName());
        return topicRepository.findByUserIdAndNormalizedName(USER_ID, normalized)
                .orElseGet(() -> topicRepository.save(Topic.builder()
                        .userId(USER_ID)
                        .name(extracted.getName())
                        .normalizedName(normalized)
                        .masteryScore(0)
                        .status("NOT_STARTED")
                        .build()));
    }

    private void upsertLectureTopic(Long lectureId, Long topicId, ExtractedTopic extracted) {
        if (lectureTopicRepository.existsByUserIdAndLectureIdAndTopicId(USER_ID, lectureId, topicId)) {
            return;
        }
        lectureTopicRepository.save(LectureTopic.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .topicId(topicId)
                .importance(extracted.getImportance() != null ? extracted.getImportance() : "MEDIUM")
                .coverageLevel(extracted.getCoverageLevel() != null ? extracted.getCoverageLevel() : "INTRO")
                .evidence(extracted.getEvidence())
                .build());
    }

    private void saveRevisionItems(Long lectureId, Long courseId, String title) {
        String label = title != null ? title : "Lecture " + lectureId;
        int[] daysOut = {1, 3, 7, 14, 30};
        String[] types = {"RECALL", "FLASHCARD", "PRACTICE", "TEACH_BACK", "MIXED_TEST"};
        String[] priorities = {"HIGH", "HIGH", "MEDIUM", "MEDIUM", "LOW"};

        List<RevisionItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < daysOut.length; i++) {
            items.add(RevisionItem.builder()
                    .userId(USER_ID)
                    .lectureId(lectureId)
                    .courseId(courseId)
                    .title(label)
                    .revisionType(types[i])
                    .dueAt(now.plusDays(daysOut[i]))
                    .status("PENDING")
                    .priority(priorities[i])
                    .build());
        }
        revisionItemRepository.saveAll(items);
    }

    private void saveEvents(Long lectureId, Long courseId, List<Long> topicIds) {
        List<LearningEvent> events = new ArrayList<>();

        events.add(LearningEvent.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .courseId(courseId)
                .eventType("NOTES_GENERATED")
                .build());

        if (!topicIds.isEmpty()) {
            ArrayNode topicArray = objectMapper.createArrayNode();
            topicIds.forEach(topicArray::add);
            events.add(LearningEvent.builder()
                    .userId(USER_ID)
                    .lectureId(lectureId)
                    .courseId(courseId)
                    .eventType("TOPICS_EXTRACTED")
                    .payload(topicArray)
                    .build());
        }

        learningEventRepository.saveAll(events);
    }
}
