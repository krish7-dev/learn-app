package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.context.LearningContext;
import com.learnhowyoulearn.dto.context.MemoryContext;
import com.learnhowyoulearn.dto.request.TutorChatRequest;
import com.learnhowyoulearn.dto.response.TutorChatResponse;
import com.learnhowyoulearn.entity.LectureChatMessage;
import com.learnhowyoulearn.entity.LearningEvent;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.LectureChatMessageRepository;
import com.learnhowyoulearn.repository.LearningEventRepository;
import com.learnhowyoulearn.repository.LectureRepository;
import com.learnhowyoulearn.service.ai.AiClient;
import com.learnhowyoulearn.service.ai.AiRequest;
import com.learnhowyoulearn.service.ai.AiResponse;
import com.learnhowyoulearn.service.context.LearningContextLoaderService;
import com.learnhowyoulearn.service.memory.MemoryBuilderService;
import com.learnhowyoulearn.service.prompt.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TutorChatService {

    private static final long USER_ID = 1L;

    private final LectureRepository lectureRepository;
    private final LectureChatMessageRepository chatMessageRepository;
    private final LearningEventRepository learningEventRepository;
    private final LearningContextLoaderService contextLoaderService;
    private final MemoryBuilderService memoryBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final AiClient aiClient;
    private final AiGenerationLogService aiGenerationLogService;

    public TutorChatResponse chat(Long lectureId, TutorChatRequest request) {
        // Verify lecture exists
        lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(lectureId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));

        // 1. Save user message
        LectureChatMessage userMsg = saveMessage(lectureId, "USER", request.getMessage());

        // 2. Load context — tx closes
        LearningContext context = contextLoaderService.loadForTutorChat(USER_ID, lectureId);
        MemoryContext memoryContext = memoryBuilderService.build(context);

        // 3. Build prompt
        String combined = promptBuilderService.buildTutorChatPrompt(memoryContext, request.getMessage());
        String[] parts = promptBuilderService.splitPrompt(combined);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(parts[0])
                .userPrompt(parts[1])
                .purpose(PromptBuilderService.PURPOSE_TUTOR_CHAT)
                .temperature(0.5)
                .build();

        // 4. Call AI — no tx
        long start = System.currentTimeMillis();
        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (AiGenerationException e) {
            long latency = System.currentTimeMillis() - start;
            aiGenerationLogService.logFailed(USER_ID, lectureId,
                    context.getLecture().getCourseId(),
                    PromptBuilderService.PURPOSE_TUTOR_CHAT, "unknown", latency, e.getMessage());
            throw new AiGenerationException("Chat unavailable. Try again.");
        }

        // 5. Save assistant message + event + log
        String assistantText = aiResponse.getContent();
        LectureChatMessage assistantMsg = saveMessageAndEvent(lectureId,
                context.getLecture().getCourseId(), "ASSISTANT", assistantText);

        aiGenerationLogService.logSuccess(USER_ID, lectureId, context.getLecture().getCourseId(),
                PromptBuilderService.PURPOSE_TUTOR_CHAT, aiResponse.getModel(),
                aiResponse.getLatencyMs(), aiRequest, null);

        return TutorChatResponse.builder()
                .messageId(assistantMsg.getId())
                .role("ASSISTANT")
                .message(assistantText)
                .createdAt(assistantMsg.getCreatedAt())
                .build();
    }

    @Transactional
    protected LectureChatMessage saveMessage(Long lectureId, String role, String message) {
        return chatMessageRepository.save(LectureChatMessage.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .role(role)
                .message(message)
                .build());
    }

    @Transactional
    protected LectureChatMessage saveMessageAndEvent(Long lectureId, Long courseId, String role, String message) {
        LectureChatMessage msg = chatMessageRepository.save(LectureChatMessage.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .role(role)
                .message(message)
                .build());
        learningEventRepository.save(LearningEvent.builder()
                .userId(USER_ID)
                .lectureId(lectureId)
                .courseId(courseId)
                .eventType("TUTOR_CHAT_MESSAGE")
                .build());
        return msg;
    }
}
