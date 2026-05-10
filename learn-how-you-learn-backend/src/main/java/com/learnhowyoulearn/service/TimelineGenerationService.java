package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.dto.context.ParsedTimeline;
import com.learnhowyoulearn.dto.context.TimelineGenerationContext;
import com.learnhowyoulearn.dto.response.WeekPlanResponse;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.service.ai.AiClient;
import com.learnhowyoulearn.service.ai.AiRequest;
import com.learnhowyoulearn.service.ai.AiResponse;
import com.learnhowyoulearn.service.persistence.TimelinePersistenceService;
import com.learnhowyoulearn.service.prompt.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineGenerationService {

    private static final long USER_ID = 1L;

    private final TimelineContextLoaderService contextLoaderService;
    private final PromptBuilderService promptBuilderService;
    private final AiClient aiClient;
    private final AiGenerationLogService aiGenerationLogService;
    private final TimelinePersistenceService timelinePersistenceService;
    private final StudyTimelineService studyTimelineService;
    private final ObjectMapper objectMapper;

    // No @Transactional — isolation by design: each step manages its own TX boundary
    public WeekPlanResponse generateTimeline(Long targetId) {
        // Step 1: load context (readOnly TX closes on return)
        TimelineGenerationContext ctx = contextLoaderService.loadForTimeline(targetId);
        LearningTarget target = ctx.getTarget();

        // Step 2: build prompt
        String combined = promptBuilderService.buildTimelinePrompt(ctx);
        String[] parts = promptBuilderService.splitPrompt(combined);
        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(parts[0])
                .userPrompt(parts[1])
                .purpose(PromptBuilderService.PURPOSE_GENERATE_TIMELINE)
                .temperature(0.4)
                .build();

        // Step 3: AI call — no TX open
        long start = System.currentTimeMillis();
        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (AiGenerationException e) {
            long latency = System.currentTimeMillis() - start;
            aiGenerationLogService.logFailed(USER_ID, null, target.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_TIMELINE, "unknown", latency, e.getMessage());
            throw new AiGenerationException("Timeline could not be generated. Try again.");
        }

        // Step 4: parse JSON — if parse fails, old items are untouched
        ParsedTimeline parsed;
        JsonNode rawJson;
        try {
            String cleaned = extractJson(aiResponse.getContent());
            rawJson = objectMapper.readTree(cleaned);
            parsed = objectMapper.treeToValue(rawJson, ParsedTimeline.class);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            aiGenerationLogService.logParseFailed(USER_ID, null, target.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_TIMELINE,
                    aiResponse.getModel(), latency,
                    aiResponse.getRawResponse(), e.getMessage());
            throw new AiGenerationException("Timeline response could not be parsed. Try again.");
        }

        // Step 5: log success (REQUIRES_NEW TX)
        aiGenerationLogService.logSuccess(USER_ID, null, target.getCourseId(),
                PromptBuilderService.PURPOSE_GENERATE_TIMELINE,
                aiResponse.getModel(), aiResponse.getLatencyMs(), aiRequest, rawJson);

        // Step 6: delete regeneratable items + insert new ones (write TX) — ONLY after successful parse
        timelinePersistenceService.saveTimeline(targetId, parsed);

        // Step 7: return week view (readOnly TX)
        return studyTimelineService.getWeek(targetId);
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
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
