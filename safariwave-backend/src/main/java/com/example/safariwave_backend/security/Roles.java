package com.example.safariwave_backend.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Role names used across SafariWave (spec Part B, section B.2).
 *
 * The codebase already used OPERATIONS for the spec's OPS_SUPERVISOR, and GENERAL_MANAGER / SAFETY as
 * alternative names for ADMIN / GUIDE. Those older names keep working: a user whose role row is one of the
 * aliases is also granted the canonical authority (e.g. ROLE_GENERAL_MANAGER and ROLE_ADMIN).
 */
public final class Roles {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";
    public static final String MARKETING = "MARKETING";
    public static final String OPERATIONS = "OPERATIONS"; // spec: OPS_SUPERVISOR
    public static final String CUSTOMER_RELATIONS = "CUSTOMER_RELATIONS";
    public static final String FINANCE = "FINANCE";
    public static final String GUIDE = "GUIDE";

    /** Canonical roles in seeding order; on an empty database this gives the role ids the frontend expects (1-7). */
    public static final List<String> ALL = List.of(CUSTOMER, ADMIN, MARKETING, OPERATIONS, CUSTOMER_RELATIONS, FINANCE, GUIDE);

    private static final Map<String, String> ALIASES = Map.of(
            "GENERAL_MANAGER", ADMIN,
            "SAFETY", GUIDE,
            "OPS_SUPERVISOR", OPERATIONS
    );

    private Roles() {
    }

    /** Maps a stored role name (any case, possibly an alias) to its canonical name. */
    public static String canonical(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return ALIASES.getOrDefault(normalized, normalized);
    }

    /** Alias names that are treated as the given canonical role. */
    public static List<String> aliasesOf(String canonicalRole) {
        List<String> aliases = new ArrayList<>();
        ALIASES.forEach((alias, target) -> {
            if (target.equals(canonicalRole)) {
                aliases.add(alias);
            }
        });
        return aliases;
    }

    /** Spring Security authorities for a stored role name: the role as stored plus its canonical name. */
    public static List<SimpleGrantedAuthority> authoritiesFor(String roleName) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roleName == null || roleName.isBlank()) {
            return authorities;
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        String canonical = canonical(roleName);
        if (!canonical.equals(roleName)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + canonical));
        }
        return authorities;
    }
}
