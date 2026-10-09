package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.EmergencyIncidentRequestDTO;
import com.example.safariwave_backend.dto.request.SafetyInspectionRequestDTO;
import com.example.safariwave_backend.dto.request.WeatherAlertRequestDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.EmergencyIncident;
import com.example.safariwave_backend.entity.SafetyInspection;
import com.example.safariwave_backend.entity.WeatherAlert;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.EmergencyIncidentRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.repository.WeatherAlertRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.EmergencyIncidentServiceImpl;
import com.example.safariwave_backend.service.impl.SafetyInspectionServiceImpl;
import com.example.safariwave_backend.service.impl.WeatherAlertServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Weather, Safety & Emergency module (spec 6.4 / B.3). */
@ExtendWith(MockitoExtension.class)
class SafetyServiceTest {

    @Mock
    private WeatherAlertRepository weatherAlertRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SafetyInspectionRepository safetyInspectionRepository;

    @Mock
    private EmergencyIncidentRepository incidentRepository;

    @Mock
    private BoatRepository boatRepository;

    @Mock
    private SafariTripRepository safariTripRepository;

    @Mock
    private NotificationService notificationService;

    private WeatherAlertServiceImpl weatherService;
    private SafetyInspectionServiceImpl inspectionService;
    private EmergencyIncidentServiceImpl incidentService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherAlertServiceImpl(weatherAlertRepository, bookingRepository);
        inspectionService = new SafetyInspectionServiceImpl(safetyInspectionRepository, boatRepository, safariTripRepository, notificationService);
        incidentService = new EmergencyIncidentServiceImpl(incidentRepository, safariTripRepository, notificationService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Integer userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user" + userId + "@safariwave.test", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Roles.authoritiesFor(role)));
    }

    private EmergencyIncidentRequestDTO incident(String severity) {
        EmergencyIncidentRequestDTO dto = new EmergencyIncidentRequestDTO();
        dto.setIncidentDateTime(LocalDateTime.now().minusHours(1));
        dto.setIncidentType("Capsize risk");
        dto.setSeverity(severity);
        dto.setDescription("Strong wind near the estuary");
        return dto;
    }

    private WeatherAlert alertOn(LocalDate date) {
        WeatherAlert alert = new WeatherAlert();
        alert.setAlertDateTime(date.atTime(6, 0));
        alert.setLocation("Lagoon mouth");
        alert.setAlertStatus("ACTIVE");
        return alert;
    }

    @Test
    void guide_reportsIncident_andCriticalOnesNotifyOperations() {
        loginAs(7, Roles.GUIDE);
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(7, incidentService.createEmergencyIncident(incident("critical")).getCreatedBy());
        verify(notificationService).notifyRole(eq(Roles.OPERATIONS), eq("EMERGENCY_INCIDENT"), anyString());
    }

    @Test
    void guide_cannotEditAnotherGuidesIncident_butOperationsCan() {
        EmergencyIncident existing = new EmergencyIncident();
        existing.setIncidentId(3);
        existing.setCreatedBy(8);
        when(incidentRepository.findById(3)).thenReturn(Optional.of(existing));

        loginAs(7, Roles.GUIDE);
        assertThrows(AccessDeniedException.class, () -> incidentService.updateEmergencyIncident(3, incident("LOW")));

        loginAs(4, Roles.OPERATIONS);
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("LOW", incidentService.updateEmergencyIncident(3, incident("LOW")).getSeverity());
    }

    @Test
    void guide_cannotDeleteRecords_orCreateWeatherAlerts() {
        loginAs(7, Roles.GUIDE);

        assertThrows(AccessDeniedException.class, () -> incidentService.deleteEmergencyIncident(3));
        assertThrows(AccessDeniedException.class, () -> weatherService.createWeatherAlert(new WeatherAlertRequestDTO()));
    }

    @Test
    void invalidSeverity_isRejected() {
        loginAs(4, Roles.OPERATIONS);

        assertThrows(BadRequestException.class, () -> incidentService.createEmergencyIncident(incident("APOCALYPTIC")));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void failedInspection_notifiesOperations() {
        loginAs(7, Roles.GUIDE);
        Boat boat = new Boat();
        boat.setBoatId(4);
        boat.setBoatName("Heron");
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));
        when(safetyInspectionRepository.save(any(SafetyInspection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SafetyInspectionRequestDTO dto = new SafetyInspectionRequestDTO();
        dto.setBoatId(4);
        dto.setInspectionDate(LocalDate.now());
        dto.setSafetyStatus("FAILED");

        inspectionService.createSafetyInspection(dto);

        verify(notificationService).notifyRole(eq(Roles.OPERATIONS), eq("SAFETY_INSPECTION_FAILED"), anyString());
    }

    @Test
    void customer_onlySeesAlertsOnTheirBookingDays() {
        loginAs(10, Roles.CUSTOMER);
        LocalDate tripDay = LocalDate.now().plusDays(3);
        Booking booking = new Booking();
        booking.setTripDate(tripDay);
        when(bookingRepository.findByCustomer_UserIdAndBookingStatusIn(eq(10), anyList())).thenReturn(List.of(booking));
        when(weatherAlertRepository.findByAlertStatus("ACTIVE")).thenReturn(List.of(alertOn(tripDay), alertOn(tripDay.plusDays(1))));

        assertEquals(1, weatherService.getAllWeatherAlerts().size());
    }

    @Test
    void customer_cannotReadIncidentsOrInspections() {
        loginAs(10, Roles.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> incidentService.getAllEmergencyIncidents());
        assertThrows(AccessDeniedException.class, () -> inspectionService.getAllSafetyInspections());
    }
}
