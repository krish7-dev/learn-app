package com.learnhowyoulearn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkCreateLectureRequest {

    private String moduleName;
    private String sourceName;

    @NotNull
    @Size(min = 1, message = "At least one lecture is required")
    @Valid
    private List<LectureShellItem> lectures;

    @Getter
    @NoArgsConstructor
    public static class LectureShellItem {

        @NotBlank
        private String title;

        private Integer sourceOrder;

        @Positive
        @Min(1)
        private Integer estimatedMinutes;
    }
}
