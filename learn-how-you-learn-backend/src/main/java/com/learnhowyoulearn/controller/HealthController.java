package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.config.OpenAiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiConfig openAiConfig;

    @GetMapping
    public Map<String, Object> health() {
        String dbStatus = checkDb();
        Map<String, Object> openai = new LinkedHashMap<>();
        openai.put("enabled", openAiConfig.isEnabled());
        openai.put("model", openAiConfig.isEnabled() ? openAiConfig.getModel() : "mock");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP".equals(dbStatus) ? "UP" : "DEGRADED");
        response.put("database", dbStatus);
        response.put("openai", openai);
        return response;
    }

    private String checkDb() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
