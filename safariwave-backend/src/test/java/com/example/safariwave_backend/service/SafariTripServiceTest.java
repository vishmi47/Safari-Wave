package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.request.TripAssignmentRequestDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.EmergencyIncidentRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.SafariTripServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafariTripServiceTest {

    @Mock
    private SafariTripRepository safariTripRepository;

    @Mock
    private SafariPackageRepository safariPackageRepository;

    @Mock
    private BoatRepository boatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SafetyInspectionRepository safetyInspectionRepository;

    @Mock
    private EmergencyIncidentRepository emergencyIncidentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SafariTripServiceImpl safariTripService;

    private SafariPackage safariPackage;
    private Boat boat;
    private SafariTrip trip;
    private User guide;

    @BeforeEach
    void setUp() {
        safariPackage = new SafariPackage();
        safariPackage.setPackageId(1);
        safariPackage.setPackageName("Sunset Lagoon");
        safariPackage.setStatus("ACTIVE");

        boat = new Boat();
        boat.setBoatId(4);
        boat.setBoatName("Heron");
        boat.setCapacity(12);
        boat.setStatus("OPERATIONAL");

        Role guideRole = new Role();
        guideRole.setRoleName("SAFETY"); // legacy alias of GUIDE
        guide = new User();
        guide.setUserId(7);
        guide.setFirstName("Prabath");
        guide.setRole(guideRole);

        trip = new SafariTrip();
        trip.setTripId(20);
        trip.setSafariPackage(safariPackage);
        trip.setBoat(boat);
        trip.setGuide(guide);
        trip.setTripDate(LocalDate.now().plusDays(3));
        trip.setStartTime(LocalTime.of(9, 0));
        trip.setCapacity(10);
        trip.setStatus("SCHEDULED");
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

    private Booking booking(int id, String status, String paymentStatus) {
        User customer = new User();
        customer.setUserId(100 + id);
        Booking booking = new Booking();
        booking.setBookingId(id);
        booking.setCustomer(customer);
        booking.setBookingStatus(status);
        booking.setPaymentStatus(paymentStatus);
        return booking;
    }

    private SafariTripRequestDTO tripRequest() {
        SafariTripRequestDTO dto = new SafariTripRequestDTO();
        dto.setPackageId(1);
        dto.setBoatId(4);
        dto.setTripDate(LocalDate.now().plusDays(5));
        dto.setStartTime(LocalTime.of(8, 0));
        dto.setEndTime(LocalTime.of(10, 0));
        dto.setCapacity(10);
        dto.setStatus("scheduled");
        return dto;
    }

    @Test
    void customer_cannotCreateTrip() {
        loginAs(10, Roles.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> safariTripService.createTrip(tripRequest()));
        verify(safariTripRepository, never()).save(any());
    }

    @Test
    void operations_cannotScheduleBoatUnderMaintenance() {
        loginAs(4, Roles.OPERATIONS);
        boat.setStatus("UNDER_MAINTENANCE");
        when(safariPackageRepository.findById(1)).thenReturn(Optional.of(safariPackage));
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));

        assertThrows(BadRequestException.class, () -> safariTripService.createTrip(tripRequest()));
    }

    @Test
    void tripCapacity_cannotExceedBoatCapacity() {
        loginAs(4, Roles.OPERATIONS);
        when(safariPackageRepository.findById(1)).thenReturn(Optional.of(safariPackage));
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));
        SafariTripRequestDTO dto = tripRequest();
        dto.setCapacity(20);

        assertThrows(BadRequestException.class, () -> safariTripService.createTrip(dto));
    }

    @Test
    void assignedGuide_canCompleteTrip_andConfirmedBookingsBecomeCompleted() {
        loginAs(7, "SAFETY");
        Booking confirmed = booking(1, "CONFIRMED", "SUCCESS");
        Booking unpaid = booking(2, "PENDING", "PENDING");
        when(safariTripRepository.findById(20)).thenReturn(Optional.of(trip));
        when(bookingRepository.findBySafariTrip_TripIdAndBookingStatusIn(eq(20), anyList())).thenReturn(List.of(confirmed, unpaid));
        when(safariTripRepository.save(any(SafariTrip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        safariTripService.updateTripStatus(20, "COMPLETED", null);

        assertEquals("COMPLETED", trip.getStatus());
        assertEquals("COMPLETED", confirmed.getBookingStatus());
        assertEquals("PENDING", unpaid.getBookingStatus());
        verify(notificationService).create(eq(101), eq("TRIP_COMPLETED"), anyString());
    }

    @Test
    void guide_cannotCancelTrip() {
        loginAs(7, Roles.GUIDE);
        when(safariTripRepository.findById(20)).thenReturn(Optional.of(trip));

        assertThrows(AccessDeniedException.class, () -> safariTripService.updateTripStatus(20, "CANCELLED", null));
    }

    @Test
    void otherGuide_cannotChangeTripStatus() {
        loginAs(8, Roles.GUIDE);
        when(safariTripRepository.findById(20)).thenReturn(Optional.of(trip));

        assertThrows(AccessDeniedException.class, () -> safariTripService.updateTripStatus(20, "IN_PROGRESS", null));
    }

    @Test
    void cancellingTrip_cancelsActiveBookings_andNotifiesCustomers() {
        loginAs(4, Roles.OPERATIONS);
        Booking paid = booking(1, "CONFIRMED", "SUCCESS");
        when(safariTripRepository.findById(20)).thenReturn(Optional.of(trip));
        when(bookingRepository.findBySafariTrip_TripIdAndBookingStatusIn(eq(20), anyList())).thenReturn(List.of(paid));
        when(safariTripRepository.save(any(SafariTrip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.sumSeatsOnTrip(eq(20), anyList(), isNull())).thenReturn(0L);

        safariTripService.cancelTrip(20, "Storm warning");

        assertEquals("CANCELLED", paid.getBookingStatus());
        verify(notificationService).create(eq(101), eq("TRIP_CANCELLED"), contains("refund"));
    }

    @Test
    void deletingTripWithBookings_isRejected() {
        loginAs(4, Roles.OPERATIONS);
        when(safariTripRepository.findById(20)).thenReturn(Optional.of(trip));
        when(bookingRepository.existsBySafariTrip_TripId(20)).thenReturn(true);

        assertThrows(ConflictException.class, () -> safariTripService.deleteTrip(20));
        verify(safariTripRepository, never()).delete(any());
    }

    @Test
    void assigningNonGuideUser_isRejected() {
        loginAs(4, Roles.OPERATIONS);
        Role financeRole = new Role();
        financeRole.setRoleName(Roles.FINANCE);
        User financeUser = new User();
        financeUser.setUserId(6);
        financeUser.setFirstName("Malinda");
        financeUser.setRole(financeRole);
        when(safariTripRepository.findById(20)).thenReturn(Optional.of(trip));
        when(userRepository.findById(6)).thenReturn(Optional.of(financeUser));
        TripAssignmentRequestDTO dto = new TripAssignmentRequestDTO();
        dto.setGuideId(6);

        assertThrows(BadRequestException.class, () -> safariTripService.assignTrip(20, dto));
    }

    private static String contains(String text) {
        return argThat(message -> message != null && message.contains(text));
    }
}
