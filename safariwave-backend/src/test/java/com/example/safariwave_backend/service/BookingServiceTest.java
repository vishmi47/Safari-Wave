package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.BookingRequestDTO;
import com.example.safariwave_backend.dto.response.BookingResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.Payment;
import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.CustomerExperienceRepository;
import com.example.safariwave_backend.repository.PaymentRepository;
import com.example.safariwave_backend.repository.PromotionRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.BookingServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SafariTripRepository safariTripRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerExperienceRepository customerExperienceRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User customer;
    private SafariPackage safariPackage;
    private SafariTrip trip;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role();
        customerRole.setRoleName(Roles.CUSTOMER);
        customer = new User();
        customer.setUserId(10);
        customer.setFirstName("Nimal");
        customer.setLastName("Perera");
        customer.setRole(customerRole);

        safariPackage = new SafariPackage();
        safariPackage.setPackageId(1);
        safariPackage.setPackageName("Sunset Lagoon");
        safariPackage.setBasePrice(new BigDecimal("1000.00"));
        safariPackage.setMaxPassengers(10);
        safariPackage.setStatus("ACTIVE");

        Boat boat = new Boat();
        boat.setBoatId(4);
        boat.setCapacity(12);
        boat.setStatus("OPERATIONAL");

        trip = tripStartingIn(LocalDateTime.now().plusDays(10), boat);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private SafariTrip tripStartingIn(LocalDateTime start, Boat boat) {
        SafariTrip t = new SafariTrip();
        t.setTripId(7);
        t.setSafariPackage(safariPackage);
        t.setBoat(boat);
        t.setTripDate(start.toLocalDate());
        t.setStartTime(LocalTime.of(start.getHour(), 0));
        t.setEndTime(null);
        t.setCapacity(10);
        t.setStatus("SCHEDULED");
        return t;
    }

    private void loginAs(Integer userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user" + userId + "@safariwave.test", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Roles.authoritiesFor(role)));
    }

    private BookingRequestDTO request(int passengers) {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setTripId(7);
        dto.setNumberOfPeople(passengers);
        return dto;
    }

    private Booking existingBooking(String status) {
        Booking booking = new Booking();
        booking.setBookingId(50);
        booking.setCustomer(customer);
        booking.setSafariTrip(trip);
        booking.setSafariPackage(safariPackage);
        booking.setTripDate(trip.getTripDate());
        booking.setNumberOfPeople(2);
        booking.setUnitPrice(new BigDecimal("800.00"));
        booking.setDiscountPercentage(BigDecimal.ZERO);
        booking.setTotalPrice(new BigDecimal("1600.00"));
        booking.setBookingStatus(status);
        booking.setPaymentStatus("PENDING");
        return booking;
    }

    // ---- create: permissions, capacity and price snapshot ----

    @Test
    void customer_booksForThemselves_withPromotionDiscountCaptured() {
        loginAs(10, Roles.CUSTOMER);
        Promotion promotion = new Promotion();
        promotion.setDiscountPercentage(new BigDecimal("10"));
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(safariTripRepository.findById(7)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumSeatsOnTrip(eq(7), anyList(), isNull())).thenReturn(5L);
        when(promotionRepository.findPublishedForPackageOn(eq(1), any(LocalDate.class))).thenReturn(List.of(promotion));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingRequestDTO dto = request(3);
        dto.setUserId(99); // a customer cannot book for someone else; this is ignored
        BookingResponseDTO response = bookingService.createBooking(dto);

        assertEquals(10, response.getUserId());
        assertEquals(0, new BigDecimal("2700.00").compareTo(response.getTotalPrice())); // 3 x 1000 - 10%
        assertEquals(0, new BigDecimal("10").compareTo(response.getDiscountPercentage()));
        assertEquals("PENDING", response.getBookingStatus());
    }

    @Test
    void finance_cannotCreateBooking() {
        loginAs(6, Roles.FINANCE);

        assertThrows(AccessDeniedException.class, () -> bookingService.createBooking(request(2)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void booking_exceedingAvailableSeats_isBlocked() {
        loginAs(10, Roles.CUSTOMER);
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(safariTripRepository.findById(7)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumSeatsOnTrip(eq(7), anyList(), isNull())).thenReturn(8L);

        BadRequestException error = assertThrows(BadRequestException.class, () -> bookingService.createBooking(request(3)));
        assertTrue(error.getMessage().contains("2 seat(s) left"));
    }

    @Test
    void customerRelations_mustNameTheCustomer() {
        loginAs(5, Roles.CUSTOMER_RELATIONS);

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(request(2)));
    }

    // ---- read ----

    @Test
    void customer_cannotViewAnotherCustomersBooking() {
        loginAs(11, Roles.CUSTOMER);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(existingBooking("PENDING")));

        assertThrows(AccessDeniedException.class, () -> bookingService.getBookingById(50));
    }

    @Test
    void assignedGuide_canViewBookingOnTheirTrip() {
        User guide = new User();
        guide.setUserId(7);
        trip.setGuide(guide);
        loginAs(7, Roles.GUIDE);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(existingBooking("CONFIRMED")));

        assertTrue(bookingService.getBookingById(50).isPresent());
    }

    // ---- update / cancel ----

    @Test
    void customerUpdate_recalculatesFromSnapshotPrice_notCurrentPackagePrice() {
        loginAs(10, Roles.CUSTOMER);
        Booking booking = existingBooking("PENDING");
        when(bookingRepository.findById(50)).thenReturn(Optional.of(booking));
        when(bookingRepository.sumSeatsOnTrip(7, List.of("PENDING", "CONFIRMED"), 50)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponseDTO response = bookingService.updateBooking(50, request(4));

        // the package now costs 1000, but this booking was made at 800
        assertEquals(0, new BigDecimal("3200.00").compareTo(response.getTotalPrice()));
    }

    @Test
    void customerCancel_insideCutoff_isRejected() {
        loginAs(10, Roles.CUSTOMER);
        trip = tripStartingIn(LocalDateTime.now().plusHours(5), trip.getBoat());
        Booking booking = existingBooking("CONFIRMED");
        booking.setSafariTrip(trip);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(50, null));
        assertEquals("CONFIRMED", booking.getBookingStatus());
    }

    @Test
    void customerCancel_beforeCutoff_setsCancelledAndNotifies() {
        loginAs(10, Roles.CUSTOMER);
        Booking booking = existingBooking("CONFIRMED");
        when(bookingRepository.findById(50)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponseDTO response = bookingService.cancelBooking(50, "Change of plans");

        assertEquals("CANCELLED", response.getBookingStatus());
        verify(notificationService).create(eq(10), eq("BOOKING_CANCELLED"), anyString());
        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void operations_cannotEditBookingDetails() {
        loginAs(4, Roles.OPERATIONS);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(existingBooking("PENDING")));

        assertThrows(AccessDeniedException.class, () -> bookingService.updateBooking(50, request(2)));
    }

    // ---- finance ----

    @Test
    void finance_refundRequiresSuccessfulPayment() {
        loginAs(6, Roles.FINANCE);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(existingBooking("CONFIRMED")));

        assertThrows(BadRequestException.class, () -> bookingService.updatePaymentStatus(50, "REFUNDED", null));
    }

    @Test
    void finance_markingPaid_confirmsPendingBooking() {
        loginAs(6, Roles.FINANCE);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(existingBooking("PENDING")));
        when(paymentRepository.findByBooking_BookingId(50)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponseDTO response = bookingService.updatePaymentStatus(50, "paid", null);

        assertEquals("SUCCESS", response.getPaymentStatus());
        assertEquals("CONFIRMED", response.getBookingStatus());
    }

    @Test
    void customer_cannotChangePaymentStatus() {
        loginAs(10, Roles.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> bookingService.updatePaymentStatus(50, "SUCCESS", null));
    }

    // ---- delete ----

    @Test
    void customerRelations_cannotHardDelete() {
        loginAs(5, Roles.CUSTOMER_RELATIONS);

        assertThrows(AccessDeniedException.class, () -> bookingService.deleteBooking(50));
        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void admin_cannotDeleteBookingWithPayment() {
        loginAs(2, Roles.ADMIN);
        when(bookingRepository.findById(50)).thenReturn(Optional.of(existingBooking("CONFIRMED")));
        when(paymentRepository.findByBooking_BookingId(50)).thenReturn(Optional.of(new Payment()));

        assertThrows(ConflictException.class, () -> bookingService.deleteBooking(50));
        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void invalidPassengerCount_isRejected() {
        loginAs(10, Roles.CUSTOMER);
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(safariTripRepository.findById(7)).thenReturn(Optional.of(trip));

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(request(0)));
        verify(bookingRepository, never()).sumSeatsOnTrip(anyInt(), anyList(), any());
    }
}
