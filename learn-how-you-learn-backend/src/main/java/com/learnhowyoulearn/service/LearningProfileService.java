package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.UpdateLearningProfileRequest;
import com.learnhowyoulearn.dto.response.LearningProfileResponse;
import com.learnhowyoulearn.entity.LearningProfile;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.mapper.LearningProfileMapper;
import com.learnhowyoulearn.repository.LearningProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningProfileService {

    private static final long USER_ID = 1L;

    private final LearningProfileRepository learningProfileRepository;
    private final LearningProfileMapper learningProfileMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultProfileIfMissing() {
        if (!learningProfileRepository.existsByUserId(USER_ID)) {
            log.info("No learning profile found for user {}. Seeding default profile.", USER_ID);
            LearningProfile profile = LearningProfile.builder()
                    .userId(USER_ID)
                    .preferredExplanationStyles(List.of(
                            "step_by_step", "code_first", "dry_run",
                            "examples", "practical_usage", "mistake_based_learning"))
                    .struggles(List.of(
                            "long_abstract_theory", "unstructured_topics",
                            "restarting_after_gap", "forgetting_revision",
                            "too_many_pending_topics", "losing_context_after_break"))
                    .tonePreference("direct, friendly, motivating, bro-style, technically clear")
                    .learningGoals(List.of(
                            "Complete DSA for interviews",
                            "Build LLD/HLD knowledge",
                            "Master SQL"))
                    .build();
            learningProfileRepository.save(profile);
        }
    }

    @Cacheable("learningProfile")
    @Transactional(readOnly = true)
    public LearningProfileResponse getProfile() {
        return learningProfileMapper.toResponse(findOrThrow());
    }

    @CacheEvict(value = "learningProfile", allEntries = true)
    @Transactional
    public LearningProfileResponse update(UpdateLearningProfileRequest request) {
        LearningProfile profile = findOrThrow();
        if (request.getPreferredExplanationStyles() != null)
            profile.setPreferredExplanationStyles(request.getPreferredExplanationStyles());
        if (request.getStruggles() != null)
            profile.setStruggles(request.getStruggles());
        if (request.getTonePreference() != null)
            profile.setTonePreference(request.getTonePreference());
        if (request.getLearningGoals() != null)
            profile.setLearningGoals(request.getLearningGoals());
        return learningProfileMapper.toResponse(learningProfileRepository.save(profile));
    }

    private LearningProfile findOrThrow() {
        return learningProfileRepository.findByUserId(USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Learning profile not found for user: " + USER_ID));
    }
}
