package com.example.safariwave_backend.dto.response;

import java.util.Map;

public record ReportResponseDTO(String report, Map<String, Object> metrics) {}
