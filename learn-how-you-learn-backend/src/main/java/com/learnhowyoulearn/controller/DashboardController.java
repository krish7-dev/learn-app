package com.learnhowyoulearn.controller;

import com.learnhowyoulearn.dto.response.DashboardResponse;
import com.learnhowyoulearn.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/today")
    public DashboardResponse today() {
        return dashboardService.getToday();
    }
}
