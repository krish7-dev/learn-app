package com.learnhowyoulearn.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ImportNotesBatchRequest {
    private String moduleName;
    private String sourceName;
    private List<String> contents;
}
