package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.CreateCourseRequest;
import com.learnhowyoulearn.dto.request.UpdateCourseRequest;
import com.learnhowyoulearn.dto.response.CourseResponse;
import com.learnhowyoulearn.dto.response.CourseSummaryResponse;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.create(request);
    }

    @GetMapping
    public PageResponse<CourseSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return courseService.list(page, size);
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request);
    }

    @PatchMapping("/{id}/module-order")
    public CourseResponse updateModuleOrder(@PathVariable Long id,
                                            @RequestBody List<String> moduleOrder) {
        return courseService.updateModuleOrder(id, moduleOrder);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
