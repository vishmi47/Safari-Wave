package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.repository.RoleRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.service.factory.UserFactory;
import com.example.safariwave_backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterDTO dto;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        // Real factory on top of the mocked password encoder
        ReflectionTestUtils.setField(userService, "userFactory", new UserFactory(passwordEncoder));

        dto = new UserRegisterDTO();
        dto.setFirstName("Nimal");
        dto.setLastName("Perera");
        dto.setEmail("nimal@example.com");
        dto.setPassword("password123");
        dto.setRoleId(2); // tries to sign up as ADMIN
        dto.setStatus("ACTIVE");

        customerRole = new Role();
        customerRole.setRoleId(1);
        customerRole.setRoleName("CUSTOMER");
    }

    @Test
    void registerCustomer_shouldIgnoreRequestedRoleAndCreateCustomer() {
        when(roleRepository.findFirstByRoleNameIgnoreCase("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = userService.registerCustomer(dto);

        assertEquals("CUSTOMER", response.getRoleName());
        assertEquals("ACTIVE", response.getStatus());
        verify(roleRepository, never()).findById(anyInt());
    }

    @Test
    void registerCustomer_duplicateEmail_shouldThrowConflict() {
        when(roleRepository.findFirstByRoleNameIgnoreCase("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateResourceException.class, () -> userService.registerCustomer(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_withoutRole_shouldFailValidation() {
        dto.setRoleId(null);

        assertThrows(BadRequestException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }
}
