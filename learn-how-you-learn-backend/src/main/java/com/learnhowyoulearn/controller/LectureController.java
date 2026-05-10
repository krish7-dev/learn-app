package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.AddToNotesRequest;
import com.learnhowyoulearn.dto.request.BulkCreateLectureRequest;
import com.learnhowyoulearn.dto.request.ConfusionRequest;
import com.learnhowyoulearn.dto.request.CreateLectureRequest;
import com.learnhowyoulearn.dto.request.TutorChatRequest;
import com.learnhowyoulearn.dto.request.UpdateLectureRequest;
import com.learnhowyoulearn.dto.response.ConfusionResponse;
import com.learnhowyoulearn.dto.response.LectureDetailResponse;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.dto.response.TutorChatResponse;

import java.util.List;
import com.learnhowyoulearn.service.ConfusionService;
import com.learnhowyoulearn.service.LectureService;
import com.learnhowyoulearn.service.NoteGenerationService;
import com.learnhowyoulearn.service.TutorChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;
    private final NoteGenerationService noteGenerationService;
    private final TutorChatService tutorChatService;
    private final ConfusionService confusionService;

    @PostMapping("/api/v1/courses/{courseId}/lectures")
    @ResponseStatus(HttpStatus.CREATED)
    public LectureDetailResponse create(@PathVariable Long courseId,
                                        @Valid @RequestBody CreateLectureRequest request) {
        return lectureService.create(courseId, request);
    }

    @PostMapping("/api/v1/courses/{courseId}/lectures/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LectureSummaryResponse> bulkCreate(@PathVariable Long courseId,
                                                   @Valid @RequestBody BulkCreateLectureRequest request) {
        return lectureService.bulkCreate(courseId, request);
    }

    @GetMapping("/api/v1/courses/{courseId}/lectures")
    public PageResponse<LectureSummaryResponse> listByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return lectureService.listByCourse(courseId, page, size);
    }

    @GetMapping("/api/v1/lectures/{id}")
    public LectureDetailResponse getById(@PathVariable Long id) {
        return lectureService.getById(id);
    }

    @PutMapping("/api/v1/lectures/{id}")
    public LectureDetailResponse update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateLectureRequest request) {
        return lectureService.update(id, request);
    }

    @DeleteMapping("/api/v1/lectures/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        lectureService.delete(id);
    }

    @PostMapping("/api/v1/lectures/{id}/generate-notes")
    public LectureDetailResponse generateNotes(@PathVariable Long id) {
        return noteGenerationService.generateNotes(id);
    }

    @PostMapping("/api/v1/lectures/{id}/chat")
    public TutorChatResponse chat(@PathVariable Long id,
                                  @Valid @RequestBody TutorChatRequest request) {
        return tutorChatService.chat(id, request);
    }

    @PatchMapping("/api/v1/lectures/{id}/notes/additions")
    public LectureDetailResponse addToNotes(@PathVariable Long id,
                                            @Valid @RequestBody AddToNotesRequest request) {
        return lectureService.addToNotes(id, request);
    }

    @PostMapping("/api/v1/lectures/{id}/confusions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfusionResponse logConfusion(@PathVariable Long id,
                                          @Valid @RequestBody ConfusionRequest request) {
        return confusionService.logConfusion(id, request);
    }
}
