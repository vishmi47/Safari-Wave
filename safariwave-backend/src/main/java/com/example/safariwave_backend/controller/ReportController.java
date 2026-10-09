package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.response.ReportResponseDTO;
import com.example.safariwave_backend.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','GENERAL_MANAGER','FINANCE','MARKETING','OPERATIONS','CUSTOMER_RELATIONS','GUIDE','SAFETY')")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports = reports; }
    @GetMapping("/bookings") public ReportResponseDTO bookings() { return reports.bookings(); }
    @GetMapping("/revenue") public ReportResponseDTO revenue() { return reports.revenue(); }
    @GetMapping("/packages") public ReportResponseDTO packages() { return reports.packages(); }
    @GetMapping("/promotions") public ReportResponseDTO promotions() { return reports.promotions(); }
    @GetMapping("/boats") public ReportResponseDTO boats() { return reports.boats(); }
    @GetMapping("/maintenance") public ReportResponseDTO maintenance() { return reports.maintenance(); }
    @GetMapping("/customers") public ReportResponseDTO customers() { return reports.customers(); }
    @GetMapping("/complaints") public ReportResponseDTO complaints() { return reports.complaints(); }
    @GetMapping("/safety") public ReportResponseDTO safety() { return reports.safety(); }
    @GetMapping("/vendors") public ReportResponseDTO vendors() { return reports.vendors(); }
}
