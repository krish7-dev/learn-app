package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.TeachBackRequest;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.dto.response.TeachBackResponse;
import com.learnhowyoulearn.dto.response.TopicDetailResponse;
import com.learnhowyoulearn.dto.response.TopicSummaryResponse;
import com.learnhowyoulearn.service.TeachBackService;
import com.learnhowyoulearn.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final TeachBackService teachBackService;

    @GetMapping
    public PageResponse<TopicSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return topicService.list(page, size);
    }

    @GetMapping("/{id}")
    public TopicDetailResponse getById(@PathVariable Long id) {
        return topicService.getById(id);
    }

    @PostMapping("/{id}/teach-back")
    public TeachBackResponse teachBack(@PathVariable Long id,
                                       @Valid @RequestBody TeachBackRequest request) {
        return teachBackService.evaluate(id, request);
    }

    @DeleteMapping("/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        topicService.delete(id);
    }
}
