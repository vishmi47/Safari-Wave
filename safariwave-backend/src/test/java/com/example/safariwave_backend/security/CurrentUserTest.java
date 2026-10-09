package com.example.safariwave_backend.security;

import com.example.safariwave_backend.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Integer userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user" + userId + "@safariwave.test", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Roles.authoritiesFor(role)));
    }

    @Test
    void customer_canAccessOwnRecord() {
        loginAs(10, Roles.CUSTOMER);

        assertDoesNotThrow(() -> CurrentUser.requireOwnerOrAnyRole(10, Roles.CUSTOMER_RELATIONS));
    }

    @Test
    void customer_cannotAccessAnotherCustomersRecord() {
        loginAs(10, Roles.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> CurrentUser.requireOwnerOrAnyRole(11, Roles.CUSTOMER_RELATIONS));
    }

    @Test
    void staffRole_canAccessOtherCustomersRecord() {
        loginAs(5, Roles.CUSTOMER_RELATIONS);

        assertDoesNotThrow(() -> CurrentUser.requireOwnerOrAnyRole(11, Roles.CUSTOMER_RELATIONS));
    }

    @Test
    void generalManager_isTreatedAsAdminEverywhere() {
        loginAs(2, "GENERAL_MANAGER");

        assertTrue(CurrentUser.isAdmin());
        assertEquals(Roles.ADMIN, CurrentUser.role());
        assertDoesNotThrow(() -> CurrentUser.requireAnyRole(Roles.MARKETING));
    }

    @Test
    void guide_isForbiddenFromMarketingOnlyAction() {
        loginAs(7, Roles.GUIDE);

        assertThrows(AccessDeniedException.class, () -> CurrentUser.requireAnyRole(Roles.MARKETING));
    }

    @Test
    void safetyAlias_isTreatedAsGuide() {
        loginAs(8, "SAFETY");

        assertTrue(CurrentUser.hasAnyRole(Roles.GUIDE));
    }

    @Test
    void anonymous_isUnauthorized() {
        assertFalse(CurrentUser.isAdmin());
        assertThrows(UnauthorizedException.class, CurrentUser::get);
        assertThrows(UnauthorizedException.class, () -> CurrentUser.requireAnyRole(Roles.CUSTOMER));
    }
}
