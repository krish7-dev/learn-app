package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.dto.context.LearningContext;
import com.learnhowyoulearn.dto.context.MemoryContext;
import com.learnhowyoulearn.dto.request.TeachBackRequest;
import com.learnhowyoulearn.dto.response.TeachBackResponse;
import com.learnhowyoulearn.entity.LearningEvent;
import com.learnhowyoulearn.entity.Topic;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.repository.LearningEventRepository;
import com.learnhowyoulearn.repository.TopicRepository;
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
public class TeachBackService {

    private static final long USER_ID = 1L;

    private final TopicRepository topicRepository;
    private final LearningEventRepository learningEventRepository;
    private final LearningContextLoaderService contextLoaderService;
    private final MemoryBuilderService memoryBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final AiClient aiClient;
    private final AiGenerationLogService aiGenerationLogService;
    private final ObjectMapper objectMapper;

    public TeachBackResponse evaluate(Long topicId, TeachBackRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        // Build a lightweight memory context — no lecture needed
        MemoryContext memoryContext = MemoryContext.builder()
                .courseTitle(null)
                .lectureTitle(topic.getName())
                .rawContent(null)
                .preferredStyles(java.util.Collections.emptyList())
                .struggles(java.util.Collections.emptyList())
                .tonePreference(null)
                .learningGoals(java.util.Collections.emptyList())
                .activeWeakAreas(java.util.Collections.emptyList())
                .recentConfusions(java.util.Collections.emptyList())
                .existingTopics(java.util.Collections.emptyList())
                .recentChatMessages(java.util.Collections.emptyList())
                .build();

        String combined = promptBuilderService.buildTeachBackPrompt(memoryContext, topic.getName(), request.getExplanation());
        String[] parts = promptBuilderService.splitPrompt(combined);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(parts[0])
                .userPrompt(parts[1])
                .purpose(PromptBuilderService.PURPOSE_TEACH_BACK_ANALYSIS)
                .temperature(0.3)
                .build();

        long start = System.currentTimeMillis();
        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (AiGenerationException e) {
            long latency = System.currentTimeMillis() - start;
            aiGenerationLogService.logFailed(USER_ID, null, null,
                    PromptBuilderService.PURPOSE_TEACH_BACK_ANALYSIS, "unknown", latency, e.getMessage());
            throw new AiGenerationException("Evaluation unavailable. Try again.");
        }

        TeachBackResponse result;
        try {
            String content = aiResponse.getContent().trim();
            if (content.startsWith("```")) {
                int nl = content.indexOf('\n');
                int last = content.lastIndexOf("```");
                if (nl > 0 && last > nl) content = content.substring(nl + 1, last).trim();
            }
            result = objectMapper.readValue(content, TeachBackResponse.class);
        } catch (Exception e) {
            aiGenerationLogService.logParseFailed(USER_ID, null, null,
                    PromptBuilderService.PURPOSE_TEACH_BACK_ANALYSIS,
                    aiResponse.getModel(), aiResponse.getLatencyMs(),
                    aiResponse.getRawResponse(), e.getMessage());
            throw new AiGenerationException("Evaluation could not be parsed. Try again.");
        }

        saveEvent(topicId);
        aiGenerationLogService.logSuccess(USER_ID, null, null,
                PromptBuilderService.PURPOSE_TEACH_BACK_ANALYSIS,
                aiResponse.getModel(), aiResponse.getLatencyMs(), aiRequest, null);

        return result;
    }

    @Transactional
    protected void saveEvent(Long topicId) {
        learningEventRepository.save(LearningEvent.builder()
                .userId(USER_ID)
                .topicId(topicId)
                .eventType("TEACH_BACK_SUBMITTED")
                .build());
    }
}
