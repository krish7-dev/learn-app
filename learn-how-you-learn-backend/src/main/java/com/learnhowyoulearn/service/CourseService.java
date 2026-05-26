package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.CreateCourseRequest;
import com.learnhowyoulearn.dto.request.UpdateCourseRequest;
import com.learnhowyoulearn.dto.response.CourseResponse;
import com.learnhowyoulearn.dto.response.CourseSummaryResponse;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.entity.Course;
import com.learnhowyoulearn.entity.CourseStatus;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.mapper.CourseMapper;
import com.learnhowyoulearn.repository.CourseRepository;
import com.learnhowyoulearn.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final long USER_ID = 1L;

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final LectureRepository lectureRepository;

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        Course course = Course.builder()
                .userId(USER_ID)
                .title(request.getTitle())
                .description(request.getDescription())
                .goal(request.getGoal())
                .status(CourseStatus.NOT_STARTED)
                .build();
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> coursePage = courseRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(USER_ID, pageable);
        return PageResponse.<CourseSummaryResponse>builder()
                .content(coursePage.getContent().stream().map(courseMapper::toSummary).toList())
                .page(coursePage.getNumber())
                .size(coursePage.getSize())
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .last(coursePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        return courseMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CourseResponse update(Long id, UpdateCourseRequest request) {
        Course course = findOrThrow(id);
        if (request.getTitle() != null)       course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getGoal() != null)        course.setGoal(request.getGoal());
        if (request.getStatus() != null)      course.setStatus(request.getStatus());
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateModuleOrder(Long id, List<String> moduleOrder) {
        Course course = findOrThrow(id);
        course.setModuleOrder(moduleOrder);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(Long id) {
        Course course = findOrThrow(id);
        LocalDateTime now = LocalDateTime.now();
        lectureRepository.softDeleteByCourseId(id, now);
        course.setDeletedAt(now);
        courseRepository.save(course);
    }

    private Course findOrThrow(Long id) {
        return courseRepository.findByIdAndUserIdAndDeletedAtIsNull(id, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }
}
