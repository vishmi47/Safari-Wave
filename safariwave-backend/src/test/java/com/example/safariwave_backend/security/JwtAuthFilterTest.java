package com.example.safariwave_backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-that-is-long-enough-for-hs256-signing-123456");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_shouldSetAuthenticatedUserPrincipalWithCanonicalAuthority() throws Exception {
        String token = jwtUtil.generateToken("gm@safariwave.test", 2, "GENERAL_MANAGER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        AuthenticatedUser user = assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
        assertEquals(2, user.userId());
        assertEquals(Roles.ADMIN, user.role());
        // Existing controllers rely on getName() returning the email
        assertEquals("gm@safariwave.test", authentication.getName());
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_GENERAL_MANAGER", "ROLE_ADMIN"), authorities);
    }

    @Test
    void invalidToken_shouldLeaveRequestUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void entryPoint_shouldReturn401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestSecurityErrorHandler().commence(request, response, new BadCredentialsException("no token"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"path\":\"/api/bookings\""));
    }
}
