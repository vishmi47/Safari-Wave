package com.example.safariwave_backend.security;

import java.security.Principal;

/**
 * The logged-in user, built from the JWT claims by {@link JwtAuthFilter}.
 *
 * getName() returns the email, so existing code that calls authentication.getName() keeps working.
 */
public record AuthenticatedUser(Integer userId, String email, String roleName) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    /** Canonical role name (aliases such as GENERAL_MANAGER are mapped to ADMIN). */
    public String role() {
        return Roles.canonical(roleName);
    }
}
