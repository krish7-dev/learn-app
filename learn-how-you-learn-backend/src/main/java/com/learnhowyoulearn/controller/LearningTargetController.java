package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.CreateTargetRequest;
import com.learnhowyoulearn.dto.request.UpdateTargetRequest;
import com.learnhowyoulearn.dto.response.DayPlanResponse;
import com.learnhowyoulearn.dto.response.LearningTargetResponse;
import com.learnhowyoulearn.dto.response.WeekPlanResponse;
import com.learnhowyoulearn.service.LearningTargetService;
import com.learnhowyoulearn.service.StudyTimelineService;
import com.learnhowyoulearn.service.TimelineGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/targets")
@RequiredArgsConstructor
public class LearningTargetController {

    private final LearningTargetService learningTargetService;
    private final StudyTimelineService studyTimelineService;
    private final TimelineGenerationService timelineGenerationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningTargetResponse create(@Valid @RequestBody CreateTargetRequest request) {
        return learningTargetService.create(request);
    }

    @GetMapping
    public List<LearningTargetResponse> listActive() {
        return learningTargetService.listActive();
    }

    @GetMapping("/{id}")
    public LearningTargetResponse getById(@PathVariable Long id) {
        return learningTargetService.getById(id);
    }

    @PutMapping("/{id}")
    public LearningTargetResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateTargetRequest request) {
        return learningTargetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        learningTargetService.delete(id);
    }

    @DeleteMapping("/{id}/timeline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeline(@PathVariable Long id) {
        studyTimelineService.clearTimeline(id);
    }

    @PostMapping("/{id}/generate-timeline")
    public WeekPlanResponse generateTimeline(@PathVariable Long id) {
        return timelineGenerationService.generateTimeline(id);
    }

    @GetMapping("/{id}/timeline/today")
    public DayPlanResponse getToday(@PathVariable Long id) {
        return studyTimelineService.getToday(id);
    }

    @GetMapping("/{id}/timeline/week")
    public WeekPlanResponse getWeek(@PathVariable Long id) {
        return studyTimelineService.getWeek(id);
    }
}
