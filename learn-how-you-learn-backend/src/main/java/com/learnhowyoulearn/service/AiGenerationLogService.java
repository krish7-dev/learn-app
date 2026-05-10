package com.learnhowyoulearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.learnhowyoulearn.entity.AiGeneration;
import com.learnhowyoulearn.entity.AiGenerationStatus;
import com.learnhowyoulearn.repository.AiGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationLogService {

    private final AiGenerationRepository aiGenerationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiGeneration logSuccess(Long userId, Long lectureId, Long courseId, String purpose,
                                   String model, long latencyMs,
                                   Object requestPayload, JsonNode responseJson) {
        return save(AiGeneration.builder()
                .userId(userId)
                .lectureId(lectureId)
                .courseId(courseId)
                .purpose(purpose)
                .model(model)
                .status(AiGenerationStatus.SUCCESS)
                .latencyMs(latencyMs)
                .requestJson(toJson(requestPayload))
                .responseJson(responseJson)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiGeneration logFailed(Long userId, Long lectureId, Long courseId, String purpose,
                                   String model, long latencyMs, String errorMessage) {
        return save(AiGeneration.builder()
                .userId(userId)
                .lectureId(lectureId)
                .courseId(courseId)
                .purpose(purpose)
                .model(model)
                .status(AiGenerationStatus.FAILED)
                .latencyMs(latencyMs)
                .errorMessage(errorMessage)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiGeneration logParseFailed(Long userId, Long lectureId, Long courseId, String purpose,
                                        String model, long latencyMs,
                                        String rawResponse, String parseError) {
        return save(AiGeneration.builder()
                .userId(userId)
                .lectureId(lectureId)
                .courseId(courseId)
                .purpose(purpose)
                .model(model)
                .status(AiGenerationStatus.PARSE_FAILED)
                .latencyMs(latencyMs)
                .responseJson(TextNode.valueOf(rawResponse))
                .errorMessage(parseError)
                .build());
    }

    private AiGeneration save(AiGeneration gen) {
        try {
            return aiGenerationRepository.save(gen);
        } catch (Exception e) {
            log.error("Failed to persist ai_generation log: {}", e.getMessage());
            return gen;
        }
    }

    private JsonNode toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.valueToTree(obj);
        } catch (Exception e) {
            return TextNode.valueOf(obj.toString());
        }
    }
}
