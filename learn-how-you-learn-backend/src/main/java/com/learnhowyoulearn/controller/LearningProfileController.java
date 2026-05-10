package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.UpdateLearningProfileRequest;
import com.learnhowyoulearn.dto.response.LearningProfileResponse;
import com.learnhowyoulearn.service.LearningProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class LearningProfileController {

    private final LearningProfileService learningProfileService;

    @GetMapping
    public LearningProfileResponse get() {
        return learningProfileService.getProfile();
    }

    @PutMapping
    public LearningProfileResponse update(@RequestBody UpdateLearningProfileRequest request) {
        return learningProfileService.update(request);
    }
}
