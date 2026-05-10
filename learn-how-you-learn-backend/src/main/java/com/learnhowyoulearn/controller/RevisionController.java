package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.request.UpdateRevisionRequest;
import com.learnhowyoulearn.dto.response.RevisionItemResponse;
import com.learnhowyoulearn.service.RevisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/revision")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping
    public List<RevisionItemResponse> getPending() {
        return revisionService.getPending();
    }

    @PutMapping("/{id}")
    public RevisionItemResponse update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateRevisionRequest request) {
        return revisionService.update(id, request);
    }
}
