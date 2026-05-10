package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.dto.context.LearningContext;
import com.learnhowyoulearn.dto.context.MemoryContext;
import com.learnhowyoulearn.dto.context.ParsedNotesResponse;
import com.learnhowyoulearn.dto.response.LectureDetailResponse;
import com.learnhowyoulearn.entity.LectureNotes;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.mapper.LectureMapper;
import com.learnhowyoulearn.repository.LectureRepository;
import com.learnhowyoulearn.service.ai.AiClient;
import com.learnhowyoulearn.service.ai.AiRequest;
import com.learnhowyoulearn.service.ai.AiResponse;
import com.learnhowyoulearn.service.context.LearningContextLoaderService;
import com.learnhowyoulearn.service.memory.MemoryBuilderService;
import com.learnhowyoulearn.service.persistence.NotesPersistenceService;
import com.learnhowyoulearn.service.prompt.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteGenerationService {

    private static final long USER_ID = 1L;
    private static final int MAX_RAW_CONTENT = 50_000;

    private final LectureRepository lectureRepository;
    private final LearningContextLoaderService contextLoaderService;
    private final MemoryBuilderService memoryBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final AiClient aiClient;
    private final AiGenerationLogService aiGenerationLogService;
    private final NotesPersistenceService notesPersistenceService;
    private final LectureMapper lectureMapper;
    private final ObjectMapper objectMapper;

    public LectureDetailResponse generateNotes(Long lectureId) {
        // 1. Validate lecture + raw content size (read-only, context loader handles the tx)
        var lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        String rawContent = lecture.getRawContent();
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Lecture has no raw content. Add transcript before generating notes.");
        }
        if (rawContent.length() > MAX_RAW_CONTENT) {
            throw new IllegalArgumentException("Content too large. Please split into multiple lectures.");
        }

        // 2. Load context — tx closes after this call
        LearningContext context = contextLoaderService.loadForLectureNoteGeneration(USER_ID, lectureId);
        MemoryContext memoryContext = memoryBuilderService.build(context);

        // 3. Build prompt
        String combined = promptBuilderService.buildGenerateNotesPrompt(memoryContext);
        String[] parts = promptBuilderService.splitPrompt(combined);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(parts[0])
                .userPrompt(parts[1])
                .purpose(PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES)
                .temperature(0.3)
                .build();

        // 4. Call AI — no tx open during this call
        long start = System.currentTimeMillis();
        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (AiGenerationException e) {
            long latency = System.currentTimeMillis() - start;
            aiGenerationLogService.logFailed(USER_ID, lectureId, lecture.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES,
                    "unknown", latency, e.getMessage());
            throw new AiGenerationException("Notes could not be generated. Try again.");
        }

        // 5. Parse JSON
        ParsedNotesResponse parsed;
        JsonNode rawJson;
        try {
            String content = aiResponse.getContent();
            String cleaned = extractJson(content);
            rawJson = objectMapper.readTree(cleaned);
            parsed = objectMapper.treeToValue(rawJson, ParsedNotesResponse.class);
        } catch (Exception e) {
            aiGenerationLogService.logParseFailed(USER_ID, lectureId, lecture.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES,
                    aiResponse.getModel(), aiResponse.getLatencyMs(),
                    aiResponse.getRawResponse(), e.getMessage());
            throw new AiGenerationException("Notes could not be generated. Try again.");
        }

        // 6. Log success
        aiGenerationLogService.logSuccess(USER_ID, lectureId, lecture.getCourseId(),
                PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES,
                aiResponse.getModel(), aiResponse.getLatencyMs(),
                aiRequest, rawJson);

        // 7. Persist notes, topics, revision items, events
        LectureNotes savedNotes = notesPersistenceService.saveAll(lectureId, lecture.getCourseId(), parsed, rawJson);

        // 8. Return response
        var updatedLecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));
        LectureDetailResponse response = lectureMapper.toDetail(updatedLecture);
        return LectureDetailResponse.builder()
                .id(response.getId())
                .courseId(response.getCourseId())
                .moduleName(response.getModuleName())
                .title(response.getTitle())
                .sourceName(response.getSourceName())
                .sourceOrder(response.getSourceOrder())
                .rawContent(response.getRawContent())
                .status(response.getStatus())
                .difficulty(response.getDifficulty())
                .lastStudiedAt(response.getLastStudiedAt())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .notes(lectureMapper.toNotesResponse(savedNotes))
                .build();
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
        // Strip markdown code fences if AI wrapped the JSON
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
