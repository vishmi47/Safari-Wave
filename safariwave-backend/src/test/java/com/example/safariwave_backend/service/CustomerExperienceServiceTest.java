package com.example.safariwave_backend.service;

import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.dto.request.CustomerExperienceRequestDTO;
import com.example.safariwave_backend.entity.CustomerExperience;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.CustomerExperienceRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.service.impl.CustomerExperienceServiceImpl;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerExperienceServiceTest {

    @Mock
    private CustomerExperienceRepository customerExperienceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private CustomerExperienceServiceImpl customerExperienceService;

    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        user.setEmail("customer@example.com");

        booking = new Booking();
        booking.setBookingId(7);
        // A rating needs the customer's own completed booking (spec B.3)
        booking.setCustomer(user);
        booking.setBookingStatus("COMPLETED");

        AuthenticatedUser principal = new AuthenticatedUser(1, "customer@example.com", Roles.CUSTOMER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Roles.authoritiesFor(Roles.CUSTOMER)));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createExperience_shouldPersistAndReturnSavedRecord() {
        CustomerExperienceRequestDTO dto = new CustomerExperienceRequestDTO();
        dto.setCustomerId(1);
        dto.setBookingId(7);
        dto.setExperienceType("FEEDBACK");
        dto.setSubject("Service quality");
        dto.setDescription("Everything was excellent");
        dto.setRating(5);
        dto.setStatus("SUBMITTED");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(7)).thenReturn(Optional.of(booking));
        when(customerExperienceRepository.save(any(CustomerExperience.class))).thenAnswer(invocation -> {
            CustomerExperience saved = invocation.getArgument(0);
            saved.setId(10);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        var response = customerExperienceService.createExperience(dto);

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals("FEEDBACK", response.getExperienceType());
        assertEquals("Service quality", response.getSubject());
    }
}
