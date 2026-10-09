package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','GENERAL_MANAGER','MARKETING','OPERATIONS','CUSTOMER_RELATIONS','FINANCE','GUIDE','SAFETY')")
    public ResponseEntity<Map<String,Object>> summary() { return ResponseEntity.ok(dashboardService.summary()); }
    @GetMapping("/customers/{userId}")
    public ResponseEntity<Map<String,Object>> customer(@PathVariable Integer userId) { return ResponseEntity.ok(dashboardService.customerSummary(userId)); }
}
