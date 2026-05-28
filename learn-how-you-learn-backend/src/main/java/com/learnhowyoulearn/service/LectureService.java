package com.learnhowyoulearn.service;

import com.learnhowyoulearn.dto.request.AddToNotesRequest;
import com.learnhowyoulearn.dto.request.BulkCreateLectureRequest;
import com.learnhowyoulearn.dto.request.CreateLectureRequest;
import com.learnhowyoulearn.dto.request.ParseLectureListRequest;
import com.learnhowyoulearn.dto.request.UpdateLectureRequest;
import com.learnhowyoulearn.dto.response.LectureDetailResponse;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.dto.response.PageResponse;
import com.learnhowyoulearn.exception.AiGenerationException;
import com.learnhowyoulearn.service.ai.AiClient;
import com.learnhowyoulearn.service.ai.AiRequest;
import com.learnhowyoulearn.service.ai.AiResponse;
import com.learnhowyoulearn.entity.Course;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LectureService {

    private static final long USER_ID = 1L;

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final LectureNotesRepository lectureNotesRepository;
    private final LectureMapper lectureMapper;
    private final TreeNodeService treeNodeService;
    private final AiClient aiClient;

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
    public void renameModule(Long courseId, String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be blank");
        }
        List<Lecture> lectures = lectureRepository.findByModuleAndUser(USER_ID, courseId, oldName);
        lectures.forEach(l -> l.setModuleName(newName.trim()));
        lectureRepository.saveAll(lectures);
    }

    @Transactional
    public void delete(Long id) {
        Lecture lecture = findOrThrow(id);
        lecture.setDeletedAt(LocalDateTime.now());
        lectureRepository.save(lecture);
    }

    @Transactional
    public LectureDetailResponse updateNotesContent(Long id, String fullCleanNotes) {
        Lecture lecture = findOrThrow(id);
        LectureNotes notes = lectureNotesRepository.findByUserIdAndLectureId(USER_ID, id)
                .orElseThrow(() -> new ResourceNotFoundException("No notes found for lecture: " + id));
        notes.setFullCleanNotes(fullCleanNotes);
        LectureNotes saved = lectureNotesRepository.save(notes);
        LectureDetailResponse response = lectureMapper.toDetail(lecture);
        response.setNotes(lectureMapper.toNotesResponse(saved));
        return response;
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
        Course course = courseRepository.findByIdAndUserIdAndDeletedAtIsNull(courseId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        List<Lecture> lectures = request.getLectures().stream()
                .map(item -> Lecture.builder()
                        .userId(USER_ID)
                        .courseId(courseId)
                        .moduleName(item.getModuleName() != null && !item.getModuleName().isBlank()
                                ? item.getModuleName() : request.getModuleName())
                        .sourceName(request.getSourceName())
                        .title(item.getTitle())
                        .sourceOrder(item.getSourceOrder())
                        .estimatedMinutes(item.getEstimatedMinutes() != null ? item.getEstimatedMinutes() : 60)
                        .contentStatus("NOT_ADDED")
                        .status(LectureStatus.NOT_STARTED)
                        .build())
                .toList();
        List<Lecture> saved = lectureRepository.saveAll(lectures);

        String courseTitle = course.getTitle();
        for (int i = 0; i < saved.size(); i++) {
            Lecture l = saved.get(i);
            String moduleName = (l.getModuleName() != null && !l.getModuleName().isBlank())
                    ? l.getModuleName() : courseTitle;
            treeNodeService.createShellNode(USER_ID, courseTitle, moduleName, l.getId(), l.getTitle(), i);
        }

        return saved.stream().map(l -> lectureMapper.toSummary(l, false)).toList();
    }

    public Map<String, String> parseLectureList(Long courseId, ParseLectureListRequest request) {
        Course course = courseRepository.findByIdAndUserIdAndDeletedAtIsNull(courseId, USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        String system = """
                You are a course organizer. Given raw lecture/topic names from a programming or DSA course, \
                organize them into a clean structured list.
                Return ONLY the formatted list — no JSON, no markdown, no explanation.
                Each line must be exactly: Module: Lecture Title
                Rules:
                - If a line already has a clear module prefix (e.g. "Arrays: Two Pointers"), keep it
                - Group unlabeled topics into logical DSA/programming modules based on their content
                - Preserve the original title as closely as possible — only fix obvious formatting issues
                - Do NOT skip any lectures from the input
                - Do NOT add lectures that are not in the input
                - One lecture per line, no blank lines, no bullet points, no numbering
                """;

        String user = "Course: " + course.getTitle() + "\n\nRaw lecture names:\n---\n"
                + request.getRawText() + "\n---";

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(system)
                .userPrompt(user)
                .purpose("PARSE_LECTURE_LIST")
                .temperature(0.2)
                .build();

        AiResponse aiResponse;
        try {
            aiResponse = aiClient.generate(aiRequest);
        } catch (Exception e) {
            throw new AiGenerationException("Could not parse lecture list. Please try again.");
        }

        String formatted = aiResponse.getContent().trim();
        return Map.of("formatted", formatted);
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
