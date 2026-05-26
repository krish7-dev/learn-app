package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.dto.context.ParsedTimeline;
import com.learnhowyoulearn.dto.context.TimelineGenerationContext;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.exception.TimelineValidationException;
import com.learnhowyoulearn.service.ai.AiClient;
import com.learnhowyoulearn.service.ai.AiRequest;
import com.learnhowyoulearn.service.ai.AiResponse;
import com.learnhowyoulearn.service.persistence.TimelinePersistenceService;
import com.learnhowyoulearn.service.prompt.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineGenerationService {

    private static final long USER_ID = 1L;
    private static final int MAX_ITEMS_PER_DAY = 4;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "STUDY_LECTURE", "REVISION", "PRACTICE"
    );

    private final TimelineContextLoaderService contextLoaderService;
    private final PromptBuilderService promptBuilderService;
    private final AiClient aiClient;
    private final AiGenerationLogService aiGenerationLogService;
    private final TimelinePersistenceService timelinePersistenceService;
    private final TimelineGenerationStatusService statusService;
    private final ObjectMapper objectMapper;

    public String previewAiPlan(String exportText) {
        log.info("ASK-AI REQUEST — {} chars", exportText.length());
        log.debug("ASK-AI REQUEST BODY:\n{}", exportText);
        AiRequest request = AiRequest.builder()
                .systemPrompt("You are a study planner. Generate a day-by-day schedule from the given lecture list. Return ONLY valid JSON. No explanation. No markdown.")
                .userPrompt(exportText)
                .purpose("PREVIEW_AI_PLAN")
                .temperature(0.3)
                .timeoutSeconds(120)
                .maxTokens(4000)
                .build();
        AiResponse response = aiClient.generate(request);
        log.info("ASK-AI RESPONSE — model={}, latencyMs={}, {} chars", response.getModel(), response.getLatencyMs(), response.getContent().length());
        log.debug("ASK-AI RESPONSE BODY:\n{}", response.getContent());

        LocalDate today = LocalDate.now();
        LocalDate deadline = extractDeadline(exportText);
        int dailyBudget = extractDailyBudget(exportText);
        String returnContent = response.getContent();
        try {
            String cleaned = extractJson(response.getContent());
            ParsedTimeline parsed = objectMapper.treeToValue(objectMapper.readTree(cleaned), ParsedTimeline.class);
            ParsedTimeline repaired = repairBudgetOverruns(parsed, dailyBudget);
            repaired = filterAfterDeadline(repaired, today, deadline);
            returnContent = objectMapper.writeValueAsString(repaired);
            log.info("ASK-AI REPAIR — returning repaired plan");
        } catch (Exception e) {
            log.warn("ASK-AI REPAIR — could not apply repair, returning original: {}", e.getMessage());
        }

        validatePreviewResponse(returnContent, dailyBudget);
        return returnContent;
    }

    private int extractDailyBudget(String exportText) {
        for (String line : exportText.split("\n")) {
            if (line.startsWith("DAILY_BUDGET_MINUTES:")) {
                try { return Integer.parseInt(line.split(":")[1].trim()); } catch (Exception ignored) {}
            }
        }
        return 60;
    }

    private LocalDate extractDeadline(String exportText) {
        for (String line : exportText.split("\n")) {
            if (line.startsWith("DEADLINE:")) {
                try { return LocalDate.parse(line.split(":", 2)[1].trim()); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private ParsedTimeline filterAfterDeadline(ParsedTimeline parsed, LocalDate today, LocalDate deadline) {
        if (parsed == null || parsed.getDays() == null || deadline == null) return parsed;
        int before = parsed.getDays().size();
        List<ParsedTimeline.ParsedDay> kept = new ArrayList<>();
        for (ParsedTimeline.ParsedDay day : parsed.getDays()) {
            try {
                LocalDate date = LocalDate.parse(day.getDate());
                if (!date.isBefore(today) && !date.isAfter(deadline)) kept.add(day);
            } catch (Exception ignored) {}
        }
        if (kept.size() != before) {
            log.info("ASK-AI DEADLINE FILTER — removed {} days outside {} to {}", before - kept.size(), today, deadline);
        }
        ParsedTimeline result = new ParsedTimeline();
        result.setDays(kept);
        return result;
    }

    private void validatePreviewResponse(String content, int dailyBudgetMinutes) {
        try {
            String cleaned = extractJson(content);
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode days = root.path("days");
            if (!days.isArray()) {
                log.warn("ASK-AI VALIDATION — response missing 'days' array");
                return;
            }
            int totalItems = 0;
            int daysWithBudgetExceeded = 0;
            int invalidLectureIds = 0;
            for (JsonNode day : days) {
                JsonNode items = day.path("items");
                if (!items.isArray()) continue;
                int dayMinutes = 0;
                for (JsonNode item : items) {
                    totalItems++;
                    dayMinutes += item.path("estimatedMinutes").asInt(0);
                    JsonNode lid = item.path("lectureId");
                    if (!lid.isNull() && !lid.isMissingNode() && !lid.isNumber()) {
                        log.warn("ASK-AI VALIDATION — non-numeric lectureId on day {}: '{}'",
                                day.path("date").asText(), lid.asText());
                        invalidLectureIds++;
                    }
                }
                if (dayMinutes > dailyBudgetMinutes) {
                    log.warn("ASK-AI VALIDATION — day {} exceeds budget: {} min (limit {})",
                            day.path("date").asText(), dayMinutes, dailyBudgetMinutes);
                    daysWithBudgetExceeded++;
                }
            }
            log.info("ASK-AI VALIDATION — {} days, {} total items, {} days over budget, {} invalid lectureIds",
                    days.size(), totalItems, daysWithBudgetExceeded, invalidLectureIds);
        } catch (Exception e) {
            log.warn("ASK-AI VALIDATION — could not parse response as JSON: {}", e.getMessage());
        }
    }

    @Async
    public void generateTimelineAsync(Long targetId) {
        log.info("ASYNC START — targetId={}, thread={}", targetId, Thread.currentThread().getName());
        statusService.setGenerating(targetId);
        try {
            generateTimeline(targetId);
            log.info("ASYNC DONE — calling setDone for targetId={}", targetId);
            statusService.setDone(targetId);
            log.info("ASYNC DONE — setDone complete for targetId={}", targetId);
        } catch (Exception e) {
            log.error("ASYNC ERROR — targetId={}: {}", targetId, e.getMessage(), e);
            statusService.setError(targetId, e.getMessage());
        }
    }

    // No @Transactional — isolation by design: each step manages its own TX boundary
    public void generateTimeline(Long targetId) {
        // Step 1: load context (readOnly TX closes on return)
        TimelineGenerationContext ctx = contextLoaderService.loadForTimeline(targetId);
        LearningTarget target = ctx.getTarget();

        LocalDate today = LocalDate.now();
        LocalDate targetDate = target.getTargetDate();
        int dailyBudgetMinutes = target.getDailyMinutes();

        List<ParsedTimeline.ParsedDay> allDays = new ArrayList<>();
        Set<Long> plannedLectureIds = new HashSet<>();
        AiResponse lastSuccessfulResponse = null;
        AiRequest lastSuccessfulRequest = null;

        // Step 2: loop through 14-day chunks covering today → targetDate
        LocalDate chunkStart = today;
        int chunkIndex = 0;
        while (!chunkStart.isAfter(targetDate)) {
            LocalDate chunkEnd = chunkStart.plusDays(13).isBefore(targetDate)
                    ? chunkStart.plusDays(13) : targetDate;
            int chunkWindowDays = (int) ChronoUnit.DAYS.between(chunkStart, chunkEnd) + 1;
            int maxTotalItems = chunkWindowDays * MAX_ITEMS_PER_DAY;

            String combined = promptBuilderService.buildTimelinePrompt(
                    ctx, chunkStart, chunkEnd, MAX_ITEMS_PER_DAY, maxTotalItems, plannedLectureIds);
            String[] parts = promptBuilderService.splitPrompt(combined);
            log.info("TIMELINE CHUNK {} — window: {} to {}, days: {}, maxItems: {}, alreadyPlanned: {}",
                    chunkIndex, chunkStart, chunkEnd, chunkWindowDays, maxTotalItems, plannedLectureIds.size());

            AiRequest aiRequest = AiRequest.builder()
                    .systemPrompt(parts[0])
                    .userPrompt(parts[1])
                    .purpose(PromptBuilderService.PURPOSE_GENERATE_TIMELINE)
                    .temperature(0.4)
                    .timeoutSeconds(120)
                    .maxTokens(4000)
                    .build();

            long start = System.currentTimeMillis();
            AiResponse aiResponse;
            try {
                aiResponse = aiClient.generate(aiRequest);
            } catch (AiGenerationException e) {
                long latency = System.currentTimeMillis() - start;
                aiGenerationLogService.logFailed(USER_ID, null, target.getCourseId(),
                        PromptBuilderService.PURPOSE_GENERATE_TIMELINE, "unknown", latency, e.getMessage());
                throw new AiGenerationException("Timeline chunk " + chunkIndex + " could not be generated. Try again.");
            }

            log.info("TIMELINE CHUNK {} RESPONSE — {} chars", chunkIndex, aiResponse.getContent().length());

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
                        aiResponse.getModel(), latency, aiResponse.getRawResponse(), e.getMessage());
                throw new AiGenerationException("Timeline chunk " + chunkIndex + " could not be parsed. Try again.");
            }

            // Deterministic repair first — removes over-budget REVISION/PRACTICE before structural validation
            parsed = repairBudgetOverruns(parsed, dailyBudgetMinutes);

            try {
                validate(parsed, chunkStart, chunkEnd, MAX_ITEMS_PER_DAY, maxTotalItems, dailyBudgetMinutes);
            } catch (TimelineValidationException e) {
                log.warn("Chunk {} validation failed ({}), attempting AI repair", chunkIndex, e.getMessage());
                parsed = repairTimeline(aiResponse.getContent(), chunkStart, chunkEnd,
                        MAX_ITEMS_PER_DAY, maxTotalItems, dailyBudgetMinutes, target.getCourseId());
            }

            allDays.addAll(parsed.getDays());

            // track which lectures were scheduled so the next chunk can skip them
            parsed.getDays().stream()
                    .flatMap(d -> d.getItems().stream())
                    .filter(i -> i.getLectureId() != null)
                    .map(ParsedTimeline.ParsedItem::getLectureId)
                    .forEach(plannedLectureIds::add);

            lastSuccessfulResponse = aiResponse;
            lastSuccessfulRequest = aiRequest;
            chunkStart = chunkEnd.plusDays(1);
            chunkIndex++;
        }

        // Step 3: log success for the overall generation
        if (lastSuccessfulResponse != null) {
            aiGenerationLogService.logSuccess(USER_ID, null, target.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_TIMELINE,
                    lastSuccessfulResponse.getModel(), lastSuccessfulResponse.getLatencyMs(),
                    lastSuccessfulRequest, null);
        }

        // Step 4: merge all chunks and save once, clearing today → targetDate
        ParsedTimeline merged = new ParsedTimeline();
        merged.setDays(allDays);
        timelinePersistenceService.saveTimeline(targetId, merged, targetDate);

        log.info("TIMELINE COMPLETE — {} chunks, {} total days saved", chunkIndex, allDays.size());
    }

    private ParsedTimeline repairTimeline(String originalContent, LocalDate today, LocalDate planUntil,
                                          int maxItemsPerDay, int maxTotalItems,
                                          int dailyBudgetMinutes, Long courseId) {
        String repairSystem = String.format(
                "You are a JSON repair assistant. Fix the JSON to satisfy these constraints:\n" +
                "- Max %d items per day.\n" +
                "- Max %d total items.\n" +
                "- Each item estimatedMinutes between 5 and 60.\n" +
                "- Each day total minutes <= %d.\n" +
                "- Dates only between %s and %s.\n" +
                "- Each date appears exactly once.\n" +
                "- Do not add new lecture items that were not present in the invalid JSON.\n" +
                "- Only remove, trim, or redistribute existing items if needed.\n" +
                "- You may add missing dates with empty items arrays if required " +
                "(e.g. {\"date\":\"YYYY-MM-DD\",\"items\":[]}).\n" +
                "Return STRICT valid JSON only. Same structure.",
                maxItemsPerDay, maxTotalItems, dailyBudgetMinutes, today, planUntil);

        AiRequest repairRequest = AiRequest.builder()
                .systemPrompt(repairSystem)
                .userPrompt("Invalid JSON:\n" + originalContent)
                .purpose(PromptBuilderService.PURPOSE_GENERATE_TIMELINE)
                .temperature(0.0)
                .timeoutSeconds(60)
                .maxTokens(4000)
                .build();

        AiResponse repairResponse;
        try {
            repairResponse = aiClient.generate(repairRequest);
        } catch (AiGenerationException e) {
            throw new AiGenerationException("Timeline violated constraints and repair call failed. Try again.");
        }

        log.info("TIMELINE REPAIR RESPONSE — {} chars", repairResponse.getContent().length());

        ParsedTimeline repaired;
        try {
            String cleaned = extractJson(repairResponse.getContent());
            repaired = objectMapper.treeToValue(objectMapper.readTree(cleaned), ParsedTimeline.class);
        } catch (Exception e) {
            throw new AiGenerationException("Timeline repair response could not be parsed. Try again.");
        }

        try {
            validate(repaired, today, planUntil, maxItemsPerDay, maxTotalItems, dailyBudgetMinutes);
        } catch (TimelineValidationException e) {
            log.error("Timeline repair still invalid: {}", e.getMessage());
            throw new AiGenerationException("Timeline violated constraints after retry. Try again.");
        }

        return repaired;
    }

    private ParsedTimeline repairBudgetOverruns(ParsedTimeline parsed, int dailyBudgetMinutes) {
        if (parsed == null || parsed.getDays() == null) return parsed;
        for (ParsedTimeline.ParsedDay day : parsed.getDays()) {
            if (day.getItems() == null || day.getItems().isEmpty()) continue;

            int before = day.getItems().stream().mapToInt(ParsedTimeline.ParsedItem::getEstimatedMinutes).sum();
            if (before <= dailyBudgetMinutes) continue;

            log.warn("BUDGET REPAIR — day {} total {} min > limit {} min", day.getDate(), before, dailyBudgetMinutes);
            List<ParsedTimeline.ParsedItem> items = new ArrayList<>(day.getItems());

            // 1. Remove REVISION first
            int total = before;
            if (total > dailyBudgetMinutes) {
                items.removeIf(i -> "REVISION".equals(i.getItemType()));
                total = items.stream().mapToInt(ParsedTimeline.ParsedItem::getEstimatedMinutes).sum();
            }

            // 2. Remove PRACTICE if still over
            if (total > dailyBudgetMinutes) {
                items.removeIf(i -> "PRACTICE".equals(i.getItemType()));
                total = items.stream().mapToInt(ParsedTimeline.ParsedItem::getEstimatedMinutes).sum();
            }

            // 3. Trim or remove last STUDY_LECTURE if still over
            if (total > dailyBudgetMinutes) {
                for (int i = items.size() - 1; i >= 0; i--) {
                    ParsedTimeline.ParsedItem item = items.get(i);
                    if ("STUDY_LECTURE".equals(item.getItemType())) {
                        int trimmed = item.getEstimatedMinutes() - (total - dailyBudgetMinutes);
                        if (trimmed >= 30) {
                            item.setEstimatedMinutes(trimmed);
                        } else {
                            items.remove(i);
                        }
                        break;
                    }
                }
            }

            int after = items.stream().mapToInt(ParsedTimeline.ParsedItem::getEstimatedMinutes).sum();
            log.info("BUDGET REPAIR — day {} repaired: {} min → {} min ({} items)", day.getDate(), before, after, items.size());
            day.setItems(items);
        }
        return parsed;
    }

    private void validate(ParsedTimeline parsed, LocalDate today, LocalDate planUntil,
                          int maxItemsPerDay, int maxTotalItems, int dailyBudgetMinutes) {
        if (parsed == null || parsed.getDays() == null)
            throw new TimelineValidationException("Timeline days cannot be null");

        int totalItems = 0;
        Set<String> seenDates = new HashSet<>();

        for (ParsedTimeline.ParsedDay day : parsed.getDays()) {
            LocalDate date;
            try {
                date = LocalDate.parse(day.getDate());
            } catch (DateTimeParseException e) {
                throw new TimelineValidationException("Invalid date format: " + day.getDate());
            }

            if (date.isBefore(today) || date.isAfter(planUntil))
                throw new TimelineValidationException("Date outside plan window: " + day.getDate());

            if (!seenDates.add(day.getDate()))
                throw new TimelineValidationException("Duplicate date: " + day.getDate());

            if (day.getItems() == null)
                throw new TimelineValidationException("Items cannot be null for date: " + day.getDate());

            if (day.getItems().size() > maxItemsPerDay)
                throw new TimelineValidationException("Too many items for date: " + day.getDate());

            int dayMinutes = 0;
            for (ParsedTimeline.ParsedItem item : day.getItems()) {
                if (!ALLOWED_TYPES.contains(item.getItemType()))
                    throw new TimelineValidationException("Invalid item type: " + item.getItemType());
                if (item.getEstimatedMinutes() < 5 || item.getEstimatedMinutes() > 60)
                    throw new TimelineValidationException("Invalid item duration: " + item.getEstimatedMinutes());
                dayMinutes += item.getEstimatedMinutes();
            }

            if (dayMinutes > dailyBudgetMinutes)
                throw new TimelineValidationException("Day exceeds budget: " + day.getDate());

            totalItems += day.getItems().size();
        }

        if (totalItems > maxTotalItems)
            throw new TimelineValidationException("Timeline exceeds max total items");

        LocalDate d = today;
        while (!d.isAfter(planUntil)) {
            if (!seenDates.contains(d.toString()))
                throw new TimelineValidationException("Missing date in timeline: " + d);
            d = d.plusDays(1);
        }
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
