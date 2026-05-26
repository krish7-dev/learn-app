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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public LectureNotes saveAll(Long lectureId, Long courseId, ParsedNotesResponse parsed, JsonNode rawAiResponse, String model) {
        LectureNotes notes = saveOrUpdateNotes(lectureId, courseId, parsed, rawAiResponse, model);
        markNotesGenerated(lectureId);
        applyModuleIfBlank(lectureId, parsed.getSuggestedModule());

        lectureTopicRepository.deleteByUserIdAndLectureId(USER_ID, lectureId);

        List<Long> topicIds = new ArrayList<>();
        if (parsed.getExtractedTopics() != null) {
            for (ExtractedTopic extracted : parsed.getExtractedTopics()) {
                if (extracted.getName() == null || extracted.getName().isBlank()) continue;
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

    private void applyModuleIfBlank(Long lectureId, String suggestedModule) {
        if (suggestedModule == null || suggestedModule.isBlank()) return;
        lectureRepository.findById(lectureId).ifPresent(lecture -> {
            if (lecture.getModuleName() == null || lecture.getModuleName().isBlank()) {
                lecture.setModuleName(suggestedModule.trim());
                lectureRepository.save(lecture);
            }
        });
    }

    private static String strip(String s) {
        if (s == null) return null;
        return s.replaceAll("\\x00", "").replace("\\u0000", "");
    }

    private JsonNode stripJson(JsonNode node) {
        if (node == null) return null;
        try {
            String json = objectMapper.writeValueAsString(node);
            return objectMapper.readTree(json.replace("\\u0000", ""));
        } catch (Exception e) {
            return node;
        }
    }

    private LectureNotes saveOrUpdateNotes(Long lectureId, Long courseId, ParsedNotesResponse parsed, JsonNode rawAiResponse, String model) {
        LectureNotes notes = lectureNotesRepository.findByUserIdAndLectureId(USER_ID, lectureId)
                .orElse(LectureNotes.builder().userId(USER_ID).lectureId(lectureId).courseId(courseId).build());
        if (model != null) notes.setModel(model);

        notes.setTitle(strip(parsed.getTitle()));
        notes.setFullCleanNotes(strip(parsed.getFullCleanNotes()));
        notes.setSimpleExplanation(strip(parsed.getSimpleExplanation()));
        notes.setPracticalUsage(strip(parsed.getPracticalUsage()));
        notes.setExamples(stripJson(parsed.getExamples()));
        notes.setMistakesToAvoid(stripJson(parsed.getMistakesToAvoid()));
        notes.setEdgeCases(stripJson(parsed.getEdgeCases()));
        notes.setRevisionNotes(strip(parsed.getRevisionNotes()));
        notes.setInterviewQuestions(stripJson(parsed.getInterviewQuestions()));
        notes.setFlashcards(stripJson(parsed.getFlashcards()));
        notes.setPracticeQuestions(stripJson(parsed.getPracticeQuestions()));
        notes.setWeakAreaChecks(stripJson(parsed.getWeakAreaChecks()));
        notes.setRawAiResponse(stripJson(rawAiResponse));

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
        String treePathJson = null;
        if (extracted.getTreePath() != null && !extracted.getTreePath().isBlank()) {
            try {
                List<String> parts = Arrays.stream(extracted.getTreePath().split(">"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList());
                treePathJson = objectMapper.writeValueAsString(parts);
            } catch (Exception ignored) {}
        }

        LectureTopic record = lectureTopicRepository
                .findByUserIdAndLectureIdAndTopicId(USER_ID, lectureId, topicId)
                .orElse(LectureTopic.builder()
                        .userId(USER_ID)
                        .lectureId(lectureId)
                        .topicId(topicId)
                        .build());

        record.setImportance(sanitizeImportance(extracted.getImportance()));
        record.setCoverageLevel(sanitizeCoverageLevel(extracted.getCoverageLevel()));
        record.setEvidence(extracted.getEvidence());
        record.setTreePath(treePathJson);
        lectureTopicRepository.save(record);
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

    private static final java.util.Set<String> VALID_IMPORTANCE = java.util.Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final java.util.Set<String> VALID_COVERAGE = java.util.Set.of("INTRO", "INTERMEDIATE", "ADVANCED");

    private String sanitizeImportance(String val) {
        if (val != null && VALID_IMPORTANCE.contains(val.toUpperCase())) return val.toUpperCase();
        return "MEDIUM";
    }

    private String sanitizeCoverageLevel(String val) {
        if (val != null && VALID_COVERAGE.contains(val.toUpperCase())) return val.toUpperCase();
        return "INTRO";
    }
}
