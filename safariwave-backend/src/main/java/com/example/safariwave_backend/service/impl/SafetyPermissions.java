package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Locale;

/**
 * Shared rules for the Weather, Safety & Emergency module (spec 6.4 / B.3).
 *
 * Read: OPERATIONS, GUIDE, CUSTOMER_RELATIONS (customers only see weather alerts that affect their bookings).
 * Inspections and incidents: GUIDE and OPERATIONS create; OPERATIONS edits all, a GUIDE only their own records.
 * Weather alerts: OPERATIONS creates and edits. Deleting invalid or duplicate records: OPERATIONS.
 */
final class SafetyPermissions {

    static final List<String> SEVERITIES = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private SafetyPermissions() {
    }

    static void requireStaffRead() {
        CurrentUser.requireAnyRole(Roles.OPERATIONS, Roles.GUIDE, Roles.CUSTOMER_RELATIONS);
    }

    static void requireCanRecord() {
        CurrentUser.requireAnyRole(Roles.OPERATIONS, Roles.GUIDE);
    }

    /** Operations (and admin) edit any record; a guide only the records they created. */
    static void requireCanEdit(Integer createdBy) {
        if (CurrentUser.isAdmin() || CurrentUser.hasAnyRole(Roles.OPERATIONS)) {
            return;
        }
        if (CurrentUser.hasAnyRole(Roles.GUIDE) && CurrentUser.isOwner(createdBy)) {
            return;
        }
        throw new AccessDeniedException("Guides can only update records they created.");
    }

    static void requireCanDelete() {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
    }

    /** Upper-cases and checks a value against the allowed list; uses the fallback when blank. */
    static String oneOf(String value, List<String> allowed, String label, String fallback) {
        if (value == null || value.isBlank()) {
            if (fallback == null) {
                throw new BadRequestException(label + " is required.");
            }
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BadRequestException(label + " must be one of " + allowed + ".");
        }
        return normalized;
    }
}
