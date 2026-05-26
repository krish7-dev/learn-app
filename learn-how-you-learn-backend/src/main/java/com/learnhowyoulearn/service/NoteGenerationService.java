package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.dto.context.LearningContext;
import com.learnhowyoulearn.dto.context.MemoryContext;
import com.learnhowyoulearn.dto.context.ParsedNotesResponse;
import com.learnhowyoulearn.dto.request.ImportNotesBatchRequest;
import com.learnhowyoulearn.dto.response.LectureDetailResponse;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.LectureNotes;
import com.learnhowyoulearn.entity.LectureStatus;
import com.learnhowyoulearn.entity.AiGeneration;
import com.learnhowyoulearn.entity.AiGenerationStatus;
import org.springframework.transaction.annotation.Transactional;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.mapper.LectureMapper;
import com.learnhowyoulearn.repository.AiGenerationRepository;
import com.learnhowyoulearn.repository.LectureRepository;
import com.learnhowyoulearn.service.ai.AiClient;
import com.learnhowyoulearn.service.ai.AiRequest;
import com.learnhowyoulearn.service.ai.AiResponse;
import com.learnhowyoulearn.service.context.LearningContextLoaderService;
import com.learnhowyoulearn.service.memory.MemoryBuilderService;
import com.learnhowyoulearn.service.persistence.NotesPersistenceService;
import com.learnhowyoulearn.config.OpenAiConfig;
import com.learnhowyoulearn.service.prompt.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteGenerationService {

    private static final long USER_ID = 1L;
    private static final int MAX_RAW_CONTENT = 200_000;

    private final LectureRepository lectureRepository;
    private final LearningContextLoaderService contextLoaderService;
    private final MemoryBuilderService memoryBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final AiClient aiClient;
    private final AiGenerationLogService aiGenerationLogService;
    private final AiGenerationRepository aiGenerationRepository;
    private final NotesPersistenceService notesPersistenceService;
    private final LectureMapper lectureMapper;
    private final ObjectMapper objectMapper;
    private final OpenAiConfig openAiConfig;

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
                .temperature(0.2)
                .maxTokens(8000)
                .timeoutSeconds(180)
                .build();

        // 4. Call AI — no tx open during this call
        String userPromptNoTranscript = parts[1].contains("Raw lecture transcript:")
                ? parts[1].substring(0, parts[1].indexOf("Raw lecture transcript:")).trim()
                : parts[1];
        log.info("=== NoteGen REQUEST lectureId={} ===\n--- SYSTEM ---\n{}\n--- USER (no transcript) ---\n{}",
                lectureId, parts[0], userPromptNoTranscript);
        long start = System.currentTimeMillis();
        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (AiGenerationException e) {
            long latency = System.currentTimeMillis() - start;
            log.error("=== NoteGen FAILED lectureId={} latencyMs={} error={} ===", lectureId, latency, e.getMessage());
            aiGenerationLogService.logFailed(USER_ID, lectureId, lecture.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES,
                    "unknown", latency, e.getMessage());
            throw e;
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
            String preview = aiResponse.getRawResponse() != null
                    ? aiResponse.getRawResponse().substring(0, Math.min(500, aiResponse.getRawResponse().length()))
                    : "null";
            log.error("Notes parse failed for lectureId={}: {} | response preview: {}", lectureId, e.getMessage(), preview);
            aiGenerationLogService.logParseFailed(USER_ID, lectureId, lecture.getCourseId(),
                    PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES,
                    aiResponse.getModel(), aiResponse.getLatencyMs(),
                    aiResponse.getRawResponse(), e.getMessage());
            lecture.setContentStatus("PARSE_FAILED");
            lectureRepository.save(lecture);
            throw new AiGenerationException("Notes could not be generated. Try again.");
        }

        // 6. Log success
        log.info("=== NoteGen RESPONSE lectureId={} model={} latency={}ms topics={} ===\n{}",
                lectureId, aiResponse.getModel(), aiResponse.getLatencyMs(),
                parsed.getExtractedTopics() != null ? parsed.getExtractedTopics().size() : 0,
                aiResponse.getContent());
        aiGenerationLogService.logSuccess(USER_ID, lectureId, lecture.getCourseId(),
                PromptBuilderService.PURPOSE_GENERATE_LECTURE_NOTES,
                aiResponse.getModel(), aiResponse.getLatencyMs(),
                aiRequest, rawJson);

        // 7. Persist notes, topics, revision items, events
        LectureNotes savedNotes = notesPersistenceService.saveAll(lectureId, lecture.getCourseId(), parsed, rawJson, aiResponse.getModel());

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

    public LectureDetailResponse importNotes(Long lectureId, String rawContent) {
        var lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Pasted content is empty");
        }

        ParsedNotesResponse parsed;
        JsonNode rawJson;
        try {
            String cleaned = extractJson(rawContent);
            rawJson = objectMapper.readTree(cleaned);
            parsed = objectMapper.treeToValue(rawJson, ParsedNotesResponse.class);
        } catch (Exception e) {
            log.error("Import notes parse failed for lectureId={}: {}", lectureId, e.getMessage());
            throw new AiGenerationException("Could not parse the pasted content: " + e.getMessage());
        }

        LectureNotes savedNotes = notesPersistenceService.saveAll(lectureId, lecture.getCourseId(), parsed, rawJson, "imported");

        var updatedLecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));
        var response = lectureMapper.toDetail(updatedLecture);
        return LectureDetailResponse.builder()
                .id(response.getId()).courseId(response.getCourseId())
                .moduleName(response.getModuleName()).title(response.getTitle())
                .sourceName(response.getSourceName()).sourceOrder(response.getSourceOrder())
                .rawContent(response.getRawContent()).status(response.getStatus())
                .difficulty(response.getDifficulty()).lastStudiedAt(response.getLastStudiedAt())
                .createdAt(response.getCreatedAt()).updatedAt(response.getUpdatedAt())
                .notes(lectureMapper.toNotesResponse(savedNotes))
                .build();
    }

    @Transactional
    public List<LectureSummaryResponse> importNotesBatch(Long courseId, ImportNotesBatchRequest request) {
        List<LectureSummaryResponse> results = new ArrayList<>();
        List<String> contents = request.getContents();
        for (int i = 0; i < contents.size(); i++) {
            String raw = contents.get(i);
            ParsedNotesResponse parsed;
            JsonNode rawJson;
            try {
                String cleaned = extractJson(raw);
                rawJson = objectMapper.readTree(cleaned);
                parsed = objectMapper.treeToValue(rawJson, ParsedNotesResponse.class);
            } catch (Exception e) {
                log.error("Batch import parse failed for item {}: {}", i, e.getMessage());
                throw new AiGenerationException("Could not parse file " + (i + 1) + ": " + e.getMessage());
            }

            String title = parsed.getTitle() != null && !parsed.getTitle().isBlank()
                    ? parsed.getTitle() : "Lecture " + (i + 1);

            Lecture lecture = Lecture.builder()
                    .userId(USER_ID)
                    .courseId(courseId)
                    .moduleName(request.getModuleName())
                    .sourceName(request.getSourceName())
                    .title(title)
                    .sourceOrder(i + 1)
                    .status(LectureStatus.NOT_STARTED)
                    .build();
            lecture = lectureRepository.save(lecture);

            notesPersistenceService.saveAll(lecture.getId(), courseId, parsed, rawJson, "imported");

            boolean notesGenerated = true;
            results.add(lectureMapper.toSummary(lecture, notesGenerated));
        }
        return results;
    }

    public LectureDetailResponse retryParseNotes(Long lectureId) {
        var lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        AiGeneration last = aiGenerationRepository
                .findLatestByLectureIdAndStatus(lectureId, AiGenerationStatus.PARSE_FAILED)
                .orElseThrow(() -> new IllegalStateException("No failed parse response found for lecture " + lectureId));

        String rawResponse = last.getResponseJson() != null ? last.getResponseJson().asText() : null;
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("Saved response is empty");
        }

        ParsedNotesResponse parsed;
        JsonNode rawJson;
        try {
            String cleaned = extractJson(rawResponse);
            rawJson = objectMapper.readTree(cleaned);
            parsed = objectMapper.treeToValue(rawJson, ParsedNotesResponse.class);
        } catch (Exception e) {
            log.error("Retry parse also failed for lectureId={}: {}", lectureId, e.getMessage());
            throw new AiGenerationException("Retry parse failed: " + e.getMessage());
        }

        LectureNotes savedNotes = notesPersistenceService.saveAll(lectureId, lecture.getCourseId(), parsed, rawJson, null);

        var updatedLecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));
        var response = lectureMapper.toDetail(updatedLecture);
        return LectureDetailResponse.builder()
                .id(response.getId()).courseId(response.getCourseId())
                .moduleName(response.getModuleName()).title(response.getTitle())
                .sourceName(response.getSourceName()).sourceOrder(response.getSourceOrder())
                .rawContent(response.getRawContent()).status(response.getStatus())
                .difficulty(response.getDifficulty()).lastStudiedAt(response.getLastStudiedAt())
                .createdAt(response.getCreatedAt()).updatedAt(response.getUpdatedAt())
                .notes(lectureMapper.toNotesResponse(savedNotes))
                .build();
    }

    private static final String PURPOSE_CLEAN_TRANSCRIPT = "CLEAN_TRANSCRIPT";

    public LectureDetailResponse cleanTranscript(Long lectureId) {
        var lecture = lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        String rawContent = lecture.getRawContent();
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Lecture has no raw content to clean.");
        }

        String systemPrompt = """
                You are a transcript noise remover for a software engineering learning app.

                Your job is to clean a raw lecture transcript before it is sent to another AI model for final note generation.

                This is NOT final note generation.
                This is NOT a summarization task.
                This is NOT a rewriting task.

                Your main job:
                Remove unwanted transcript noise, repeated chatter, and non-learning content.
                Keep the useful technical lecture content as close to the original teaching flow as possible.

                Most important rule:
                If a sentence or paragraph teaches a technical concept, explains reasoning, gives an example, shows a derivation, compares approaches, discusses complexity, or clarifies an edge case, keep it.
                If it is classroom management, social chat, repeated confirmation, platform/setup discussion, logistics, career discussion, or duplicate repetition, remove it.

                Remove:
                - greetings and casual opening chat
                - attendance checks
                - waiting for students to join
                - screen sharing, audio/video, theme, tab, or setup issues
                - quiz logistics, quiz leaderboard, quiz timing, or quiz UI discussion
                - repeated confirmations like "quick thumbs up", "making sense?", "is this clear?", "yes/no/maybe?"
                - classroom management lines
                - instructor biography and personal background
                - company, salary, work-life, interview, or career discussion
                - course schedule, module roadmap, upcoming topics, homework, assignments, doubt-solving logistics
                - lecture recording or notes availability
                - contact details such as LinkedIn or Slack
                - thank-you or closing lines
                - repeated explanations that do not add new technical meaning

                Preserve:
                - technical definitions
                - formulas
                - code and pseudocode
                - examples with actual values
                - dry runs
                - derivations
                - step-by-step reasoning
                - comparisons between approaches
                - time complexity and space complexity discussion
                - edge cases
                - mistakes and corrections
                - important student doubts only when they clarify a technical concept
                - instructor tips and warnings directly related to the technical topic

                Duplicate handling:
                If the same explanation appears multiple times, keep only the clearest complete version and delete all repeated copies.
                If a repeated explanation adds a new example, edge case, calculation, or clarification, keep that new useful part.

                Code and math preservation:
                Preserve code and pseudocode formatting.
                Preserve indentation when possible.
                Do not remove or corrupt operators such as *, %, ==, <=, >=, //, +, -, /.
                Do not convert `i * i` into `ii`.
                Do not convert `mid * mid` into `midmid`.

                Cleanup behavior:
                Prefer deleting unwanted sentences over rewriting the whole lecture.
                Lightly rewrite only when needed for grammar, readability, or removing filler words.
                Do not collapse technical reasoning into only final conclusions.
                Do not add new explanations that were not present in the transcript.
                Do not add generated introduction lines.
                Do not add generated summary sections.
                Do not add generated conclusion sections.
                Return only the cleaned transcript text.

                Length guidance:
                Do not over-compress a full technical lecture into a tiny summary.
                After removing noise and duplicates, keep enough detail for another model to generate complete study notes.
                For long technical lectures, a cleaned output around 25%–50% of the useful technical transcript is acceptable.
                If the transcript contains repeated classroom chatter, repeated confirmations, or duplicated explanations, remove those aggressively.
                Do not target 30k tokens because final note generation also needs prompt instructions, schema/formatting rules, and output tokens.

                Accuracy guidance:
                Do not introduce unsupported claims.
                If the transcript is unclear, keep the closest supported explanation.
                For integer division by 2 until reaching 1:
                - If n = 2^k, the count is exactly k.
                - For general integer n, the count is floor(log₂(n)).
                - Do not say the answer is the greatest power of 2 itself.

                Final validation:
                Before returning, remove any remaining course/admin/career/logistics content.
                Before returning, remove any generated ending sentence like "This concludes..." or "This cleaned transcript...".
                End immediately after the last useful technical explanation.""";

        String userPrompt = """
                Remove unwanted transcript noise from the following lecture transcript before final note generation.

                Goal:
                - delete greetings, setup talk, classroom management, quiz logistics, career/admin/logistics content, and repeated confirmations
                - remove duplicate repeated explanations
                - keep the clearest complete version of each technical explanation
                - preserve technical reasoning, examples, derivations, dry runs, pseudocode, formulas, edge cases, and complexity analysis
                - preserve the original teaching flow as much as possible
                - do not summarize into final notes
                - do not add introduction, summary, or conclusion sections
                - return only the cleaned transcript text

                <transcript>
                %s
                </transcript>""".formatted(rawContent);

        String model = openAiConfig.getCleanupModel();
        AiRequest aiRequest = AiRequest.builder()
                .model(model)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .purpose(PURPOSE_CLEAN_TRANSCRIPT)
                .temperature(0.1)
                .maxTokens(openAiConfig.getCleanupMaxTokens())
                .timeoutSeconds(120)
                .build();

        log.info("=== CleanTranscript REQUEST lectureId={} model={} rawLen={} ===", lectureId, model, rawContent.length());
        long start = System.currentTimeMillis();
        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (AiGenerationException e) {
            long latency = System.currentTimeMillis() - start;
            log.error("=== CleanTranscript FAILED lectureId={} latencyMs={} error={} ===", lectureId, latency, e.getMessage());
            throw e;
        }

        String cleaned = aiResponse.getContent().trim();
        log.info("=== CleanTranscript DONE lectureId={} model={} latency={}ms rawLen={} cleanedLen={} ===",
                lectureId, aiResponse.getModel(), aiResponse.getLatencyMs(), rawContent.length(), cleaned.length());

        lecture.setRawContent(cleaned);
        lectureRepository.save(lecture);
        return lectureMapper.toDetail(lecture);
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return fixInvalidEscapes(trimmed);
    }

    /**
     * Fixes invalid JSON escape sequences by tracking parser state.
     * Inside strings: removes backslashes before non-standard escape chars (e.g. \[ \* \.).
     * Outside strings: removes any stray backslash entirely (e.g. \n before a value).
     */
    private String fixInvalidEscapes(String json) {
        StringBuilder sb = new StringBuilder(json.length());
        boolean inString = false;
        int i = 0;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == '/' || next == 'b'
                            || next == 'f' || next == 'n' || next == 'r' || next == 't') {
                        sb.append(c).append(next);
                        i += 2;
                    } else if (next == 'u' && i + 5 < json.length()) {
                        sb.append(c).append(next);
                        i += 2;
                    } else {
                        // Invalid escape inside string — drop the backslash, keep the char
                        sb.append(next);
                        i += 2;
                    }
                } else if (c == '"') {
                    inString = false;
                    sb.append(c);
                    i++;
                } else {
                    sb.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inString = true;
                    sb.append(c);
                    i++;
                } else if (c == '\\') {
                    // Stray backslash outside a string — drop it and the char that follows
                    // (e.g. \n before an opening quote: "field": \n"value" → "field": "value")
                    i++;
                    if (i < json.length() && json.charAt(i) != '"') {
                        i++; // skip escape char (n, t, r, etc.) but not a quote (that's the value start)
                    }
                } else {
                    sb.append(c);
                    i++;
                }
            }
        }
        return sb.toString();
    }
}
