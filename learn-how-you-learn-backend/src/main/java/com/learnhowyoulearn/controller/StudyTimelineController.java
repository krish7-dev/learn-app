package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.UpdateTimelineItemRequest;
import com.learnhowyoulearn.dto.response.StudyTimelineItemResponse;
import com.learnhowyoulearn.service.StudyTimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/timeline")
@RequiredArgsConstructor
public class StudyTimelineController {

    private final StudyTimelineService studyTimelineService;

    @PutMapping("/{itemId}")
    public StudyTimelineItemResponse markItem(@PathVariable Long itemId,
                                              @Valid @RequestBody UpdateTimelineItemRequest request) {
        return studyTimelineService.markItem(itemId, request);
    }
}
