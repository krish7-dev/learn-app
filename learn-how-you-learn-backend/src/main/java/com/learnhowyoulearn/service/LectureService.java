package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.AddToNotesRequest;
import com.learnhowyoulearn.dto.request.BulkCreateLectureRequest;
import com.learnhowyoulearn.dto.request.CreateLectureRequest;
import com.learnhowyoulearn.dto.request.UpdateLectureRequest;
import com.learnhowyoulearn.dto.response.LectureDetailResponse;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.LectureNotes;
import com.learnhowyoulearn.entity.LectureStatus;
import com.learnhowyoulearn.exception.ResourceNotFoundException;
import com.learnhowyoulearn.mapper.LectureMapper;
import com.learnhowyoulearn.repository.CourseRepository;
import com.learnhowyoulearn.repository.LectureNotesRepository;
import com.learnhowyoulearn.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {

    private static final long USER_ID = 1L;

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final LectureNotesRepository lectureNotesRepository;
    private final LectureMapper lectureMapper;

    @Transactional
    public LectureDetailResponse create(Long courseId, CreateLectureRequest request) {
        verifyCourseExists(courseId);
        Lecture lecture = Lecture.builder()
                .userId(USER_ID)
                .courseId(courseId)
                .moduleName(request.getModuleName())
                .title(request.getTitle())
                .sourceName(request.getSourceName())
                .sourceOrder(request.getSourceOrder())
                .rawContent(request.getRawContent())
                .status(LectureStatus.NOT_STARTED)
                .build();
        return lectureMapper.toDetail(lectureRepository.save(lecture));
    }

    @Transactional(readOnly = true)
    public PageResponse<LectureSummaryResponse> listByCourse(Long courseId, int page, int size) {
        verifyCourseExists(courseId);
        Page<Lecture> lecturePage = lectureRepository.findByCourseAndUser(
                USER_ID, courseId, PageRequest.of(page, size));
        return PageResponse.<LectureSummaryResponse>builder()
                .content(lecturePage.getContent().stream()
                        .map(l -> lectureMapper.toSummary(l,
                                lectureNotesRepository.existsByUserIdAndLectureId(USER_ID, l.getId())))
                        .toList())
                .page(lecturePage.getNumber())
                .size(lecturePage.getSize())
                .totalElements(lecturePage.getTotalElements())
                .totalPages(lecturePage.getTotalPages())
                .last(lecturePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public LectureDetailResponse getById(Long id) {
        Lecture lecture = findOrThrow(id);
        LectureDetailResponse response = lectureMapper.toDetail(lecture);
        lectureNotesRepository.findByUserIdAndLectureId(USER_ID, id).ifPresent(notes ->
                response.setNotes(lectureMapper.toNotesResponse(notes)));
        return response;
    }

    @Transactional
    public LectureDetailResponse update(Long id, UpdateLectureRequest request) {
        Lecture lecture = findOrThrow(id);
        if (request.getTitle() != null)       lecture.setTitle(request.getTitle());
        if (request.getModuleName() != null)  lecture.setModuleName(request.getModuleName());
        if (request.getSourceName() != null)  lecture.setSourceName(request.getSourceName());
        if (request.getSourceOrder() != null) lecture.setSourceOrder(request.getSourceOrder());
        if (request.getRawContent() != null) {
            lecture.setRawContent(request.getRawContent());
            // Updating raw content marks notes as stale — must regenerate
            lecture.setContentStatus("TRANSCRIPT_ADDED");
        }
        if (request.getStatus() != null)      lecture.setStatus(request.getStatus());
        if (request.getDifficulty() != null)  lecture.setDifficulty(request.getDifficulty());
        return lectureMapper.toDetail(lectureRepository.save(lecture));
    }

    @Transactional
    public void delete(Long id) {
        Lecture lecture = findOrThrow(id);
        lecture.setDeletedAt(LocalDateTime.now());
        lectureRepository.save(lecture);
    }

    @Transactional
    public LectureDetailResponse addToNotes(Long id, AddToNotesRequest request) {
        Lecture lecture = findOrThrow(id);
        LectureNotes notes = lectureNotesRepository.findByUserIdAndLectureId(USER_ID, id)
                .orElseThrow(() -> new ResourceNotFoundException("No notes found for lecture: " + id + ". Generate notes first."));
        String existing = notes.getChatAdditions();
        String separator = (existing != null && !existing.isBlank()) ? "\n\n---\n\n" : "";
        notes.setChatAdditions((existing != null ? existing : "") + separator + request.getContent().trim());
        LectureNotes saved = lectureNotesRepository.save(notes);
        LectureDetailResponse response = lectureMapper.toDetail(lecture);
        response.setNotes(lectureMapper.toNotesResponse(saved));
        return response;
    }

    @Transactional
    public List<LectureSummaryResponse> bulkCreate(Long courseId, BulkCreateLectureRequest request) {
        verifyCourseExists(courseId);
        List<Lecture> lectures = request.getLectures().stream()
                .map(item -> Lecture.builder()
                        .userId(USER_ID)
                        .courseId(courseId)
                        .moduleName(request.getModuleName())
                        .sourceName(request.getSourceName())
                        .title(item.getTitle())
                        .sourceOrder(item.getSourceOrder())
                        .estimatedMinutes(item.getEstimatedMinutes() != null ? item.getEstimatedMinutes() : 60)
                        .contentStatus("NOT_ADDED")
                        .status(LectureStatus.NOT_STARTED)
                        .build())
                .toList();
        return lectureRepository.saveAll(lectures).stream()
                .map(l -> lectureMapper.toSummary(l, false))
                .toList();
    }

    private Lecture findOrThrow(Long id) {
        return lectureRepository.findByIdAndUserIdAndDeletedAtIsNull(id, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + id));
    }

    private void verifyCourseExists(Long courseId) {
        courseRepository.findByIdAndUserIdAndDeletedAtIsNull(courseId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }
}
