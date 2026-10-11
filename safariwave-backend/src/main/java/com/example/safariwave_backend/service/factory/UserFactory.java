package com.example.safariwave_backend.service.factory;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Factory pattern (creational): the one place that builds a new User account.
 *
 * Customers who sign up themselves and staff accounts made by an admin follow different rules
 * (status, role). Keeping those rules here means services never build a User with "new" themselves.
 */
@Component
public class UserFactory {

    static final String ACTIVE = "ACTIVE";

    private final PasswordEncoder passwordEncoder;

    public UserFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /** Public sign-up: always the customer role and ACTIVE; any role or status in the request is ignored. */
    public User newCustomer(UserRegisterDTO dto, Role customerRole) {
        return build(dto, customerRole, ACTIVE);
    }

    /** Created by an admin: the chosen role, and the requested status (ACTIVE when none is given). */
    public User newAccountWithRole(UserRegisterDTO dto, Role role) {
        String status = dto.getStatus() == null || dto.getStatus().isBlank() ? ACTIVE : dto.getStatus();
        return build(dto, role, status);
    }

    private User build(UserRegisterDTO dto, Role role, String status) {
        User user = new User();
        user.setRole(role);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // stored hashed, never as typed
        user.setPhone(dto.getPhone());
        user.setStatus(status);
        return user;
    }
}
