package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.response.ReportResponseDTO;
import com.example.safariwave_backend.repository.*;
import com.example.safariwave_backend.service.ReportService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    private final BookingRepository bookings; private final SafariPackageRepository packages; private final PromotionRepository promotions;
    private final BoatRepository boats; private final BoatMaintenanceRecordRepository maintenance; private final CustomerExperienceRepository experiences;
    private final WeatherAlertRepository weather; private final SafetyInspectionRepository inspections; private final EmergencyIncidentRepository incidents;
    private final PartnerVendorRepository vendors;
    public ReportServiceImpl(BookingRepository bookings, SafariPackageRepository packages, PromotionRepository promotions, BoatRepository boats,
                             BoatMaintenanceRecordRepository maintenance, CustomerExperienceRepository experiences, WeatherAlertRepository weather,
                             SafetyInspectionRepository inspections, EmergencyIncidentRepository incidents, PartnerVendorRepository vendors) {
        this.bookings=bookings; this.packages=packages; this.promotions=promotions; this.boats=boats; this.maintenance=maintenance; this.experiences=experiences;
        this.weather=weather; this.inspections=inspections; this.incidents=incidents; this.vendors=vendors;
    }
    public ReportResponseDTO bookings() { return report("bookings", "total", bookings.count(), "confirmed", bookings.findByBookingStatus("CONFIRMED").size()); }
    public ReportResponseDTO revenue() { BigDecimal total = bookings.findAll().stream().filter(b -> "SUCCESS".equalsIgnoreCase(b.getPaymentStatus())).map(com.example.safariwave_backend.entity.Booking::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add); return report("revenue", "successfulPayments", bookings.findAll().stream().filter(b -> "SUCCESS".equalsIgnoreCase(b.getPaymentStatus())).count(), "totalRevenue", total); }
    public ReportResponseDTO packages() { return report("packages", "total", packages.count(), "active", packages.findAll().stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())).count()); }
    public ReportResponseDTO promotions() { return report("promotions", "total", promotions.count(), "published", promotions.findAll().stream().filter(p -> "PUBLISHED".equalsIgnoreCase(p.getStatus())).count()); }
    public ReportResponseDTO boats() { return report("boats", "total", boats.count(), "operational", boats.findAll().stream().filter(b -> "OPERATIONAL".equalsIgnoreCase(b.getStatus())).count()); }
    public ReportResponseDTO maintenance() { return report("maintenance", "records", maintenance.count(), "boatsUnderMaintenance", boats.findAll().stream().filter(b -> "UNDER_MAINTENANCE".equalsIgnoreCase(b.getStatus())).count()); }
    public ReportResponseDTO customers() { return report("customer-satisfaction", "experiences", experiences.count(), "ratingsSubmitted", experiences.findAll().stream().filter(e -> e.getRating() != null).count()); }
    public ReportResponseDTO complaints() { return report("complaints", "total", experiences.findByExperienceType(com.example.safariwave_backend.enums.CustomerExperienceType.COMPLAINT).size(), "resolved", experiences.findByStatus(com.example.safariwave_backend.enums.CustomerExperienceStatus.RESOLVED).size()); }
    public ReportResponseDTO safety() { return report("safety", "weatherAlerts", weather.count(), "inspections", inspections.count(), "incidents", incidents.count()); }
    public ReportResponseDTO vendors() { return report("vendors", "total", vendors.count(), "pendingApproval", vendors.countByStatus(com.example.safariwave_backend.enums.PartnerVendorStatus.PENDING_APPROVAL)); }
    private ReportResponseDTO report(String name, Object... values) { Map<String,Object> metrics = new LinkedHashMap<>(); for (int i=0;i<values.length;i+=2) metrics.put(String.valueOf(values[i]), values[i+1]); return new ReportResponseDTO(name, metrics); }
}
