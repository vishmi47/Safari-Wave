package com.example.safariwave_backend.security;

import com.example.safariwave_backend.exception.UnauthorizedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Access to the logged-in user from controllers and services, plus the reusable permission checks used by
 * every module for "own records" and role-based rules (spec Part B, section B.3).
 *
 * Role checks compare canonical role names, so GENERAL_MANAGER counts as ADMIN and SAFETY counts as GUIDE.
 * The require* methods always let ADMIN through, because ADMIN has full CRUD on every module (spec B.2).
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The logged-in user, or empty for anonymous requests. */
    public static Optional<AuthenticatedUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** The logged-in user; throws 401 when the request is anonymous. */
    public static AuthenticatedUser get() {
        return find().orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }

    public static Integer id() {
        return get().userId();
    }

    /** Canonical role of the logged-in user. */
    public static String role() {
        return get().role();
    }

    public static boolean isAdmin() {
        return find().map(user -> Roles.ADMIN.equals(user.role())).orElse(false);
    }

    /** True when the logged-in user has one of the given roles. ADMIN is not added implicitly here. */
    public static boolean hasAnyRole(String... roles) {
        return find().map(user -> Arrays.asList(roles).contains(user.role())).orElse(false);
    }

    /** True when the given owner id is the logged-in user. */
    public static boolean isOwner(Integer ownerId) {
        return find().map(user -> user.userId() != null && Objects.equals(user.userId(), ownerId)).orElse(false);
    }

    /** Throws 403 unless the user is ADMIN or has one of the given roles. */
    public static void requireAnyRole(String... roles) {
        get();
        if (!isAdmin() && !hasAnyRole(roles)) {
            throw new AccessDeniedException("You are not authorized to perform this operation.");
        }
    }

    /** Throws 403 unless the user owns the record, is ADMIN, or has one of the given roles. */
    public static void requireOwnerOrAnyRole(Integer ownerId, String... roles) {
        get();
        if (!isOwner(ownerId) && !isAdmin() && !hasAnyRole(roles)) {
            throw new AccessDeniedException("You can only access your own records.");
        }
    }
}
