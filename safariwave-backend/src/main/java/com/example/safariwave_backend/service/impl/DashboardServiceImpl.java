package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.repository.*;
import com.example.safariwave_backend.service.DashboardService;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final UserRepository users; private final BookingRepository bookings; private final BoatRepository boats;
    private final SafariTripRepository trips; private final SafariPackageRepository packages; private final PromotionRepository promotions;
    private final PartnerVendorRepository vendors; private final WeatherAlertRepository weather; private final EmergencyIncidentRepository incidents;
    private final CustomerExperienceRepository experiences;
    public DashboardServiceImpl(UserRepository users, BookingRepository bookings, BoatRepository boats, SafariTripRepository trips,
                                SafariPackageRepository packages, PromotionRepository promotions, PartnerVendorRepository vendors,
                                WeatherAlertRepository weather, EmergencyIncidentRepository incidents, CustomerExperienceRepository experiences) {
        this.users=users; this.bookings=bookings; this.boats=boats; this.trips=trips; this.packages=packages; this.promotions=promotions;
        this.vendors=vendors; this.weather=weather; this.incidents=incidents; this.experiences=experiences;
    }
    public Map<String,Object> summary() {
        Map<String,Object> result = new LinkedHashMap<>(); result.put("totalUsers", users.count()); result.put("totalBookings", bookings.count());
        result.put("upcomingTrips", trips.findAll().stream().filter(t -> "SCHEDULED".equalsIgnoreCase(t.getStatus())).count());
        result.put("totalBoats", boats.count()); result.put("operationalBoats", boats.findAll().stream().filter(b -> "OPERATIONAL".equalsIgnoreCase(b.getStatus())).count());
        result.put("maintenanceBoats", boats.findAll().stream().filter(b -> "UNDER_MAINTENANCE".equalsIgnoreCase(b.getStatus())).count());
        result.put("activePackages", packages.findAll().stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())).count());
        result.put("publishedPromotions", promotions.findAll().stream().filter(p -> "PUBLISHED".equalsIgnoreCase(p.getStatus())).count());
        result.put("pendingVendors", vendors.countByStatus(com.example.safariwave_backend.enums.PartnerVendorStatus.PENDING_APPROVAL));
        result.put("complaints", experiences.findByExperienceType(com.example.safariwave_backend.enums.CustomerExperienceType.COMPLAINT).size());
        result.put("weatherAlerts", weather.count()); result.put("emergencyIncidents", incidents.count()); return result;
    }
    public Map<String,Object> customerSummary(Integer userId) {
        Map<String,Object> result = new LinkedHashMap<>(); result.put("userId", userId); result.put("myBookings", bookings.findByCustomer_UserId(userId).size());
        result.put("myComplaints", experiences.findByCustomer(users.findById(userId).orElseThrow()).stream().filter(e -> e.getExperienceType() == com.example.safariwave_backend.enums.CustomerExperienceType.COMPLAINT).count()); return result;
    }
}
