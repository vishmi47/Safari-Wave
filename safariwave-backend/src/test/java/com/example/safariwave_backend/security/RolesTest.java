package com.example.safariwave_backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RolesTest {

    @Test
    void canonical_shouldMapLegacyAliases() {
        assertEquals(Roles.ADMIN, Roles.canonical("GENERAL_MANAGER"));
        assertEquals(Roles.GUIDE, Roles.canonical("SAFETY"));
        assertEquals(Roles.OPERATIONS, Roles.canonical("OPS_SUPERVISOR"));
        assertEquals(Roles.MARKETING, Roles.canonical(" marketing "));
        assertNull(Roles.canonical(null));
    }

    @Test
    void authoritiesFor_aliasRole_shouldGrantStoredAndCanonicalAuthority() {
        List<SimpleGrantedAuthority> authorities = Roles.authoritiesFor("GENERAL_MANAGER");

        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_GENERAL_MANAGER"), new SimpleGrantedAuthority("ROLE_ADMIN")), authorities);
    }

    @Test
    void authoritiesFor_canonicalRole_shouldGrantSingleAuthority() {
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_FINANCE")), Roles.authoritiesFor("FINANCE"));
        assertTrue(Roles.authoritiesFor(null).isEmpty());
    }

    @Test
    void all_shouldStartWithCustomerSoFrontendRoleIdsMatchOnEmptyDatabase() {
        assertEquals(List.of("CUSTOMER", "ADMIN", "MARKETING", "OPERATIONS", "CUSTOMER_RELATIONS", "FINANCE", "GUIDE"), Roles.ALL);
    }
}
