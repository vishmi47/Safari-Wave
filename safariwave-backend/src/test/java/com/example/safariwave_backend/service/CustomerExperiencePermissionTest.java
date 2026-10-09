package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.ComplaintResponseRequestDTO;
import com.example.safariwave_backend.dto.request.CustomerExperienceRequestDTO;
import com.example.safariwave_backend.dto.response.CustomerExperienceResponseDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.CustomerExperience;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.enums.CustomerExperienceStatus;
import com.example.safariwave_backend.enums.CustomerExperienceType;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.CustomerExperienceRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.CustomerExperienceServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Customer Experience permission and validation rules (spec 6.5 / B.3). */
@ExtendWith(MockitoExtension.class)
class CustomerExperiencePermissionTest {

    @Mock
    private CustomerExperienceRepository customerExperienceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CustomerExperienceServiceImpl service;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUserId(10);
        customer.setFirstName("Nimal");
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

    private CustomerExperienceRequestDTO rating(Integer bookingId, Integer stars) {
        CustomerExperienceRequestDTO dto = new CustomerExperienceRequestDTO();
        dto.setExperienceType("RATING");
        dto.setSubject("Sunset trip");
        dto.setDescription("Lovely evening");
        dto.setBookingId(bookingId);
        dto.setRating(stars);
        return dto;
    }

    private Booking booking(String status) {
        Booking booking = new Booking();
        booking.setBookingId(5);
        booking.setCustomer(customer);
        booking.setBookingStatus(status);
        return booking;
    }

    private CustomerExperience record(CustomerExperienceStatus status, String response) {
        CustomerExperience experience = new CustomerExperience();
        experience.setId(1);
        experience.setCustomer(customer);
        experience.setExperienceType(CustomerExperienceType.COMPLAINT);
        experience.setStatus(status);
        experience.setResponse(response);
        experience.setInternalNotes("staff only");
        return experience;
    }

    @Test
    void customer_canRateOnlyACompletedTrip() {
        loginAs(10, Roles.CUSTOMER);
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking("CONFIRMED")));

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.createExperience(rating(5, 4)));
        assertTrue(error.getMessage().contains("completed"));
    }

    @Test
    void ratingOutsideOneToFive_isRejected() {
        loginAs(10, Roles.CUSTOMER);
        when(userRepository.findById(10)).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking("COMPLETED")));

        assertThrows(BadRequestException.class, () -> service.createExperience(rating(5, 6)));
    }

    @Test
    void customer_cannotRateSomeoneElsesBooking() {
        loginAs(11, Roles.CUSTOMER);
        User other = new User();
        other.setUserId(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(other));
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking("COMPLETED")));

        assertThrows(AccessDeniedException.class, () -> service.createExperience(rating(5, 5)));
    }

    @Test
    void customerRelations_cannotCreateRecords() {
        loginAs(5, Roles.CUSTOMER_RELATIONS);

        assertThrows(AccessDeniedException.class, () -> service.createExperience(rating(5, 5)));
    }

    @Test
    void customer_cannotEditAfterStaffResponded() {
        loginAs(10, Roles.CUSTOMER);
        when(customerExperienceRepository.findById(1)).thenReturn(Optional.of(record(CustomerExperienceStatus.RESPONDED, "We are sorry")));
        CustomerExperienceRequestDTO dto = rating(null, null);
        dto.setExperienceType("COMPLAINT");

        assertThrows(BadRequestException.class, () -> service.updateExperience(1, dto));
        assertThrows(BadRequestException.class, () -> service.deleteExperience(1));
    }

    @Test
    void customer_neverSeesInternalNotes() {
        loginAs(10, Roles.CUSTOMER);
        when(customerExperienceRepository.findById(1)).thenReturn(Optional.of(record(CustomerExperienceStatus.SUBMITTED, null)));

        assertNull(service.getExperienceById(1).orElseThrow().getInternalNotes());
    }

    @Test
    void complaintResponses_areCustomerRelationsOnly_andNotifyTheCustomer() {
        when(customerExperienceRepository.findById(1)).thenReturn(Optional.of(record(CustomerExperienceStatus.SUBMITTED, null)));
        ComplaintResponseRequestDTO dto = new ComplaintResponseRequestDTO();
        dto.setResponse("We have refunded you.");

        loginAs(10, Roles.CUSTOMER);
        assertThrows(AccessDeniedException.class, () -> service.respondToComplaint(1, dto));

        loginAs(5, Roles.CUSTOMER_RELATIONS);
        when(customerExperienceRepository.save(any(CustomerExperience.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CustomerExperienceResponseDTO response = service.respondToComplaint(1, dto);
        assertEquals("RESPONDED", response.getStatus());
        verify(notificationService).create(eq(10), eq("COMPLAINT_RESPONSE"), anyString());
    }

    @Test
    void customerRelations_deletesOnlyResolvedOrRejected() {
        loginAs(5, Roles.CUSTOMER_RELATIONS);
        when(customerExperienceRepository.findById(1)).thenReturn(Optional.of(record(CustomerExperienceStatus.IN_PROGRESS, null)));
        assertThrows(BadRequestException.class, () -> service.deleteExperience(1));

        CustomerExperience resolved = record(CustomerExperienceStatus.RESOLVED, "Done");
        when(customerExperienceRepository.findById(1)).thenReturn(Optional.of(resolved));
        service.deleteExperience(1);
        verify(customerExperienceRepository).delete(resolved);
    }

    @Test
    void marketing_seesRatingsWithoutCustomerDetails_butNotComplaints() {
        loginAs(3, Roles.MARKETING);
        CustomerExperience rated = record(CustomerExperienceStatus.SUBMITTED, null);
        rated.setExperienceType(CustomerExperienceType.RATING);
        rated.setRating(4);
        when(customerExperienceRepository.findByRatingIsNotNull()).thenReturn(List.of(rated));

        CustomerExperienceResponseDTO dto = service.getRatings().get(0);
        assertNull(dto.getCustomerName());
        assertNull(dto.getInternalNotes());
        assertEquals(4.0, ((java.math.BigDecimal) service.getRatingSummary().get("averageRating")).doubleValue());
        assertThrows(AccessDeniedException.class, () -> service.getAllComplaints());
    }
}
