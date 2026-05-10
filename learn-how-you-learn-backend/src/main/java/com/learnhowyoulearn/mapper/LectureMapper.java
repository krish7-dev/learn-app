package com.learnhowyoulearn.mapper;

import com.learnhowyoulearn.dto.response.LectureDetailResponse;
import com.learnhowyoulearn.dto.response.LectureNotesResponse;
import com.learnhowyoulearn.dto.response.LectureSummaryResponse;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.LectureNotes;
import org.springframework.stereotype.Component;

@Component
public class LectureMapper {

    public LectureSummaryResponse toSummary(Lecture lecture, boolean notesGenerated) {
        return LectureSummaryResponse.builder()
                .id(lecture.getId())
                .courseId(lecture.getCourseId())
                .moduleName(lecture.getModuleName())
                .title(lecture.getTitle())
                .sourceName(lecture.getSourceName())
                .sourceOrder(lecture.getSourceOrder())
                .status(lecture.getStatus())
                .difficulty(lecture.getDifficulty())
                .notesGenerated(notesGenerated)
                .lastStudiedAt(lecture.getLastStudiedAt())
                .createdAt(lecture.getCreatedAt())
                .updatedAt(lecture.getUpdatedAt())
                .build();
    }

    public LectureDetailResponse toDetail(Lecture lecture) {
        return LectureDetailResponse.builder()
                .id(lecture.getId())
                .courseId(lecture.getCourseId())
                .moduleName(lecture.getModuleName())
                .title(lecture.getTitle())
                .sourceName(lecture.getSourceName())
                .sourceOrder(lecture.getSourceOrder())
                .rawContent(lecture.getRawContent())
                .status(lecture.getStatus())
                .difficulty(lecture.getDifficulty())
                .lastStudiedAt(lecture.getLastStudiedAt())
                .createdAt(lecture.getCreatedAt())
                .updatedAt(lecture.getUpdatedAt())
                .notes(null)
                .build();
    }

    public LectureNotesResponse toNotesResponse(LectureNotes notes) {
        if (notes == null) return null;
        return LectureNotesResponse.builder()
                .id(notes.getId())
                .title(notes.getTitle())
                .fullCleanNotes(notes.getFullCleanNotes())
                .simpleExplanation(notes.getSimpleExplanation())
                .practicalUsage(notes.getPracticalUsage())
                .examples(notes.getExamples())
                .mistakesToAvoid(notes.getMistakesToAvoid())
                .edgeCases(notes.getEdgeCases())
                .revisionNotes(notes.getRevisionNotes())
                .interviewQuestions(notes.getInterviewQuestions())
                .flashcards(notes.getFlashcards())
                .practiceQuestions(notes.getPracticeQuestions())
                .weakAreaChecks(notes.getWeakAreaChecks())
                .chatAdditions(notes.getChatAdditions())
                .createdAt(notes.getCreatedAt())
                .updatedAt(notes.getUpdatedAt())
                .build();
    }
}
