package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.response.TreeNodeDetailResponse;
import com.learnhowyoulearn.dto.response.TreeNodeResponse;
import com.learnhowyoulearn.service.TreeNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning-tree")
@RequiredArgsConstructor
public class TreeController {

    private static final long USER_ID = 1L;

    private final TreeNodeService treeNodeService;

    @GetMapping
    public List<TreeNodeResponse> getFullTree() {
        return treeNodeService.getFullTree(USER_ID);
    }

    @GetMapping("/{nodeId}")
    public TreeNodeDetailResponse getNodeDetail(@PathVariable Long nodeId) {
        return treeNodeService.getNodeDetail(USER_ID, nodeId);
    }

    @PostMapping("/backfill")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Integer> backfill() {
        int count = treeNodeService.backfill(USER_ID);
        return Map.of("nodesProcessed", count);
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Integer> reset() {
        return treeNodeService.reset(USER_ID);
    }
}
