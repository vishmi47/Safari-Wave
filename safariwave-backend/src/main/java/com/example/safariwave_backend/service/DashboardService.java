package com.example.safariwave_backend.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> summary();
    Map<String, Object> customerSummary(Integer userId);
}
