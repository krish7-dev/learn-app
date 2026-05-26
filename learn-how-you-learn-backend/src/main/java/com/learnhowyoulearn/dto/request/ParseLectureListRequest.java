package com.learnhowyoulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParseLectureListRequest {

    @NotBlank
    @Size(max = 20_000)
    private String rawText;
}
