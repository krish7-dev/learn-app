package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.context.ParsedTimeline;
import com.learnhowyoulearn.dto.request.CreateTargetRequest;
import com.learnhowyoulearn.dto.request.UpdateTargetRequest;
import com.learnhowyoulearn.dto.response.DayPlanResponse;
import com.learnhowyoulearn.dto.response.LearningTargetResponse;
import com.learnhowyoulearn.dto.response.WeekPlanResponse;
import com.learnhowyoulearn.service.LearningTargetService;
import com.learnhowyoulearn.service.StudyTimelineService;
import com.learnhowyoulearn.service.TimelineGenerationService;
import com.learnhowyoulearn.service.TimelineGenerationStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/targets")
@RequiredArgsConstructor
@Slf4j
public class LearningTargetController {

    private final LearningTargetService learningTargetService;
    private final StudyTimelineService studyTimelineService;
    private final TimelineGenerationService timelineGenerationService;
    private final TimelineGenerationStatusService timelineGenerationStatusService;

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

    @PostMapping("/{id}/import-timeline")
    @ResponseStatus(HttpStatus.OK)
    public void importTimeline(@PathVariable Long id, @RequestBody ParsedTimeline timeline) {
        studyTimelineService.importTimeline(id, timeline);
    }

    @PostMapping("/{id}/ask-ai")
    public Map<String, String> askAi(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = timelineGenerationService.previewAiPlan(body.get("prompt"));
        return Map.of("content", content);
    }

    @PostMapping("/{id}/generate-timeline")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> generateTimeline(@PathVariable Long id) {
        timelineGenerationService.generateTimelineAsync(id);
        return Map.of("status", "GENERATING");
    }

    @GetMapping("/{id}/generation-status")
    public Map<String, String> getGenerationStatus(@PathVariable Long id) {
        TimelineGenerationStatusService.Status status = timelineGenerationStatusService.getStatus(id);
        log.info("STATUS POLL — targetId={}, status={}", id, status);
        if (status == TimelineGenerationStatusService.Status.ERROR) {
            return Map.of("status", "ERROR", "message", timelineGenerationStatusService.getError(id));
        }
        return Map.of("status", status.name());
    }

    @GetMapping("/{id}/timeline/today")
    public DayPlanResponse getToday(@PathVariable Long id) {
        return studyTimelineService.getToday(id);
    }

    @GetMapping("/{id}/timeline/week")
    public WeekPlanResponse getWeek(@PathVariable Long id) {
        return studyTimelineService.getWeek(id);
    }

    @GetMapping("/{id}/timeline/full")
    public WeekPlanResponse getFullTimeline(@PathVariable Long id) {
        return studyTimelineService.getFullTimeline(id);
    }
}
