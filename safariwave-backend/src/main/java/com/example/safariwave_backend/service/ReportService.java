package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.response.ReportResponseDTO;

public interface ReportService {
    ReportResponseDTO bookings();
    ReportResponseDTO revenue();
    ReportResponseDTO packages();
    ReportResponseDTO promotions();
    ReportResponseDTO boats();
    ReportResponseDTO maintenance();
    ReportResponseDTO customers();
    ReportResponseDTO complaints();
    ReportResponseDTO safety();
    ReportResponseDTO vendors();
}
