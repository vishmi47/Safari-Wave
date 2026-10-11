package com.example.safariwave_backend.service.factory;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserFactoryTest {

    private UserFactory factory;
    private UserRegisterDTO dto;
    private Role role;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("password123")).thenReturn("hashed");
        factory = new UserFactory(encoder);

        dto = new UserRegisterDTO();
        dto.setFirstName("Nimal");
        dto.setLastName("Perera");
        dto.setEmail("nimal@example.com");
        dto.setPassword("password123");
        dto.setPhone("0771234567");
        dto.setStatus("INACTIVE");

        role = new Role();
        role.setRoleName("FINANCE");
    }

    @Test
    void newCustomer_isAlwaysActive_withThePasswordHashed() {
        User user = factory.newCustomer(dto, role);

        assertEquals("ACTIVE", user.getStatus());
        assertEquals("hashed", user.getPassword());
        assertSame(role, user.getRole());
        assertEquals("nimal@example.com", user.getEmail());
        assertEquals("0771234567", user.getPhone());
    }

    @Test
    void newAccountWithRole_keepsTheRequestedStatus_orDefaultsToActive() {
        assertEquals("INACTIVE", factory.newAccountWithRole(dto, role).getStatus());

        dto.setStatus(" ");
        assertEquals("ACTIVE", factory.newAccountWithRole(dto, role).getStatus());
    }
}
