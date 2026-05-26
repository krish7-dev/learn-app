package com.learnhowyoulearn.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class TimelineGenerationStatusService {

    public enum Status { IDLE, GENERATING, DONE, ERROR }

    private final Map<Long, Status> statusMap = new ConcurrentHashMap<>();
    private final Map<Long, String> errorMap  = new ConcurrentHashMap<>();

    public void setGenerating(Long targetId) {
        statusMap.put(targetId, Status.GENERATING);
        errorMap.remove(targetId);
    }

    public void setDone(Long targetId) {
        statusMap.put(targetId, Status.DONE);
    }

    public void setError(Long targetId, String message) {
        statusMap.put(targetId, Status.ERROR);
        errorMap.put(targetId, message);
    }

    public Status getStatus(Long targetId) {
        return statusMap.getOrDefault(targetId, Status.IDLE);
    }

    public String getError(Long targetId) {
        return errorMap.getOrDefault(targetId, "Generation failed");
    }
}
