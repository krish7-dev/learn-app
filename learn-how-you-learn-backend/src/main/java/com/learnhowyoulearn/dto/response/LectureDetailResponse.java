package com.learnhowyoulearn.dto.response;

import com.learnhowyoulearn.entity.Difficulty;
import com.learnhowyoulearn.entity.LectureStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Full detail — includes rawContent. Only used for single-lecture endpoints and AI calls.
@Getter @Setter @Builder
public class LectureDetailResponse {
    private Long id;
    private Long courseId;
    private String moduleName;
    private String title;
    private String sourceName;
    private Integer sourceOrder;
    private String rawContent;
    private LectureStatus status;
    private Difficulty difficulty;
    private LocalDateTime lastStudiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // notes and topics populated after Step 7-8
    private LectureNotesResponse notes;
}
