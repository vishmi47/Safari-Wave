package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.WeatherAlertRequestDTO;
import com.example.safariwave_backend.dto.response.WeatherAlertResponseDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.WeatherAlert;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.WeatherAlertRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.WeatherAlertService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Weather alerts - Weather, Safety & Emergency module (spec 6.4 / B.3).
 *
 * OPERATIONS records and updates alerts and deletes invalid or duplicate ones; GUIDE and CUSTOMER_RELATIONS read.
 * CUSTOMER reads only ACTIVE alerts dated on the day of one of their upcoming bookings.
 * Postponing or cancelling the affected trips is done through the trip endpoints, which update bookings and notify customers.
 */
@Service
public class WeatherAlertServiceImpl implements WeatherAlertService {

    static final List<String> ALERT_STATUSES = List.of("ACTIVE", "RESOLVED");
    private static final List<String> UPCOMING_BOOKING_STATUSES = List.of("PENDING", "CONFIRMED");

    private final WeatherAlertRepository weatherAlertRepository;
    private final BookingRepository bookingRepository;

    public WeatherAlertServiceImpl(WeatherAlertRepository weatherAlertRepository, BookingRepository bookingRepository) {
        this.weatherAlertRepository = weatherAlertRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public WeatherAlertResponseDTO createWeatherAlert(WeatherAlertRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        WeatherAlert alert = new WeatherAlert();
        copyRequestToEntity(dto, alert, "ACTIVE");
        alert.setCreatedBy(CurrentUser.id());

        WeatherAlert saved = weatherAlertRepository.save(alert);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<WeatherAlertResponseDTO> getWeatherAlertById(Integer alertId) {
        Optional<WeatherAlert> alert = weatherAlertRepository.findById(alertId);
        if (isCustomer()) {
            Set<LocalDate> dates = myUpcomingTripDates();
            return alert.filter(a -> affects(a, dates)).map(this::toResponseDTO);
        }
        SafetyPermissions.requireStaffRead();
        return alert.map(this::toResponseDTO);
    }

    @Override
    public List<WeatherAlertResponseDTO> getAllWeatherAlerts() {
        if (isCustomer()) {
            return alertsForMyBookings();
        }
        SafetyPermissions.requireStaffRead();
        return weatherAlertRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<WeatherAlertResponseDTO> getWeatherAlertsBySeverity(String severity) {
        SafetyPermissions.requireStaffRead();
        return weatherAlertRepository.findBySeverity(severity.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<WeatherAlertResponseDTO> getWeatherAlertsByStatus(String status) {
        SafetyPermissions.requireStaffRead();
        return weatherAlertRepository.findByAlertStatus(status.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public Page<WeatherAlertResponseDTO> searchWeatherAlerts(String query, String status, int page, int size) {
        PageRequest pageRequest = PagingSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "alertDateTime"));
        if (isCustomer()) {
            List<WeatherAlertResponseDTO> mine = alertsForMyBookings();
            int from = Math.min((int) pageRequest.getOffset(), mine.size());
            int to = Math.min(from + size, mine.size());
            return new PageImpl<>(mine.subList(from, to), pageRequest, mine.size());
        }
        SafetyPermissions.requireStaffRead();
        return weatherAlertRepository.search(PagingSupport.text(query), PagingSupport.status(status, ALERT_STATUSES), pageRequest)
                .map(this::toResponseDTO);
    }

    @Override
    public WeatherAlertResponseDTO updateWeatherAlert(Integer alertId, WeatherAlertRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        WeatherAlert existing = findAlert(alertId);
        copyRequestToEntity(dto, existing, existing.getAlertStatus());

        WeatherAlert updated = weatherAlertRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteWeatherAlert(Integer alertId) {
        SafetyPermissions.requireCanDelete();
        weatherAlertRepository.delete(findAlert(alertId));
    }

    private void copyRequestToEntity(WeatherAlertRequestDTO dto, WeatherAlert alert, String defaultStatus) {
        alert.setAlertDateTime(dto.getAlertDateTime());
        alert.setLocation(dto.getLocation().trim());
        alert.setWeatherCondition(dto.getWeatherCondition().trim());
        alert.setSeverity(SafetyPermissions.oneOf(dto.getSeverity(), SafetyPermissions.SEVERITIES, "Severity", null));
        alert.setDescription(dto.getDescription());
        alert.setAlertStatus(SafetyPermissions.oneOf(dto.getAlertStatus(), ALERT_STATUSES, "Alert status", defaultStatus));
        alert.setRemarks(dto.getRemarks());
    }

    private boolean isCustomer() {
        return CurrentUser.hasAnyRole(Roles.CUSTOMER);
    }

    /** Dates of the logged-in customer's upcoming pending or confirmed bookings. */
    private Set<LocalDate> myUpcomingTripDates() {
        LocalDate today = LocalDate.now();
        return bookingRepository.findByCustomer_UserIdAndBookingStatusIn(CurrentUser.id(), UPCOMING_BOOKING_STATUSES).stream()
                .map(Booking::getTripDate)
                .filter(date -> date != null && !date.isBefore(today))
                .collect(Collectors.toSet());
    }

    private boolean affects(WeatherAlert alert, Set<LocalDate> dates) {
        return "ACTIVE".equals(alert.getAlertStatus()) && alert.getAlertDateTime() != null
                && dates.contains(alert.getAlertDateTime().toLocalDate());
    }

    private List<WeatherAlertResponseDTO> alertsForMyBookings() {
        if (!isCustomer()) {
            throw new AccessDeniedException("Only customers have booking-related alerts.");
        }
        Set<LocalDate> dates = myUpcomingTripDates();
        return weatherAlertRepository.findByAlertStatus("ACTIVE").stream()
                .filter(alert -> affects(alert, dates))
                .sorted(Comparator.comparing(WeatherAlert::getAlertDateTime))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private WeatherAlert findAlert(Integer alertId) {
        return weatherAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Weather alert not found with id: " + alertId));
    }

    private WeatherAlertResponseDTO toResponseDTO(WeatherAlert alert) {
        WeatherAlertResponseDTO dto = new WeatherAlertResponseDTO();
        dto.setAlertId(alert.getAlertId());
        dto.setAlertDateTime(alert.getAlertDateTime());
        dto.setLocation(alert.getLocation());
        dto.setWeatherCondition(alert.getWeatherCondition());
        dto.setSeverity(alert.getSeverity());
        dto.setDescription(alert.getDescription());
        dto.setAlertStatus(alert.getAlertStatus());
        dto.setRemarks(alert.getRemarks());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setUpdatedAt(alert.getUpdatedAt());
        dto.setCreatedBy(alert.getCreatedBy());
        return dto;
    }
}
